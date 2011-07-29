/* CardViewActivity.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          July 19, 2011
 * PROJECT:       Perfect Paper Passwords for Android
 * ANDROID V.:	  1.1
 * 
 * [Description]
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

import java.util.ArrayList;

import com.gpfcomics.android.ppp.jppp.PPPengine;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class SinglePasscodeActivity extends Activity {

	/** This constant identifies the Help option menu */
	private static final int OPTMENU_HELP = Menu.FIRST;

	/** A convenience constant pointing to the "on" or "struck through" Drawable
	 *  resource for our card's ToggleButtons. */
	private static final int toggleBgOn = R.drawable.strikethru_on;
	
	/** A convenience constant pointing to the "off" or "cleared" Drawable
	 *  resource for our card's ToggleButtons. */
	private static final int toggleBgOff = R.drawable.strikethru_off;
	
	/** This array lists the letters of the alphabet to be used for our column
	 *  headings.  These may get moved to the res/values/strings.xml resource
	 *  eventually. */
	private static final String[] letters = {"A", "B", "C", "D", "E", "F", "G", "H",
		"I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T" /*, "U", "V", "W",
		"X", "Y", "Z"*/};
	
	/** A Cardset object representing the card set we are currently using. */
	private Cardset cardSet = null;

	/** The jPPP Perfect Paper Passwords engine which does the actual work of
	 *  generating passcodes */
	private PPPengine ppp = null;
	
	/** A reference back to our parent application */
	private PPPApplication theApp = null;
	
	/** A reference to our database helper */
	private CardDBAdapter DBHelper = null;
	
	/** The card set title label */
	private TextView lblCardTitle = null;

	/** The card number text box */
	private EditText txtCardNumber = null;
	
	/** The column number Spinner */
	private Spinner spinColumn = null;

	/** The row number Spinner */
	private Spinner spinRow = null;
	
	/** The passcode ToggleButton */
	private ToggleButton tbPasscode = null;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	// The usual start-up stuff.  Set the layout and grab references to the
    	// parent application and the DB helper.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.single_passcode_layout);
        theApp = (PPPApplication)getApplication();
        DBHelper = theApp.getDBHelper();

        // Asbestos underpants:
        try {
        
        	// Get handy references to our UI elements:
	        lblCardTitle = (TextView)findViewById(R.id.single_pc_cardset_label);
	        txtCardNumber = (EditText)findViewById(R.id.single_pc_cardnum_edit);
	        spinColumn = (Spinner)findViewById(R.id.single_pc_column_spin);
	        spinRow = (Spinner)findViewById(R.id.single_pc_row_spin);
	        tbPasscode = (ToggleButton)findViewById(R.id.single_pc_toggle_button);
	        
	        // This view is pretty much useless without a card set to work with,
	        // so check the intent we were called with and try to get the card
	        // set passed in:
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
	        	}
	        // If the bundle was empty, complain and exit:
	        } else {
        		Toast.makeText(this, R.string.error_cardset_not_found,
        				Toast.LENGTH_LONG).show();
        		this.setResult(MainMenuActivity.RESPONSE_ERROR);
        		finish();
	        }
	        
	        // Built the PPP engine from the card set data:
	        PPPengine.setAlphabet(cardSet.getAlphabet());
	        PPPengine.setCardColumns(cardSet.getNumberOfColumns());
	        PPPengine.setCardRows(cardSet.getNumberOfRows());
	        PPPengine.setPasscodeLength(cardSet.getPasscodeLength());
	    	ppp = new PPPengine(cardSet.getSequenceKey());

	    	// Set the card set title as well as the text box containing
	    	// the current/last card number:
	        lblCardTitle.setText(cardSet.getName());
	        txtCardNumber.setText(String.valueOf(cardSet.getLastCard()));
	        
	        // Set up the row number Spinner.  I wish this was more elegant, but
	        // we need to build a list of strings containing the numbers one
	        // through the number of rows in the card.  We'll assign that list to
	        // an adapter, then assign the adapter to the Spinner.  For now, we'll
	        // set the spinner to the first item in the list.
	        ArrayList<String> rowList = new ArrayList<String>();
	        for (int i = 1; i <= cardSet.getNumberOfRows(); i++)
	        	rowList.add(String.valueOf(i));
	        ArrayAdapter<String> rowAdapter = new ArrayAdapter<String>(this,
	        		android.R.layout.simple_spinner_item, rowList);
	        rowAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	        spinRow.setAdapter(rowAdapter);
	        spinRow.setSelection(0);
	        
	        // Next, do the same for the column spinner.  This is essentially the
	        // same except that we're using the letters array rather than the
	        // numbers themselves for the values.
	        ArrayList<String> colList = new ArrayList<String>();
	        for (int i = 0; i < cardSet.getNumberOfColumns(); i++)
	        	colList.add(letters[i]);
	        ArrayAdapter<String> colAdapter = new ArrayAdapter<String>(this,
	        		android.R.layout.simple_spinner_item, colList);
	        colAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	        spinColumn.setAdapter(colAdapter);
	        spinColumn.setSelection(0);
	        
	        // Define the OnCheckedChangeListener for the ToggleButton.  This
	        // listener will be responsible for changing the visual "strike through"
	        // element of the button.  Since we're not changing the text of the
	        // button, we need something to visually convey that the button has been
	        // toggled.  For our purposes, a passcode that has been used should be
	        // "stricken" to indicate it is no longer usable.  Rather than change the
	        // button text like the default (since we're using that text to display
	        // the passcode), we'll toggle the background.  If it's clear, the button
	        // is "off" and the passcode is "clear".  If the button is "on", the
	        // passcode has been "stricken" and will appear with a line through it.
	        // This has to occur in a separate listener than the code for checking for
	        //user-initiated toggles so we can toggle the buttons programmatically.
	        tbPasscode.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton button,
						boolean isChecked) {
					if (isChecked) button.setBackgroundResource(toggleBgOn);
					else button.setBackgroundResource(toggleBgOff);
				}
	        });
	        
	        // Define the OnClickListener for the ToggleButton.  This listener
	        // handles what occurs when the user toggles the button, which in our case
	        // will record the "strike through" state of the passocde to the database.
	        // We divorce this from the visual "strike through" effect so this only
	        // gets fired when the user initiates it, while the visual effect should
	        // occur even if we toggle the button programmatically.
	        tbPasscode.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
			    	try {
			    		// Get the card, row, and column numbers from the various
			    		// UI elements:
			    		int cardNumber = Integer.parseInt(txtCardNumber.getText().toString());
			    		int row = spinRow.getSelectedItemPosition() + 1;
			    		int col = spinColumn.getSelectedItemPosition() + 1;
			    		// Toggle the actual passcode in the database:
						DBHelper.tooglePasscode(cardSet.getCardsetId(),
								cardNumber, col, row);
						// Get a reference to the toggle button:
						ToggleButton tb = (ToggleButton)v;
						// If the button is checked (i.e. the passcode has been
						// stricken) and the user has the "copy passcode to clipboard"
						// preference turned on, copy the password to the clipboard:
						if (tb.isChecked() && theApp.copyPasscodesToClipboard()) {
							ClipboardManager clippy =
								ClipboardManager.newInstance(theApp);
							clippy.setText(tb.getTextOn());
							Toast.makeText(v.getContext(), 
									getResources().getString(R.string.cardview_passcode_copied).replace(getResources().getString(R.string.meta_replace_token), letters[col - 1] + row),
									Toast.LENGTH_SHORT).show();
						}
					// If something blew up, complain:
			    	} catch (Exception e) {
			        	Toast.makeText(getBaseContext(), "ERROR: " + e.getMessage(),
								Toast.LENGTH_LONG).show();
			    	}
				}
	        });
	        
	        // Query the database and see if we can find the last passcode that
	        // was toggled for the current card.  This will either return an
	        // integer array with the column and row number of the passcode, or
	        // a null if no passcodes have been toggled or something blew up.
	        int[] lastToggle =
	        	DBHelper.getLastToggledPasscodeForCard(cardSet.getCardsetId(),
	        			cardSet.getLastCard());
	        // If we got anything back, operate on that data.  If we got a null,
	        // we'll silently keep the defaults already set above.
	        if (lastToggle != null) {
	        	// Get convenience copies of the column and row number:
	        	int col = lastToggle[0];
	        	int row = lastToggle[1];
	        	// Increment the column by one.  If that exceed the card's column
	        	// size, reset the column number to one and bump up the row number.
	        	col++;
	        	if (col > cardSet.getNumberOfColumns()) {
	        		col = 1;
	        		row++;
	        	}
	        	// Now look at the row number.  If that is now too large, reset it
	        	// to one and bump the card number up by one.  In this case, we'll
	        	// need to save the new card number to the database so we'll
	        	// remember the change.
	        	if (row > cardSet.getNumberOfRows()) {
	        		row = 1;
	        		cardSet.setLastCard(cardSet.getLastCard() + 1);
	        		DBHelper.saveCardset(cardSet);
	        	}
	        	// Now populate the UI elements with the new adjusted values.  This
	        	// should now put us on the first passcode after the last toggled
	        	// one for the last selected card.
	        	txtCardNumber.setText(String.valueOf(cardSet.getLastCard()));
	        	spinColumn.setSelection(col - 1);
	        	spinRow.setSelection(row - 1);
	        }
	        
	        // Now that we've adjusted where we are, fetch the passcode and its
	        // toggle state based on the current state of the database:
	        fetchPasscode();

	        // Set up the listener for our two spinners.  Since these spinners
	        // will do essentially the same thing from a functionality standpoint,
	        // it makes sense to build a single listener and assign it to both.
	        // If the value of the spinner changes, get the new passcode based
	        // on the new value.
	        OnItemSelectedListener spinListener = new OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					fetchPasscode();
				}
				public void onNothingSelected(AdapterView<?> arg0) {
				}
	        };
	        
	        // Now assign the listener to the spinners:
	        spinColumn.setOnItemSelectedListener(spinListener);
	        spinRow.setOnItemSelectedListener(spinListener);
	        
	        // Set up a key listener on the card number text box.  When the value
	        // of this box changes, parse the number in the box and get the
	        // appropriate passcode.
	        txtCardNumber.setOnKeyListener(new OnKeyListener() {
				public boolean onKey(View arg0, int arg1, KeyEvent arg2) {
					// Get the value of the box as a string:
					String cardNumber = txtCardNumber.getText().toString();
					// If the box is empty, don't do anything yet:
					if (cardNumber != null && cardNumber.length() > 0) {
						try {
							// Try to parse the string as a long.  Technically, this
							// shouldn't fail because Android itself enforces the
							// "number" restriction on the field.  However, we'll put
							// this in a try/catch block just in case.
							long longCardNum = Long.parseLong(cardNumber);
							// We're using a long in this case because the user could
							// in theory enter a card number that is too large.  We'll
							// test for that condition by making sure this long value
							// is greater than zero and less than or equal to the
							// maximum value of an integer.
							if (longCardNum > 0l &&
									longCardNum <= (long)Integer.MAX_VALUE) {
								// If the long card number looks good, update the
								// card set object and save it to the database:
								cardSet.setLastCard((int)longCardNum);
								DBHelper.saveCardset(cardSet);
								// Now fetch the passcode information:
								fetchPasscode();
							// If the card number is too big, complain:
							} else {
					        	Toast.makeText(getBaseContext(), 
					        			R.string.error_invalid_card_num,
										Toast.LENGTH_LONG).show();
							}
						// If something blew up, complain:
						} catch (Exception e) {
				        	Toast.makeText(getBaseContext(), 
				        			R.string.error_invalid_card_num,
									Toast.LENGTH_LONG).show();
						}
					}
					return false;
				}
	        });
	        
	    // If something blew up, complain and exit:
        } catch (Exception e) {
        	Toast.makeText(getBaseContext(), "ERROR: " + e.getMessage(),
					Toast.LENGTH_LONG).show();
    		finish();
        }
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
    			i.putExtra("helptext", R.string.help_text_singepasscode_view);
	    		startActivity(i);
	    		return true;
    	}
    	return false;
    }
    
    /**
     * Given the current state of the UI elements, generate the passcode for
     * that data and fetch its toggle state from the database.  Then apply this
     * new data to the passcode toggle button.
     */
    private void fetchPasscode() {
    	try {
    		// Get the card, row, and column numbers from the UI:
    		int cardNumber = Integer.parseInt(txtCardNumber.getText().toString());
    		int row = spinRow.getSelectedItemPosition() + 1;
    		int col = spinColumn.getSelectedItemPosition() + 1;
    		// Use the PPP engine to generate the passcode:
    		String passcode = ppp.getPasscode(cardNumber, col, row);
    		// Set the toggle button's text to the passcode value:
    		tbPasscode.setTextOn(passcode);
    		tbPasscode.setTextOff(passcode);
    		// Now set the checked state of the toggle button based on the data
    		// in the database.  Note that this will trigger the
    		// OnCheckedChangeListener for the toggle button and strike out the
    		// passcode if appropriate.
    		tbPasscode.setChecked(DBHelper.getToggleStateForPasscode(cardSet.getCardsetId(),
    				cardNumber, col, row));
    	// Something blew up:
    	} catch (Exception e) {
        	Toast.makeText(getBaseContext(), "ERROR: " + e.getMessage(),
					Toast.LENGTH_LONG).show();
    	}
    }

}
