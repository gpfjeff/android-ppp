/* PasswordPromptActivity.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          February 23, 2011
 * PROJECT:       Perfect Paper Passwords for Android
 * ANDROID V.:	  1.1
 * 
 * The Password Prompt activity is the front gate for Perfect Paper Passwords.  If the
 * user has set a password for the application, this activity will prompt the user to
 * enter the password before they are allowed to continue.  If there is no password
 * set, this activity transparently passes control to the Main Menu activity.
 * 
 * If a password is set, the user cannot pass to the Main Menu until the correct
 * password is entered.  However, in the event that the user forgets their password,
 * they can elect to clear the password at the expense of wiping the database and
 * losing all their saved card set data.  This allows some level of recovery while
 * still preserving the security of the stored data.
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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The Password Prompt activity is the front gate for Perfect Paper Passwords.  If the
 * user has set a password for the application, this activity will prompt the user to
 * enter the password before they are allowed to continue.  If there is no password
 * set, this activity transparently passes control to the Main Menu activity.
 * 
 * If a password is set, the user cannot pass to the Main Menu until the correct
 * password is entered.  However, in the event that the user forgets their password,
 * they can elect to clear the password at the expense of wiping the database and
 * losing all their saved card set data.  This allows some level of recovery while
 * still preserving the security of the stored data.
 * @author Jeffrey T. Darlington
 * @version 1.0
 * @since 1.0
 */
public class PasswordPromptActivity extends Activity {
	
	/** This constant identifies the Clear Password option menu */
	private static final int OPTMENU_CLEAR_PASSWORD = 7530;
	
	/** This constant identifies the Help option menu */
	private static final int OPTMENU_HELP = 7531;
	
	/** This constant identifies the Help option menu */
	private static final int OPTMENU_ABOUT = 7532;
	
	/** This constant identifies the Clear Password dialog */
	private static final int DIALOG_CLEAR_PASSWORD = 7550;
	
	/** A reference to our parent application */
	private PPPApplication theApp = null;
	
	/** A reference to the app's common database handler */
	private CardDBAdapter DBHelper = null;
	
	/** A reference to our password text box */
	private EditText txtPassword = null;
	
	/** A reference to our Unlock button */
	private Button btnUnlock = null;
	
	/** A reference to our version label */
	private TextView lblVersion = null;
	
    public void onCreate(Bundle savedInstanceState) {
    	// The usual start-up stuff.  Declare our layout:
        super.onCreate(savedInstanceState);
        setContentView(R.layout.password_layout);
        
        // Get references to the parent app and the DB helper:
        theApp = (PPPApplication)getApplication();
        DBHelper = theApp.getDBHelper();
        
        // Should we prompt for the password?  If one has been set, we need to
        // validate it.  If not, the user should pass right through to the main
		// menu.
        if (theApp.promptForPassword()) {
        
        	// Get references to the password text box, unlock button, and version
        	// label:
	        txtPassword = (EditText)findViewById(R.id.password_box);
	        btnUnlock = (Button)findViewById(R.id.password_unlock_button);
	        lblVersion = (TextView)findViewById(R.id.password_version_label);
	        
	        // This is a lot of work for a nicety, but get the version name string
	        // from the application and add that into the version label.  If for
	        // some reason this fails, we'll just empty out the version label.
	        if (PPPApplication.getVersionName() != null) {
	        	String versionString = getResources().getString(R.string.password_version_label);
		        versionString = versionString.replace(getResources().getString(R.string.meta_replace_token),
		        		PPPApplication.getVersionName());
		        lblVersion.setText(versionString);
	        } else {
	        	lblVersion.setText("");
	        }
	        
	        // Set the listener on the button.  This will do the work of validating
	        // the password and letting us in:
	        btnUnlock.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					// Get the password text and try to validate it:
					String password = ((Editable)txtPassword.getText()).toString();
					if (theApp.isValidPassword(password)) {
						// If the password is valid, let the user go on to the
						// main menu:
			        	Intent i = new Intent(v.getContext(), MainMenuActivity.class);
			        	startActivity(i);
						finish();
			        // If the password was invalid, warn the user and clear the
			        // password box.  Note that we don't do any sort limiting on the
			        // number of tries; we could, but we won't bother for now.
					} else {
						Toast.makeText(v.getContext(), R.string.error_invalid_password,
								Toast.LENGTH_LONG).show();
						txtPassword.setText("");
					}
				}
	        });
	    // If there's no password to validate, just move the user on to the main
	    // menu:
        } else {
        	// Note the use of finish() here.  finish() completely closes this Activity,
        	// removing it from the Back button's stack.  Thus, if no password has been
        	// set and we don't want to prompt for one, we effectively "skip over" this
        	// activity and "replace" it with the main menu.  If the user taps Back
        	// from there, they will be returned to wherever they came from rather than
        	// back here.
        	Intent i = new Intent(this, MainMenuActivity.class);
        	startActivity(i);
			finish();
        }
    }
    
	public boolean onCreateOptionsMenu(Menu menu) {
    	// Add the "Clear Password" menu item:
    	menu.add(0, OPTMENU_CLEAR_PASSWORD, Menu.NONE,
    		R.string.optmenu_clear_password).setIcon(android.R.drawable.ic_menu_delete);
    	// Add the "Help" menu item:
    	menu.add(0, OPTMENU_HELP, Menu.NONE,
            	R.string.optmenu_help).setIcon(android.R.drawable.ic_menu_help);
    	// Add the "About" menu item:
    	menu.add(0, OPTMENU_ABOUT, Menu.NONE,
            	R.string.optmenu_about).setIcon(android.R.drawable.ic_menu_info_details);
    	return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
    	// Which menu item did the user select?
    	switch (item.getItemId()) {
    		// This is pretty simple:  If the user chooses to clear the password,
    		// launch the Clear Password dialog:
	    	case OPTMENU_CLEAR_PASSWORD:
	    		showDialog(DIALOG_CLEAR_PASSWORD);
	    		return true;
	    	// If the Help item is selected, open up the help page for this
	    	// Activity:
	    	case OPTMENU_HELP:
	        	Intent i1 = new Intent(this, HelpActivity.class);
	        	i1.putExtra("helptext", R.string.help_text_password_prompt);
	        	startActivity(i1);
	    		return true;
	    	// Launch the about activity:
	    	case OPTMENU_ABOUT:
	    		Intent i2 = new Intent(this, AboutActivity.class);
	        	startActivity(i2);
	    		return true;
    	}
    	return false;
    }

    protected Dialog onCreateDialog(int id)
    {
		// Create references for the activity (for context) and the dialog to
		// create:
    	final Activity caller = this;
    	Dialog dialog = null;
    	switch (id) {
    		// We want the user to be able to regain access to the program if they
    		// forget their password, but we don't want to compromise their data.
    		// Thus, we'll provide them with the ability to clear the password, at
    		// the expense of deleting all their data in the database if they do.
    		// This confirmation dialog double-checks with the user to make sure
    		// they really want to execute this destructive procedure.
	    	case DIALOG_CLEAR_PASSWORD:
	    		AlertDialog.Builder adb = new AlertDialog.Builder(this);
    			adb.setTitle(R.string.optmenu_clear_password);
    			adb.setMessage(R.string.dialog_clear_password);
    			adb.setCancelable(true);
    			adb.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Clear out the database...
						if (DBHelper.deleteAllCardsets()) {
							// Then remove the password:
							if (theApp.clearPassword()) {
								// If both of those were successful, take the user on
								// to the main menu, which should now be blank:
					        	Intent i = new Intent(caller, MainMenuActivity.class);
					        	startActivity(i);
								finish();
					        // Clearing the password failed:
							} else {
								Toast.makeText(caller,
										R.string.error_database_cleared_clear_password_failed,
										Toast.LENGTH_LONG).show();
							}
						// Clearing the database failed:
						} else {
							Toast.makeText(caller,
									R.string.error_database_not_cleared_clear_password_failed,
									Toast.LENGTH_LONG).show();
						}
						caller.dismissDialog(DIALOG_CLEAR_PASSWORD);
					}
    			});
    			adb.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
    			});
    			adb.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						caller.dismissDialog(DIALOG_CLEAR_PASSWORD);
					}
				});
    			dialog = (Dialog)adb.create();
	    		break;
    	}
    	return dialog;
    }
}
