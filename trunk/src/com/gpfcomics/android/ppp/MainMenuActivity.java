/* MainMenuActivity.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          February 23, 2011
 * PROJECT:       Perfect Paper Passwords for Android
 * ANDROID V.:	  1.1
 * 
 * The main menu activity for Perfect Paper Passwords.  This activity lists the
 * currently available card sets, allowing the user to add a new one, select one
 * to display cards for, or otherwise manipulate the card sets as entire sets.
 * Card sets may be renamed, deleted, or have all their "strike-through" data
 * cleared.  Aside from these features, further editing of card set data is not
 * allowed.
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
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The main menu activity for Perfect Paper Passwords.  This activity lists the
 * currently available card sets, allowing the user to add a new one, select one
 * to display cards for, or otherwise manipulate the card sets as entire sets.
 * Card sets may be renamed, deleted, or have all their "strike-through" data
 * cleared.  Aside from these features, further editing of card set data is not
 * allowed.
 * @author Jeffreyt T. Darlington
 * @version 1.0
 * @since 1.0
 */
public class MainMenuActivity extends ListActivity {

	/** This constant identifies the Details context menu */
	private static final int MENU_DETAILS = Menu.FIRST;

	/** This constant identifies the Rename context menu */
	private static final int MENU_RENAME = Menu.FIRST + 1;

	/** This constant identifies the Clear All Strikes context menu */
	private static final int MENU_CLEAR_STRIKES = Menu.FIRST + 2;
	
	/** This constant identifies the Delete context menu */
	private static final int MENU_DELETE = Menu.FIRST + 3;

	/** This constant identifies the New option menu */
	private static final int OPTMENU_NEW = Menu.FIRST + 100;
	
	/** This constant identifies the Delete All option menu */
	private static final int OPTMENU_DELETE_ALL = Menu.FIRST + 101;
	
	/** This constant identifies the Settings option menu */
	private static final int OPTMENU_SETTINGS = Menu.FIRST + 102;

	/** This constant identifies the Help option menu */
	private static final int OPTMENU_HELP = Menu.FIRST + 103;

	/** This constant identifies the About option menu */
	private static final int OPTMENU_ABOUT = Menu.FIRST + 104;

	/** This constant identifies the Confirm Delete dialog */
	private static final int DIALOG_CONFIRM_DELETE = 1000;

	/** This constant identifies the Confirm Delete All dialog */
	private static final int DIALOG_CONFIRM_DELETE_ALL = 1001;

	/** This constant identifies the Rename dialog */
	private static final int DIALOG_RENAME = 1002;

	/** This constant identifies the Confirm Clear All Strikes dialog */
	private static final int DIALOG_CONFIRM_CLEAR_STRIKES = 1003;
	
	/** This constant identifies a request to another activity launched via
	 *  startActivityForResult() that we want to create a new card set */
	static final int REQUEST_NEW_CARDSET = 5000;
	
	/** This constant identifies a request to another activity launched via
	 *  startActivityForResult() that we want to view the last card of the
	 *  selected card set. */
	static final int REQUEST_CARD_VIEW = 5001;
	
	/** This constant identifies the return result from a called activity
	 *  has ended with an error and the operation could not be completed
	 *  successfully. */
	static final int RESPONSE_ERROR = 6000;
	
	/** This constant identifies the return result from a called activity
	 *  has been successful and the requested action is complete. */
	static final int RESPONSE_SUCCESS = 6001;

	/** The New, Add, or Create button */
	private Button btnNew = null;
	
	/** A reference back to our parent application */
	private PPPApplication theApp = null;
	
	/** A reference to the database helper */
	private CardDBAdapter DBHelper = null;
	
	/** A Cursor to feed the ListView */
	private Cursor cardsetCursor = null;

	/** The internal database ID of the currently selected card set.  This is
	 *  used in dialog boxes and such and is set by the context menu selection. */
	private long selectedCardsetID = -1l;
	
	/** A string holding the name of the currently selected card set.  This is
	 *  used in dialog boxes and such and is set by the context menu selection. */
	private String selectedCardsetName = null;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	// The usual stuff.  Set our layout:
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Get references to the parent app and DB helper:
        theApp = (PPPApplication)getApplication();
        DBHelper = theApp.getDBHelper();
        
        // Register the list view to generate context menus:
        registerForContextMenu(getListView());
        
        // Get a reference to our New button and set its click listener:
        btnNew = (Button)findViewById(R.id.cardset_list_add_btn);
        btnNew.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Simple enough: If the New button is clicked, launch the
				// new card set activity.  The try/catch block was used mostly
				// for debugging, but leaving it in is a good idea, even if
				// it may not be needed.
				try {
					Intent i = new Intent(v.getContext(), NewCardActivity.class);
			    	startActivityForResult(i, REQUEST_NEW_CARDSET);
				} catch (Exception e) {
					Toast.makeText(v.getContext(), "ERROR: " + e.toString(),
							Toast.LENGTH_LONG).show();
				}
			}
        });
        
        // Now rebuild the card set list.  This can be done at many steps, so we've
        // pulled that process out into its own method.
        rebuildCardsetList();
    }
    
    @Override
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
	    				selectedCardsetName);
	    		adb.setMessage(message);
    			adb.setCancelable(true);
    			adb.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Delete the card set:
						if (DBHelper.deleteCardset(selectedCardsetID)) {
							Toast.makeText(caller, getResources().getString(R.string.dialog_confirm_delete_success).replace(getResources().getString(R.string.meta_replace_token), selectedCardsetName), Toast.LENGTH_LONG).show();
							// Make sure to rebuild the list so the deleted set
							// is no longer displayed;
							rebuildCardsetList();
						// Oops, that didn't work:
						} else {
							Toast.makeText(caller, getResources().getString(R.string.dialog_confirm_delete_failure).replace(getResources().getString(R.string.meta_replace_token), selectedCardsetName), Toast.LENGTH_LONG).show();
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
	    				selectedCardsetName);
	    		adb2.setMessage(message2);
    			adb2.setCancelable(true);
    			adb2.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Clear the strike-outs:
						if (DBHelper.clearAllTogglesForCardset(selectedCardsetID)) {
							Toast.makeText(caller, getResources().getString(R.string.dialog_confirm_clear_all_strikes_success).replace(getResources().getString(R.string.meta_replace_token), selectedCardsetName), Toast.LENGTH_LONG).show();
						// That didn't work either:
						} else {
							Toast.makeText(caller, getResources().getString(R.string.dialog_confirm_clear_all_strikes_failure).replace(getResources().getString(R.string.meta_replace_token), selectedCardsetName), Toast.LENGTH_LONG).show();
						}
						caller.removeDialog(DIALOG_CONFIRM_CLEAR_STRIKES);
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
	    	// This dialog confirms with the user that they want to delete all card
	    	// set data from the database.  This is launched from the option menu.
	    	case DIALOG_CONFIRM_DELETE_ALL:
	    		AlertDialog.Builder adb3 = new AlertDialog.Builder(this);
    			adb3.setTitle(R.string.dialog_confirm_delete_all_title);
    			adb3.setMessage(R.string.dialog_confirm_delete_all_prompt);
    			adb3.setCancelable(true);
    			adb3.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Tactical nuke:
						if (DBHelper.deleteAllCardsets()) {
							Toast.makeText(caller,
									R.string.dialog_confirm_delete_all_success,
									Toast.LENGTH_LONG).show();
							// Rebuild the list to make sure it gets cleared;
							rebuildCardsetList();
						// Rats, they were in the fallout shelter:
						} else {
							Toast.makeText(caller, 
									R.string.dialog_confirm_delete_all_failure,
									Toast.LENGTH_LONG).show();
						}
						caller.removeDialog(DIALOG_CONFIRM_DELETE);
					}
    			});
    			adb3.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
    			});
    			adb3.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						caller.removeDialog(DIALOG_CONFIRM_DELETE);
					}
				});
    			dialog = (Dialog)adb3.create();
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
	    		txtNewName.setText(selectedCardsetName);
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
								if (DBHelper.renameCardset(selectedCardsetID, newName)) {
									Toast.makeText(caller,getResources().getString(R.string.dialog_rename_success).replace(getResources().getString(R.string.meta_replace_token), selectedCardsetName).replace(getResources().getString(R.string.meta_replace_token2), newName), Toast.LENGTH_LONG).show();
								// The rename failed:
								} else {
									Toast.makeText(caller, getResources().getString(R.string.dialog_rename_failure).replace(getResources().getString(R.string.meta_replace_token), selectedCardsetName), Toast.LENGTH_LONG).show();
								}
								// Destroy the dialog and rebuild the card set list:
								removeDialog(DIALOG_RENAME);
								rebuildCardsetList();
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
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	// When a list item is clicked, the default action is to open that card set
    	// and display its last (i.e. current) card.  This is pretty simple, then:
    	// Move the cursor to the proper position, create an intent for the card
    	// view activity, and pass it the database ID for the card set to view.
    	super.onListItemClick(l, v, position, id);
    	cardsetCursor.moveToPosition(position);
    	Intent i = new Intent(this, CardViewActivity.class);
    	i.putExtra(CardDBAdapter.KEY_CARDSETID, id);
    	// Technically, we don't need a result here, so for now we'll just use
    	// startActivity() rather than startActivityForResult().  We may change
    	// that if a return code is needed later.
    	startActivity(i);
    }
    
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		// There are only three options in the context menu for each card set:
		// rename it (we won't allow the user to change any other parameters
		// for now), clear all of its strike-out data, or delete it.
		menu.setHeaderTitle(R.string.menu_cardset_options_title);
        menu.add(0, MENU_DETAILS, 0, R.string.menu_cardset_options_details);
        menu.add(0, MENU_RENAME, 1, R.string.menu_cardset_options_rename);
        menu.add(0, MENU_CLEAR_STRIKES, 2, R.string.menu_cardset_options_clear_strikes);
        menu.add(0, MENU_DELETE, 3, R.string.menu_cardset_options_delete);
    }
    
    @Override
	public boolean onContextItemSelected(MenuItem item) {
    	// This is a bit awkward, but we need to get the card set name and internal
    	// ID for the item in the list that was selected.  We can get this using
    	// the AdapterContextMenuInfo.  The ID is easy enough; that's part of the
    	// info object.  The name, however, we'll have to get by getting the target
    	// view and getting the display text from the underlying text view.  Once
    	// we have these, stuff them into a couple of private variables so the
    	// dialog boxes can use them later.
    	AdapterContextMenuInfo info =
			(AdapterContextMenuInfo)item.getMenuInfo();
		TextView tv = (TextView)info.targetView;
		selectedCardsetName = tv.getText().toString();
		selectedCardsetID = info.id;
		// Now let's see which item in the context menu was selected.  In each case,
		// we'll launch a dialog box to confirm the action or get additional
		// information.
    	switch(item.getItemId()) {
			// Show detailed information about the selected card set:
	    	case MENU_DETAILS:
	        	Intent i = new Intent(this, CardsetDetailsActivity.class);
	        	i.putExtra(CardDBAdapter.KEY_CARDSETID, selectedCardsetID);
	        	startActivity(i);
	    		return true;
    		// Rename the card set:
	    	case MENU_RENAME:
	    		showDialog(DIALOG_RENAME);
	    		return true;
	    	// Clear all strike-out data:
	    	case MENU_CLEAR_STRIKES:
	    		showDialog(DIALOG_CONFIRM_CLEAR_STRIKES);
	    		return true;
	    	// Delete the card set:
	    	case MENU_DELETE:
	    		showDialog(DIALOG_CONFIRM_DELETE);
	    		return true;
    	}
    	return super.onContextItemSelected(item);
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
    	// Create the option menu:
    	menu.add(0, OPTMENU_NEW, Menu.NONE,
        		R.string.optmenu_new).setIcon(android.R.drawable.ic_menu_add);
    	menu.add(0, OPTMENU_DELETE_ALL, Menu.NONE,
				R.string.optmenu_delete_all).setIcon(android.R.drawable.ic_menu_delete);
    	menu.add(0, OPTMENU_SETTINGS, Menu.NONE,
				R.string.optmenu_settings).setIcon(android.R.drawable.ic_menu_preferences);
    	menu.add(0, OPTMENU_HELP, Menu.NONE,
				R.string.optmenu_help).setIcon(android.R.drawable.ic_menu_help);
    	menu.add(0, OPTMENU_ABOUT, Menu.NONE,
    			R.string.optmenu_about).setIcon(android.R.drawable.ic_menu_info_details);
    	return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		// If the New option item is selected, launch the new card set
    		// activity:
	    	case OPTMENU_NEW:
				Intent i = new Intent(getBaseContext(), NewCardActivity.class);
		    	startActivityForResult(i, REQUEST_NEW_CARDSET);
	    		return true;
	    	// If the Delete All item is selected, launch the confirmation dialog:
	    	case OPTMENU_DELETE_ALL:
	    		showDialog(DIALOG_CONFIRM_DELETE_ALL);
	    		return true;
	    	// Launch the settings activity:
	    	case OPTMENU_SETTINGS:
	    		Intent i2 = new Intent(getBaseContext(), SettingsActivity.class);
		    	startActivity(i2);
	    		return true;
	    	// Launch the help text for this activity:
	    	case OPTMENU_HELP:
	    		Toast.makeText(getBaseContext(), R.string.error_not_implemented, Toast.LENGTH_LONG).show();
	    		return true;
	    	// Send the user to the About page:
	    	case OPTMENU_ABOUT:
	    		Intent i3 = new Intent(getBaseContext(), AboutActivity.class);
		    	startActivity(i3);
	    		return true;
    	}
    	return false;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	switch (requestCode) {
    		// If we're returning from the new card set activity, rebuild the card
    		// set list.  Technically, we should probably look at the result code
    		// and only bother if the new card was successfully created and saved.
    		// We'll be lazy for now and force a rebuild every time since it won't
    		// be too expensive to do, and maybe add that as an enhancement before
    		// release.
	    	case REQUEST_NEW_CARDSET:
	    		rebuildCardsetList();
	    		break;
    	}
    }
    
    /**
     * Rebuild the card set list from data in the database
     */
    private void rebuildCardsetList() {
    	// I tried doing this the way I did in Cryptnos and return a self-contained
    	// data structure rather than a Cursor, but frankly, Android is much better
    	// suited to just handing it a cursor and letting it deal with the mess
    	// behind the scenes.  It doesn't do well for separating the database
    	// completely from the UI, but it's at least convenient.  Thus, get a
    	// Cursor that lists the card set names and internal IDs.
    	cardsetCursor = DBHelper.getCardsetListMenuItems();
    	// This is important:  Android blows up if the returned cursor happens to
    	// be null, which is possible if there's no data in the database.  Thus, we
    	// need to check for this before letting the activity manage it.
    	if (cardsetCursor != null) {
    		// Let the activity do the cursor's dirty work:
	    	//startManagingCursor(cardsetCursor);
	    	// Map the name and ID to the cursor adapter and assign it to the 
	    	// list view:
	    	String[] from = new String[] { CardDBAdapter.KEY_NAME };
	        int[] to = new int[] { R.id.cardset_list_row_name };
	        SimpleCursorAdapter cardsets =
	            new SimpleCursorAdapter(this, R.layout.cardset_list_row, cardsetCursor,
	            		from, to);
	        setListAdapter(cardsets);
	    // If the cursor was null, we can't have the activity manage it.  That said,
	    // we can't ignore the card set list.  If there's nothing to display, set
	    // the list adapter to a null value.  This isn't important when the app is
	    // first loaded with a clean database, but it *is* important if the user
	    // elects to clear the database and all the previous entries must be deleted.
    	} else setListAdapter(null);
    }
}