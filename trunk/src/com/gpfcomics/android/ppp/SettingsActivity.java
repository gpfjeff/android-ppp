/* SettingsActivity.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          February 24, 2011
 * PROJECT:       Perfect Paper Passwords for Android
 * ANDROID V.:	  1.1
 * 
 * [Description]
 * 
 * This program is Copyright 2011, Jeffrey T. Darlington.
 * E-mail:  android_apps@gpf-comics.com
 * Web:     http://www.gpf-comics.com/
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
import android.os.Bundle;
import android.text.Editable;
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

	/** The Set/Clear Password button */
	private Button btnSetPassword = null;
	
	/** The copy passcodes to the clipboard checkbox */
	private CheckBox chkCopyPasscodes = null;
	
	/** A reference back to the parent application */
	private PPPApplication theApp = null;
	
	/** This string holds the result of the first new password dialog box so it can
	 *  be compared with the result of the second dialog before the password is
	 *  actually set. */
	private String newPassword = null;
	
	public void onCreate(Bundle savedInstanceState) {
        // The usual GUI setup stuff:
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        // Get a reference back to the app where the settings are handled:
        theApp = (PPPApplication)getApplication();
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
        // in dialogs, so this actually won't do too much.
        btnSetPassword.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//Toast.makeText(v.getContext(),
				//		R.string.error_not_implemented,
				//		Toast.LENGTH_LONG).show();
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
	    	// passs:
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
						// previous pass.  If they match, we'll attempt to save the
						// new password.
						if (pwd != null && pwd.length() > 0 &&
								pwd.compareTo(newPassword) == 0) {
							newPassword = null;
							// Tell the app to store the password.  This will take
							// care of encrypting it and putting it in the preferences
							// file for us.
							if (theApp.setPassword(pwd)) {
								Toast.makeText(v.getContext(),
										R.string.dialog_password_set_success,
										Toast.LENGTH_LONG).show();
								// If we succeeded, change the button text on the main
								// activity to state that pressing it again will clear
								// the password:
								btnSetPassword.setText(R.string.settings_password_btn_clear);
							// If the app couldn't save the password, complain:
							} else Toast.makeText(v.getContext(),
									R.string.dialog_password_set_failure,
									Toast.LENGTH_LONG).show();
							removeDialog(DIALOG_PASSWORD_SET2);
						// If the passwords did not match, complain:
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
						removeDialog(DIALOG_PASSWORD_SET2);
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
							// If the password looks valid, try to clear it:
							if (theApp.clearPassword()) {
								Toast.makeText(v.getContext(),
										R.string.dialog_password_clear_success,
										Toast.LENGTH_LONG).show();
								// If we succeeded, change the button text on the main
								// activity to state that pressing it again will set
								// a new password:
								btnSetPassword.setText(R.string.settings_password_btn_set);
							// If that failed, complain:
							} else Toast.makeText(v.getContext(),
									R.string.dialog_password_clear_failure,
									Toast.LENGTH_LONG).show();
							removeDialog(DIALOG_PASSWORD_CLEAR);
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
    	}
    	return dialog;
    }

}
