/* SettingsActivity.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          February 24, 2011
 * PROJECT:       Perfect Paper Passwords for Android
 * ANDROID V.:	  1.1
 * 
 * This activity provides a UI for managing application-wide settings, such as
 * setting or clearing the master password and toggling the option to copy
 * passcodes to the clipboard.
 * 
 * This program is Copyright 2011, Jeffrey T. Darlington.
 * E-mail:  android_apps@gpf-comics.com
 * Web:     https://code.google.com/p/android-ppp/
 * 
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this
 * program.  If not, see http://www.gnu.org/licenses/.
*/
package com.gpfcomics.android.ppp;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

/**
 * This activity provides an interface for accessing application-wide settings, such
 * as the user's password and whether or not to copy passcodes to the clipboard.
 * @author Jeffrey T. Darlington
 * @version 1.0
 * @since 1.0
 */
public class SettingsActivity extends Activity {
	
	/** This constant identifies the first new password dialog */
	private static final int DIALOG_PASSWORD_SET1 = 920541;

	/** This constant identifies the second new password dialog */
	private static final int DIALOG_PASSWORD_SET2 = 920542;

	/** This constant identifies the clear password dialog */
	private static final int DIALOG_PASSWORD_CLEAR = 920543;

	/** This constant identifies the progress dialog */
	private static final int DIALOG_PROGRESS = 920544;
	
	/** This constant identifies the Help option menu */
	private static final int OPTMENU_HELP = Menu.FIRST;

	/** This constant signifies an error in the cryptography thread.  It states
	 *  that the password could not be set at the application level. */
	private static final int THREAD_ERROR_PASSWORD_SET_FAILED = -1;
	
	/** This constant signifies an error in the cryptography thread.  It states
	 *  that the password could not be cleared at the application level. */
	private static final int THREAD_ERROR_PASSWORD_CLEAR_FAILED = -2;
	
	/** This constant signifies an error in the cryptography thread.  It states
	 *  that there were no card sets in the database, so no cryptography is
	 *  necessary. */
	private static final int THREAD_ERROR_NO_CARD_SETS = -3;
	
	/** This constant signifies an error in the cryptography thread.  It states
	 *  that a fatal error occurred in the encryption or decryption process and
	 *  the sequence keys in the database may be in an unstable and unpredictable
	 *  state. */
	private static final int THREAD_ERROR_EXCEPTION = -1000;
	
	/** This constant will be used to tell the progress dialog that we are
	 *  setting rather than clearing the password. */
	private static final int ACTION_SET_PWD = 0;
	
	/** This constant will be used to tell the progress dialog that we are
	 *  clearing rather than setting the password. */
	private static final int ACTION_CLEAR_PWD = 1;
	
	/** The Set/Clear Password button */
	private Button btnSetPassword = null;
	
	/** The copy passcodes to the clipboard checkbox */
	private CheckBox chkCopyPasscodes = null;
	
	/** A handy reference to the ProgressDialog used when encrypting
	 *  and decrypting data in the database. */
	private ProgressDialog progressDialog = null;
	
	/** A reference back to the parent application */
	private PPPApplication theApp = null;
	
	/** This string holds the result of the first new password dialog box so it can
	 *  be compared with the result of the second dialog before the password is
	 *  actually set. */
	private String newPassword = null;
	
	/** This flag contains the current action state, i.e. whether or not we are
	 *  setting or clearing the password.  This will be used to communicate to
	 *  the progress dialog which action it should be performing, as well as
	 *  which messages to display. */
	private int currentAction = -1;
	
	/** A count of all card sets in the database */
	private int cardsetCount = 0;
	
	/** This object represents a worker thread that will perform the actual
	 *  cryptographic operations (encryption or decryption) on the sequence key
	 *  data in the database. */
	private SeqKeyCryptoThread seqKeyCryptoThread = null;
	
	public void onCreate(Bundle savedInstanceState) {
        // The usual GUI setup stuff:
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        // Get a reference back to the app where the settings are handled:
        theApp = (PPPApplication)getApplication();
        // Get the count of all card sets in the database.  Since we're not adding
        // or removing any card sets here, we can do this once at the start and
        // avoid any unnecessary DB calls later.
        cardsetCount = theApp.getDBHelper().countCarsets();
        // Get references to our UI elements:
        btnSetPassword = (Button)findViewById(R.id.settings_password_btn);
        chkCopyPasscodes = (CheckBox)findViewById(R.id.settings_copy_passcodes_chk);
        // Set the current state of the copy passcodes checkbox.  This should
        // match the current preference in the user's settings.
        chkCopyPasscodes.setChecked(theApp.copyPasscodesToClipboard());
        // Now set the on click listener for the checkbox.  We'll ue the click
        // listener rather than the check listener to make sure we only do things
        // if the user taps the UI, rather than if we do something programmatically.
        chkCopyPasscodes.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Simple enough:  Tell the app to toggle the setting.  This should
				// take care of the internal logic as well as saving the actual
				// setting to the preferences file.  The UI should handle the
				// state of the checkbox itself.
				theApp.toggleCopyPasscodesSetting();
			}
        });
        // Set the display text for the password button.  If the password is currently
        // set, tapping the button will clear the password, so display the "Clear
        // Password" text.  Conversely, if there's no password set, the button will
        // set the password.
        if (theApp.promptForPassword())
        	btnSetPassword.setText(R.string.settings_password_btn_clear);
        else
        	btnSetPassword.setText(R.string.settings_password_btn_set);
        // Set the click listener for the button.  The bulk of the work will be done
        // in dialogs, so we won't actually do too much here.
        btnSetPassword.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// If there's currently a password set, start the clearing process:
				if (theApp.promptForPassword()) showDialog(DIALOG_PASSWORD_CLEAR);
				// Otherwise, start the process for setting one:
				else showDialog(DIALOG_PASSWORD_SET1);
			}
        });
    }
	
    protected Dialog onCreateDialog(int id)
    {
    	// Create a dialog reference for us to return:
    	Dialog dialog = null;
    	// Now let's see which dialog we need to launch:
    	switch (id) {
    		// Setting a new password is a two-step process.  Like many password
    		// setting situations, we want to prompt the user to enter their new
    		// password twice to make sure it's exactly what they want.  So this
    		// dialog represents the first step:  prompt the user for the first
    		// password, which we'll compare against the second pass.  Unfortunately,
    		// this is just a little to funky for AlertDialog.Builder to handle for
    		// us, so we'll have to build this dialog manually.  (All three of our
    		// password dialogs work the same way in this case.) 
	    	case DIALOG_PASSWORD_SET1:
	    		// Start building the dialog and set its layout and title:
	    		dialog = new Dialog(this);
	    		dialog.setContentView(R.layout.password_dialog);
	    		dialog.setTitle(R.string.dialog_password_set_title);
	    		// Get the "positive" button and set it to the first-step text:
	    		Button btnSet = (Button)dialog.findViewById(R.id.btn_password_ok);
	    		btnSet.setText(R.string.dialog_password_set_btn);
	    		// Set the button's listener:
	    		btnSet.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						// Get the current value of the password text box:
						EditText txtPassword =
							(EditText)(v.getRootView().findViewById(R.id.password_dialog_txt));
						String pwd = ((Editable)txtPassword.getText()).toString();
						// Make sure the password is not empty (otherwise we don't
						// care what it contains):
						if (pwd != null && pwd.length() > 0) {
							// Store the value of the password temporarily so we can
							// compare it with step two, then dismiss this dialog and
							// launch the second:
							newPassword = pwd;
							removeDialog(DIALOG_PASSWORD_SET1);
							showDialog(DIALOG_PASSWORD_SET2);
						// If the password was empty, close this dialog and complain:
						} else {
							removeDialog(DIALOG_PASSWORD_SET1);
							Toast.makeText(v.getContext(),
									R.string.error_new_password_empty,
									Toast.LENGTH_LONG).show();
						}
					}
	    		});
	    		// The cancel button is easier.  We don't need to worry with changing
	    		// its text or anything.  It just needs to dismiss the dialog and
	    		// return to the activity.
	    		Button btnCancel = (Button)dialog.findViewById(R.id.btn_password_cancel);
	    		btnCancel.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						removeDialog(DIALOG_PASSWORD_SET1);
					}
	    		});
	    		break;
	    	// Step two of the password setting process prompts the user for the
	    	// password again and compares the result with the value from the first
	    	// pass:
	    	case DIALOG_PASSWORD_SET2:
	    		// This is essentially the same as above, only this time we set the
	    		// title and positive button text to say we're confirming the new
	    		// password:
	    		dialog = new Dialog(this);
	    		dialog.setContentView(R.layout.password_dialog);
	    		dialog.setTitle(R.string.dialog_password_confirm_title);
	    		Button btnSet2 = (Button)dialog.findViewById(R.id.btn_password_ok);
	    		btnSet2.setText(R.string.dialog_password_confirm_btn);
	    		btnSet2.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						// Get the value of the password box:
						EditText txtPassword =
							(EditText)(v.getRootView().findViewById(R.id.password_dialog_txt));
						String pwd = ((Editable)txtPassword.getText()).toString();
						// Now we need to compare this value to the value of the
						// previous pass.  If they match, we'll move to the next
						// step:
						if (pwd != null && pwd.length() > 0 &&
								pwd.compareTo(newPassword) == 0) {
							// We're going to let the progress dialog do the dirty
							// work for this process.  Set the current action flag
							// so it will know we're encrypting, then remove this
							// dialog and launch the progress dialog.
							currentAction = ACTION_SET_PWD;
							removeDialog(DIALOG_PASSWORD_SET2);
							showDialog(DIALOG_PROGRESS);
						// The passwords did not match:
						} else {
							removeDialog(DIALOG_PASSWORD_SET2);
							Toast.makeText(v.getContext(),
									R.string.error_new_password_nomatch,
									Toast.LENGTH_LONG).show();
						}
					}
	    		});
	    		Button btnCancel2 = (Button)dialog.findViewById(R.id.btn_password_cancel);
	    		btnCancel2.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						// When the cancel button is clicked, we not only want to
						// remove this dialog, but we need to clear the local cache
						// of the unencrypted password as well:
						removeDialog(DIALOG_PASSWORD_SET2);
						newPassword = null;
					}
	    		});
	    		break;
	    	// Clearing the password requires the user to enter the password first as
	    	// confirmation.  This is only a one-step process, unlike the above, but
	    	// the dialog follows the same basic pattern.
	    	case DIALOG_PASSWORD_CLEAR:
	    		// Same song, different chorus.  Set the title and positive button text
	    		// to say we're clearing the password:
	    		dialog = new Dialog(this);
	    		dialog.setContentView(R.layout.password_dialog);
	    		dialog.setTitle(R.string.dialog_password_clear_title);
	    		Button btnSet3 = (Button)dialog.findViewById(R.id.btn_password_ok);
	    		btnSet3.setText(R.string.dialog_password_clear_btn);
	    		btnSet3.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						// Get the password from the box:
						EditText txtPassword =
							(EditText)(v.getRootView().findViewById(R.id.password_dialog_txt));
						String pwd = ((Editable)txtPassword.getText()).toString();
						// This time, we need to confirm that the password matches the
						// one stored in the app.  Ask the app to validate it:
						if (pwd != null && pwd.length() > 0 &&
								theApp.isValidPassword(pwd)) {
							// If the password matched, remove this dialog and launch
							// the progress dialog, which will do the dirty work:
							currentAction = ACTION_CLEAR_PWD;
							removeDialog(DIALOG_PASSWORD_CLEAR);
							showDialog(DIALOG_PROGRESS);
						// The password did not match:
						} else {
							removeDialog(DIALOG_PASSWORD_CLEAR);
							Toast.makeText(v.getContext(),
									R.string.error_invalid_password,
									Toast.LENGTH_LONG).show();
						}
					}
	    		});
	    		Button btnCancel3 = (Button)dialog.findViewById(R.id.btn_password_cancel);
	    		btnCancel3.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						removeDialog(DIALOG_PASSWORD_CLEAR);
					}
	    		});
	    		break;
	    	// The act of setting or removing the password forces us to encrypt or
	    	// decrypt (respectively) the sequence keys in the database.  This is a
	    	// process that could take quite a while if there are lots of card sets
	    	// in the database.  We should display a progress dialog when this
	    	// process launches.  Build the progress dialog, then launch the thread
	    	// to do the actual crypto work.
	    	case DIALOG_PROGRESS:
	    		progressDialog = new ProgressDialog(this);
	    		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	    		progressDialog.setMax(cardsetCount + 1);
	    		// Set the message depending on which action we're taking:
	    		if (currentAction == ACTION_SET_PWD)
	    			progressDialog.setMessage(getResources().getString(R.string.dialog_progress_encrypting_title));
	    		else
	    			progressDialog.setMessage(getResources().getString(R.string.dialog_progress_decrypting_title));
	    		// Start the crypto thread:
	    		seqKeyCryptoThread = new SeqKeyCryptoThread(handler, theApp);
	    		seqKeyCryptoThread.start();
	    		dialog = progressDialog;
	    		break;
    	}
    	return dialog;
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
    	// Create the option menu:
    	menu.add(0, OPTMENU_HELP, Menu.NONE,
				R.string.optmenu_help).setIcon(android.R.drawable.ic_menu_help);
    	return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
	    	// Launch the help text for this activity:
	    	case OPTMENU_HELP:
	    		Intent i = new Intent(getBaseContext(), HelpActivity.class);;
    			i.putExtra("helptext", R.string.help_text_settings);
	    		startActivity(i);
	    		return true;
    	}
    	return false;
    }
    
    /**
     * This Handler will be responsible for receiving and processing messages sent
     * from the encryption/decryption thread.
     */
    private final Handler handler = new Handler()
    {
    	public void handleMessage(Message msg) {
    		// Get the count of the number of sequence keys processed.  If this
    		// is a positive number, update the progress dialog accordingly.
    		int countDone = msg.getData().getInt("count");
    		if (countDone >= 0) progressDialog.setProgress(countDone);
    		// If it looks like we're finished (the count done equals the count of
    		// all card sets plus one), remove the progress dialog and inform
    		// the user of our success:
    		if (countDone >= cardsetCount + 1) {
    			removeDialog(DIALOG_PROGRESS);
    			// If we were encrypting, tell the user that the password was
    			// successfully set:
    			if (currentAction == ACTION_SET_PWD) {
    				Toast.makeText(getBaseContext(),
							R.string.dialog_password_set_success,
							Toast.LENGTH_LONG).show();
					// Change the button text on the main activity to state that
    				// pressing it again will clear the password:
					btnSetPassword.setText(R.string.settings_password_btn_clear);
					// For security reasons, clear the local cache of the
					// unencrypted password:
					newPassword = null;
				// If we were decrypting, tell the user the password was
				// successfully cleared:
    			} else {
    				Toast.makeText(getBaseContext(),
							R.string.dialog_password_clear_success,
							Toast.LENGTH_LONG).show();
					// Change the button text on the main activity to state that
    				// pressing it again will set a new password:
					btnSetPassword.setText(R.string.settings_password_btn_set);
    			}
    		// If the thread reports that there were no card sets to process, we
    		// can simply remove the progress dialog and do a simple set or clear
    		// with no regard to the encryption/decryption process:
    		} else if (countDone == THREAD_ERROR_NO_CARD_SETS) {
    			removeDialog(DIALOG_PROGRESS);
    			if (currentAction == ACTION_SET_PWD) {
    				if (theApp.setPassword(newPassword)) {
        				Toast.makeText(getBaseContext(),
    							R.string.dialog_password_set_success,
    							Toast.LENGTH_LONG).show();
    					btnSetPassword.setText(R.string.settings_password_btn_clear);
    				} else 
    					Toast.makeText(getBaseContext(),
    							R.string.dialog_password_set_failure,
    							Toast.LENGTH_LONG).show();
    			} else {
    				if (theApp.clearPassword()) {
        				Toast.makeText(getBaseContext(),
    							R.string.dialog_password_clear_success,
    							Toast.LENGTH_LONG).show();
    					btnSetPassword.setText(R.string.settings_password_btn_set);
    				} else 
    					Toast.makeText(getBaseContext(),
    							R.string.dialog_password_clear_failure,
    							Toast.LENGTH_LONG).show();
    			}
    		// If the thread reports that the password could not be set, notify
    		// the user:
    		} else if (countDone == THREAD_ERROR_PASSWORD_SET_FAILED) {
				Toast.makeText(getBaseContext(),
						R.string.dialog_password_set_failure,
						Toast.LENGTH_LONG).show();
			// If the thread reports that the password could not be cleared, notify
			// the user:
    		} else if (countDone == THREAD_ERROR_PASSWORD_CLEAR_FAILED) {
				Toast.makeText(getBaseContext(),
						R.string.dialog_password_clear_failure,
						Toast.LENGTH_LONG).show();
    		// If something blew up, we'll get this generic exception error message.
			// Tell the user that the database could be corrupted (some sequence keys
			// might be encrypted while some may not).
    		} else if (countDone == THREAD_ERROR_EXCEPTION) {
    			if (currentAction == ACTION_SET_PWD) {
    				Toast.makeText(getBaseContext(),
						R.string.dialog_progress_encrypting_error,
						Toast.LENGTH_LONG).show();
    			} else {
    				Toast.makeText(getBaseContext(),
    						R.string.dialog_progress_decrypting_error,
    						Toast.LENGTH_LONG).show();
    			}
    		}
    	}
    };
    
    /**
     * This subclass of Thread will be used to do the dirty work of encrypting and
     * decrypting the sequence keys in the database.  Since this is a process that
     * may potentially be lengthy, we should do this outside the UI thread.
     * Messages should be passed back to the Handler specified in the constructor. 
     * @author Jeffrey T. Darlington
     * @version 1.0
     * @since 1.0
     */
    private class SeqKeyCryptoThread extends Thread
    {
    	/** The Handler to update our status to */
		private Handler handler;
		/** A reference to the overall application */
		private PPPApplication theApp = null;
		
		/**
		 * Our constructor
		 * @param handler
		 * @param app
		 */
		SeqKeyCryptoThread(Handler handler, PPPApplication app) {
			this.handler = handler;
			theApp = app;
		}
		
		@Override
		public void run() {
			// Get us started by declaring a Message and Bundle for passing
			// back our progress:
            Message msg = null;
            Bundle b = null;
            // We're relying on the activity to know how many card sets are in
            // the database.  It only makes sense to do the work if there are
            // card sets to work with.
            if (cardsetCount > 0) {
                // Asbestos underpants:
            	try {
            		// What we do here depends on whether we're setting or clearing
            		// the password.  Lets look at setting the password first.
            		if (currentAction == ACTION_SET_PWD) {
            			// Attempt to set the password at the application level.
            			// If that works, we'll move on to encrypting the sequence
            			// keys.
						if (theApp.setPassword(newPassword)) {
							// Attempt to encrypt the sequence keys.  Note that we
							// pass in the handler so the DB helper can notify it of
							// its progress.
							if (theApp.getDBHelper().encryptAllSequenceKeys(handler)) {
								// If that was successful, send a message to the handler
								// by bumping the card set count by one.  This should
								// signal the handler that we're done.
			            		msg = handler.obtainMessage();
			                    b = new Bundle();
			                    b.putInt("count", cardsetCount + 1);
			                    msg.setData(b);
			                    handler.sendMessage(msg);
							} else {
								// If we get a false result from the above, something
								// blew up.  Tell the handler we encountered a problem:
			            		msg = handler.obtainMessage();
			                    b = new Bundle();
			                    b.putInt("count", THREAD_ERROR_EXCEPTION);
			                    msg.setData(b);
			                    handler.sendMessage(msg);
							}
						// If setting the password failed at the app level, notify
						// the user without attempting the encryption:
						} else {
		            		msg = handler.obtainMessage();
		                    b = new Bundle();
		                    b.putInt("count", THREAD_ERROR_PASSWORD_SET_FAILED);
		                    msg.setData(b);
		                    handler.sendMessage(msg);
						}
					// If we're clearing the password:
            		} else {
            			// We want to decrypt the sequence keys before we clear the
            			// password at the app level.  Give this a try first:
            			if (theApp.getDBHelper().decryptAllSequenceKeys(handler)) {
            				// If the sequence keys were successfully decrypted, now
            				// attempt to clear the password in the app:
							if (theApp.clearPassword()) {
								// If that was successful, bump the card set count by
								// one to let the handler know we're done:
			            		msg = handler.obtainMessage();
			                    b = new Bundle();
			                    b.putInt("count", cardsetCount + 1);
			                    msg.setData(b);
			                    handler.sendMessage(msg);
			                // If clearing the password failed at the app level,
			                // notify the user:
							} else {
			            		msg = handler.obtainMessage();
			                    b = new Bundle();
			                    b.putInt("count", THREAD_ERROR_PASSWORD_CLEAR_FAILED);
			                    msg.setData(b);
			                    handler.sendMessage(msg);
							}
						// If the sequence keys failed to be decrypted, notify the
						// user:
						} else {
		            		msg = handler.obtainMessage();
		                    b = new Bundle();
		                    b.putInt("count", THREAD_ERROR_EXCEPTION);
		                    msg.setData(b);
		                    handler.sendMessage(msg);
						}
            		}
            	// If anything blew up, notify the handler:
            	} catch (Exception e) {
            		msg = handler.obtainMessage();
                    b = new Bundle();
                    b.putInt("count", THREAD_ERROR_EXCEPTION);
                    msg.setData(b);
                    handler.sendMessage(msg);
            	}
            // There were no card sets to process:
            } else {
        		msg = handler.obtainMessage();
                b = new Bundle();
                b.putInt("count", THREAD_ERROR_NO_CARD_SETS);
                msg.setData(b);
                handler.sendMessage(msg);
            }
		}
    }

}

