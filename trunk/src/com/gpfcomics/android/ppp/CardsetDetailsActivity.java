/* CardsetDetailsActivity.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          March 7, 2011
 * PROJECT:       Perfect Paper Passwords for Android
 * ANDROID V.:	  1.1
 * 
 * This activity displays a brief summary of the parameters that define a given
 * card set, as well as a few interesting statistics, like the total number of
 * strike-outs in the set.  While this data is displayed in a read-only format,
 * the option menu includes several of the same actions that can be found in the
 * context menu of the main menu list:  rename, clear toggles, delete, etc.  There
 * is also an option menu item to go to the card view activity and see the current
 * card.
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
 * This activity displays a brief summary of the parameters that define a given
 * card set, as well as a few interesting statistics, like the total number of
 * strike-outs in the set.  While this data is displayed in a read-only format,
 * the option menu includes several of the same actions that can be found in the
 * context menu of the main menu list:  rename, clear toggles, delete, etc.  There
 * is also an option menu item to go to the card view activity and see the current
 * card.
 * @author Jeffrey T. Darlington
 * @version 1.0
 * @since 1.0
 */
public class CardsetDetailsActivity extends Activity {
	
	/** This constant identifies the View option menu */
	private static final int OPTMENU_VIEW = Menu.FIRST;

	/** This constant identifies the Rename option menu */
	private static final int OPTMENU_RENAME = Menu.FIRST + 1;

	/** This constant identifies the Clear All Strikes option menu */
	private static final int OPTMENU_CLEAR_STRIKES = Menu.FIRST + 2;
	
	/** This constant identifies the Delete option menu */
	private static final int OPTMENU_DELETE = Menu.FIRST + 3;

	/** This constant identifies the Help option menu */
	private static final int OPTMENU_HELP = Menu.FIRST + 4;
	
	/** This constant identifies the Confirm Delete dialog */
	private static final int DIALOG_CONFIRM_DELETE = 1000;

	/** This constant identifies the Rename dialog */
	private static final int DIALOG_RENAME = 1001;

	/** This constant identifies the Confirm Clear All Strikes dialog */
	private static final int DIALOG_CONFIRM_CLEAR_STRIKES = 1002;
	
	/** The card set name label */
	private TextView labelCardsetName = null;
	/** The last card label */
	private TextView labelLastCardValue = null;
	/** The strike-out or toggle count label */
	private TextView labelCountTogglesValue = null;
	/** The number of columns label */
	private TextView labelNumColumnsValue = null;
	/** The number of rows label */
	private TextView labelNumRowsValue = null;
	/** The passcode length label */
	private TextView labelPasscodeLengthValue = null;
	/** The alphabet label */
	private TextView labelAlphabetValue = null;
	/** The sequence key label */
	private TextView labelSequenceKeyValue = null;

	/** A reference back to our parent application */
	private PPPApplication theApp = null;
	
	/** A reference to our database helper */
	private CardDBAdapter DBHelper = null;
	
	/** The internal database ID for the selected card set */
	private Cardset cardSet = null;

    public void onCreate(Bundle savedInstanceState) {
    	// The usual stuff.  Set our layout and get references to our parent
    	// app and the DB helper:
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cardset_details_layout);
        theApp = (PPPApplication)getApplication();
        DBHelper = theApp.getDBHelper();
        
        // Get references to our GUI elements.  These are all labels, since this
        // is just a read-only view.
        labelCardsetName = (TextView)findViewById(R.id.labelCardsetName);
        labelLastCardValue = (TextView)findViewById(R.id.labelLastCardValue);
        labelCountTogglesValue = (TextView)findViewById(R.id.labelCountTogglesValue);
        labelNumColumnsValue = (TextView)findViewById(R.id.labelNumColumnsValue);
        labelNumRowsValue = (TextView)findViewById(R.id.labelNumRowsValue);
        labelPasscodeLengthValue = (TextView)findViewById(R.id.labelPasscodeLengthValue);
        labelAlphabetValue = (TextView)findViewById(R.id.labelAlphabetValue);
        labelSequenceKeyValue = (TextView)findViewById(R.id.labelSequenceKeyValue);
        
        try {
	        // Get card set ID from the caller, then load the card set parameters
        	// from the database:
	        Bundle extras = getIntent().getExtras();
	        if (extras != null) {
	        	long cardsetId = extras.getLong(CardDBAdapter.KEY_CARDSETID);
	        	cardSet = DBHelper.getCardset(cardsetId);
	        	// If we couldn't find the card set, complain and exit:
	        	if (cardSet == null) {
	        		Toast.makeText(this, R.string.error_cardset_not_found,
	        				Toast.LENGTH_LONG).show();
	        		this.setResult(MainMenuActivity.RESPONSE_ERROR);
	        		finish();
	        	// Otherwise, get the parameters and display them.  Note that the
	        	// count of all strike-outs/toggles is not a parameter per se, but
	        	// just an interesting bit of trivia the user might want to see.
	        	} else {
	        		labelCardsetName.setText(cardSet.getName());
	        		labelLastCardValue.setText(String.valueOf(cardSet.getLastCard()));
	        		labelCountTogglesValue.setText(String.valueOf(DBHelper.getTotalToggleCount(cardSet)));
	        		labelNumColumnsValue.setText(String.valueOf(cardSet.getNumberOfColumns()));
	        		labelNumRowsValue.setText(String.valueOf(cardSet.getNumberOfRows()));
	        		labelPasscodeLengthValue.setText(String.valueOf(cardSet.getPasscodeLength()));
	        		labelAlphabetValue.setText(cardSet.getAlphabet());
	        		labelSequenceKeyValue.setText(cardSet.getSequenceKey());
	        	}
	        // The intent's extras bundle was not there:
	        } else {
        		Toast.makeText(this, R.string.error_cardset_not_found,
        				Toast.LENGTH_LONG).show();
        		this.setResult(MainMenuActivity.RESPONSE_ERROR);
        		finish();
	        }
	    // If anything blew up, complain and exit:
        } catch (Exception e) {
    		Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
    		finish();
        }
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
    	// Create the option menu:
    	menu.add(0, OPTMENU_VIEW, Menu.NONE,
        		R.string.optmenu_view).setIcon(android.R.drawable.ic_menu_view);
    	menu.add(0, OPTMENU_RENAME, Menu.NONE,
				R.string.menu_cardset_options_rename).setIcon(android.R.drawable.ic_menu_edit);
    	menu.add(0, OPTMENU_CLEAR_STRIKES, Menu.NONE,
				R.string.menu_cardset_options_clear_strikes).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    	menu.add(0, OPTMENU_DELETE, Menu.NONE,
    			R.string.menu_cardset_options_delete).setIcon(android.R.drawable.ic_menu_delete);
    	menu.add(0, OPTMENU_HELP, Menu.NONE,
				R.string.optmenu_help).setIcon(android.R.drawable.ic_menu_help);
    	return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		// View the last card of this card set:
	    	case OPTMENU_VIEW:
				Intent i1 = new Intent(getBaseContext(), CardViewActivity.class);
	        	i1.putExtra(CardDBAdapter.KEY_CARDSETID, cardSet.getCardsetId());
		    	startActivity(i1);
	    		return true;
	    	// Rename the card set:
	    	case OPTMENU_RENAME:
	    		showDialog(DIALOG_RENAME);
	    		return true;
	    	// Clear all strike-outs:
	    	case OPTMENU_CLEAR_STRIKES:
	    		showDialog(DIALOG_CONFIRM_CLEAR_STRIKES);
	    		return true;
	    	// Delete this card set:
	    	case OPTMENU_DELETE:
	    		showDialog(DIALOG_CONFIRM_DELETE);
	    		return true;
	    	// Launch the help text for this activity:
	    	case OPTMENU_HELP:
	    		Toast.makeText(getBaseContext(), R.string.error_not_implemented, Toast.LENGTH_LONG).show();
	    		return true;
    	}
    	return false;
    }

    protected Dialog onCreateDialog(int id)
    {
    	// Create a reference back to the parent activity (us) and to the dialog
    	// we wish to create:
    	final Activity caller = this;
    	Dialog dialog = null;
    	// Now let's see which dialog we need to launch:
    	switch (id) {
    		// This dialog asks for confirmation on whether or not the user really
    		// wants to delete an individual card set.  This is launched by the context
    		// menu attached to that card set in the list.
	    	case DIALOG_CONFIRM_DELETE:
	    		AlertDialog.Builder adb = new AlertDialog.Builder(this);
    			adb.setTitle(R.string.dialog_confirm_delete_title);
    			String message = getResources().getString(R.string.dialog_confirm_delete_prompt);
	    		message = message.replace(getResources().getString(R.string.meta_replace_token),
	    				cardSet.getName());
	    		adb.setMessage(message);
    			adb.setCancelable(true);
    			adb.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Delete the card set:
						if (DBHelper.deleteCardset(cardSet)) {
							Toast.makeText(caller, getResources().getString(R.string.dialog_confirm_delete_success).replace(getResources().getString(R.string.meta_replace_token), cardSet.getName()), Toast.LENGTH_LONG).show();
							finish();
						// Oops, that didn't work:
						} else {
							Toast.makeText(caller, getResources().getString(R.string.dialog_confirm_delete_failure).replace(getResources().getString(R.string.meta_replace_token), cardSet.getName()), Toast.LENGTH_LONG).show();
						}
						// Since we're displaying the name, we need to completely
						// destroy this dialog and recreate it later:
						caller.removeDialog(DIALOG_CONFIRM_DELETE);
					}
    			});
    			adb.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
    			});
    			adb.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						caller.removeDialog(DIALOG_CONFIRM_DELETE);
					}
				});
    			dialog = (Dialog)adb.create();
	    		break;
	    	// This dialog confirms with the user that they wish to clear all strike-
	    	// out data for the selected card set.  This is also launched from the
	    	// context menu for a given card set.
	    	case DIALOG_CONFIRM_CLEAR_STRIKES:
	    		AlertDialog.Builder adb2 = new AlertDialog.Builder(this);
    			adb2.setTitle(R.string.dialog_confirm_clear_all_strikes_title);
    			String message2 = getResources().getString(R.string.dialog_confirm_clear_all_strikes_prompt);
	    		message2 = message2.replace(getResources().getString(R.string.meta_replace_token),
	    				cardSet.getName());
	    		adb2.setMessage(message2);
    			adb2.setCancelable(true);
    			adb2.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Clear the strike-outs:
						if (DBHelper.clearAllTogglesForCardset(cardSet)) {
							Toast.makeText(caller, getResources().getString(R.string.dialog_confirm_clear_all_strikes_success).replace(getResources().getString(R.string.meta_replace_token), cardSet.getName()), Toast.LENGTH_LONG).show();
				        	Intent i1 = new Intent(caller, CardsetDetailsActivity.class);
				        	i1.putExtra(CardDBAdapter.KEY_CARDSETID, cardSet.getCardsetId());
				        	startActivity(i1);
						// That didn't work either:
						} else {
							Toast.makeText(caller, getResources().getString(R.string.dialog_confirm_clear_all_strikes_failure).replace(getResources().getString(R.string.meta_replace_token), cardSet.getName()), Toast.LENGTH_LONG).show();
						}
						caller.removeDialog(DIALOG_CONFIRM_CLEAR_STRIKES);
						//finish();
					}
    			});
    			adb2.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
    			});
    			adb2.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						caller.removeDialog(DIALOG_CONFIRM_CLEAR_STRIKES);
					}
				});
    			dialog = (Dialog)adb2.create();
	    		break;
	    	// The rename dialog is a bit too complex for the AlertDialog.Builder
	    	// to handle, so we have to build it ourselves:
	    	case DIALOG_RENAME:
	    		// Create the dialog and set its layout and title:
	    		dialog = new Dialog(this);
	    		dialog.setContentView(R.layout.rename_cardset_dialog);
	    		dialog.setTitle(R.string.dialog_rename_title);
	    		// Get a reference to the edit text box and populate it with the
	    		// value of the current card set name:
	    		EditText txtNewName =
	    			(EditText)dialog.findViewById(R.id.rename_dialog_name_box);
	    		txtNewName.setText(cardSet.getName());
	    		// Define what the Rename button should do:
	    		Button btnRename = (Button)dialog.findViewById(R.id.btn_rename);
	    		btnRename.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						// Get a reference to the text box:
						EditText txtName =
							(EditText)(v.getRootView().findViewById(R.id.rename_dialog_name_box));
						// Wrap this in a try/catch because there's a chance the user
						// could input an invalid value:
						try {
							// Get its value:
							String newName =
								((Editable)txtName.getText()).toString();
							// Make sure the new name isn't empty:
							if (Cardset.isValidName(newName)) {
								// Try to rename the card set:
								if (DBHelper.renameCardset(cardSet.getCardsetId(), newName)) {
									Toast.makeText(caller,getResources().getString(R.string.dialog_rename_success).replace(getResources().getString(R.string.meta_replace_token), cardSet.getName()).replace(getResources().getString(R.string.meta_replace_token2), newName), Toast.LENGTH_LONG).show();
								// The rename failed:
								} else {
									Toast.makeText(caller, getResources().getString(R.string.dialog_rename_failure).replace(getResources().getString(R.string.meta_replace_token), cardSet.getName()), Toast.LENGTH_LONG).show();
								}
								// Destroy the dialog and rebuild the card set list:
								removeDialog(DIALOG_RENAME);
								//finish();
					        	Intent i2 = new Intent(caller, CardsetDetailsActivity.class);
					        	i2.putExtra(CardDBAdapter.KEY_CARDSETID, cardSet.getCardsetId());
					        	startActivity(i2);
							// If the name isn't valid, show an error:
							} else {
								removeDialog(DIALOG_RENAME);
								Toast.makeText(caller, R.string.dialog_rename_invalid_name,
										Toast.LENGTH_LONG).show();
							}
						// The user entered an invalid name:
						} catch (Exception e) {
							removeDialog(DIALOG_RENAME);
							Toast.makeText(caller, R.string.dialog_rename_invalid_name,
									Toast.LENGTH_LONG).show();
						}
					}
	    		});
	    		// Define what the Cancel button should do:
	    		Button btnCancel = (Button)dialog.findViewById(R.id.btn_rename_cancel);
	    		btnCancel.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						// We'll force the application to completely dispose of this
						// dialog for now and rebuild it the next time.  Probably
						// not the most efficient thing to do, but it guarantees
						// we'll have a clean dialog the next time.
						removeDialog(DIALOG_RENAME);
					}
	    		});
	    		break;
    	}
    	return dialog;
    }
    
}
