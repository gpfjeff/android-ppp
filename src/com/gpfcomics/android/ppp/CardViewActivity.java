/* CardViewActivity.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          February 23, 2011
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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.ClipboardManager;
import android.text.Editable;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.gpfcomics.android.ppp.jppp.PPPengine;;

/**
 * This Card View Activity does the actual work of displaying a Perfect Paper Passwords
 * card.  Given a Cardset definition, it displays the "last" or "current" card for
 * that card set, as well as providing an interface for moving from card to card and
 * "striking out" used passcodes.
 * @author Jeffrey T. Darlington
 * @version 1.0
 * @since 1.0
 */
public class CardViewActivity extends Activity {
	
	/** A constant indicating that we should show the progress dialog during
	 *  the card rebuilding process. */
	private static final int DIALOG_PROGRESS = 1234567;
	
	/** A constant indicating that we should show the Go To Card dialog */
	private static final int DIALOG_GOTO = 1234568;
	
	/** A constant indicating that we should show the Clear Toggles dialog */
	private static final int DIALOG_CLEAR_TOGGLES = 1234569;
	
	/** A constant identifying the Go To option menu item */
	private static final int OPTMENU_GOTO = 54320;
	
	/** A constant identifying the Clear Toggles option menu item */
	private static final int OPTMENU_CLEAR_TOGGLES = 54321;
	
	/** A constant identifying the Settings option menu item */
	private static final int OPTMENU_SETTINGS = 54322;
	
	/** A constant identifying the Help option menu item */
	private static final int OPTMENU_HELP = 54323;
	
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
		"I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X",
		"Y", "Z"};
	
	/** A "seed" value which will be applied to the internal IDs of the card's
	 *  ToggleButtons.  This will help us identify which button was pressed.  All
	 *  ToggleButtons on the card must have this value added into the final ID
	 *  number. */
	private static final int btnIdSeed = 1000000;
	
	/** An "offset" value which will be applied to a ToggleButton's row coordinate
	 *  before the row and column values are factored into the button's internal
	 *  ID. */
	private static final int btnRowOffset = 1000;
	
	/** The Previous Card button */
	private Button btnPrevious = null;
	
	/** The Next Card button */
	private Button btnNext = null;
	
	/** The card set title label */
	private TextView lblCardTitle = null;
	
	/** The card number label */
	private TextView lblCardNumber = null;
	
	/** A handy reference to the ProgressDialog used when rebuilding cards */
	private ProgressDialog progressDialog = null;

	/** A Cardset object representing the card set we are currently using. */
	private Cardset cardSet = null;

	/** The jPPP Perfect Paper Passwords engine which does the actual work of
	 *  generating passcodes */
	private PPPengine ppp = null;
	
	/** A reference back to our parent application */
	private PPPApplication theApp = null;
	
	/** A reference to our database helper */
	private CardDBAdapter DBHelper = null;
	
	/** This worker thread allows us to move the computationally expensive passcode
	 *  generation step into a different thread from the UI.  This is required for
	 *  a good Android user experience. */
	private CardBuilderThread cardBuilderThread = null;
	
	/** The total number of passcodes on the card.  This should be the product of
	 *  the number of rows and the number of columns on the card.  This is pulled into
	 *  a variable so we only have to compute the value once. */
	private int totalPasscodes = 0;
	
	/** This two-dimensional Boolean array temporarily stores the toggled or "strike
	 *  through" state of the card's various ToggleButtons.  The first dimension is
	 *  for rows and the second for columns.  If a passcode has been "struck", the
	 *  value for its position will be true; otherwise it will be false.  Note that
	 *  array indices are zero-based while the row/column numbers are one-based. */
	private boolean[][] toggles = null;
	
	/** This two-dimensional String array temporarily stores the generated passcode
	 *  values for the card's various ToggleButtons.  The first dimension is
	 *  for rows and the second for columns.  Note that array indices are zero-based
	 *  while the row/column numbers are one-based. */
	private String[][] passcodes = null;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	// The usual start-up stuff.  Set the layout and grab references to the
    	// parent application and the DB helper.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.card_layout);
        theApp = (PPPApplication)getApplication();
        DBHelper = theApp.getDBHelper();
        // Asbestos underpants:
        try {
	        // Get card set ID from the caller, then load the card set parameters
        	// from the database:
	        Bundle extras = getIntent().getExtras();
	        if (extras != null) {
	        	long cardsetId = extras.getLong(CardDBAdapter.KEY_CARDSETID);
	        	cardSet = DBHelper.getCardset(cardsetId);
	        	if (cardSet == null) {
	        		Toast.makeText(this, R.string.error_cardset_not_found,
	        				Toast.LENGTH_LONG).show();
	        		this.setResult(MainMenuActivity.RESPONSE_ERROR);
	        		finish();
	        	}
	        } else {
        		Toast.makeText(this, R.string.error_cardset_not_found,
        				Toast.LENGTH_LONG).show();
        		this.setResult(MainMenuActivity.RESPONSE_ERROR);
        		finish();
	        }
	        
	        // Go ahead and compute the total number of passcodes on the card.  This
	        // is the product of rows and columns.
	        totalPasscodes = cardSet.getNumberOfColumns() *
	        	cardSet.getNumberOfRows();
	        
	        // ScrollView provides us with a mechanism for scrolling vertically, but
	        // not horizontally.  This puts a limit on how many columns we can display.
	        // If the card is too wide to be displayed in portrait mode, force the
	        // card to be displayed in landscape mode only.  Otherwise, let whatever
	        // default take precedence for orientation (user choice, the sensor, etc.).
	        if (cardSet.requiresLandscape())
	        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	        else
	        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	        
	        // Initialize the PPP engine given the parameters specified within
	        // the card set object.  I'm not sure why the other parameters besides
	        // the sequence key are static method calls, but they are.  We might
	        // tweak that before we release the code.
	        PPPengine.setAlphabet(cardSet.getAlphabet());
	        PPPengine.setCardColumns(cardSet.getNumberOfColumns());
	        PPPengine.setCardRows(cardSet.getNumberOfRows());
	        PPPengine.setPasscodeLength(cardSet.getPasscodeLength());
	    	ppp = new PPPengine(cardSet.getSequenceKey());
	        
	    	// Get handier references to our buttons and labels:
	        btnPrevious = (Button)this.findViewById(R.id.card_previous_button);
	        btnNext = (Button)this.findViewById(R.id.card_next_button);
	        lblCardTitle = (TextView)this.findViewById(R.id.card_title_label);
	        lblCardNumber = (TextView)this.findViewById(R.id.card_number_label);
	        
	        // Set the labels to their initial values.  The card set title won't
	        // change, but the card number will as we scroll through cards.
	        lblCardTitle.setText(cardSet.getName());
	        lblCardNumber.setText(getResources().getString(R.string.cardview_card_num_prompt).replace(getResources().getString(R.string.meta_replace_token), String.valueOf(cardSet.getLastCard())));
	        
	        // If we're already on the first card, disable the Previous button
	        // so the user can't go back any further.  Theoretically, there is
	        // no maximum, but since we're dealing with signed integers, we'll
	        // cap the Next button with the maximum integer value.
	        if (cardSet.getLastCard() == Cardset.FIRST_CARD)
	        	btnPrevious.setEnabled(false);
	        if (cardSet.getLastCard() == Cardset.FINAL_CARD)
	        	btnNext.setEnabled(false);
	        
	        // Set the Previous button listener:
	        btnPrevious.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					// This is simple enough:  Move the card set to the previous
					// card and update the label and database.  If we're now on
					// the first card, disable the Previous button and make sure
					// the Next button is always enabled.  Then rebuild the card
					// itself so the correct passcodes are displayed.
					cardSet.previousCard();
			        btnNext.setEnabled(true);
					if (cardSet.getLastCard() == Cardset.FIRST_CARD)
						btnPrevious.setEnabled(false);
					// Save the card set to the database:
					DBHelper.saveCardset(cardSet);
					rebuildCard();
			        lblCardNumber.setText(getResources().getString(R.string.cardview_card_num_prompt).replace(getResources().getString(R.string.meta_replace_token), String.valueOf(cardSet.getLastCard())));
				}});
	        
	        // Set the Previous button listener:
	        btnNext.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					// This is simple enough:  Move the card set to the next
					// card and update the label and database.  If we're now on
					// the last card, disable the Next button and make sure
					// the Previous button is always enabled.  Then rebuild the card
					// itself so the correct passcodes are displayed.
					cardSet.nextCard();
					btnPrevious.setEnabled(true);
					if (cardSet.getLastCard() == Cardset.FINAL_CARD)
						btnNext.setEnabled(false);
					// Save the card set to the database:
					DBHelper.saveCardset(cardSet);
					rebuildCard();
			        lblCardNumber.setText(getResources().getString(R.string.cardview_card_num_prompt).replace(getResources().getString(R.string.meta_replace_token), String.valueOf(cardSet.getLastCard())));
				}});
	
	        // Define the OnCheckedChangeListener for the ToggleButtons.  All of the
	        // buttons will share the same listener since they all do essentially the
	        // same thing.  This listener will be responsible for changing the visual
	        // "strike through" element of the button.  Since we're not changing the
	        // text of the button, we need something to visually convey that the
	        // button has been toggled.  For our purposes, a passcode that has been
	        // used should be "stricken" to indicate it is no longer usable.  Rather
	        // than change the button text like the default (since we're using that
	        // text to display the passcode), we'll toggle the background.  If it's
	        // clear, the button is "off" and the passcode is "clear".  If the button
	        // is "on", the passcode has been "stricken" and will appear with a line
	        // through it.  This has to occur in a separate listener than the code
	        // for checking for user-initiated toggles so we can toggle the buttons
	        // programmatically.
	        OnCheckedChangeListener onCheckedChangeListener =
	        	new OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton button, boolean isChecked) {
					if (isChecked) button.setBackgroundResource(toggleBgOn);
					else button.setBackgroundResource(toggleBgOff);
				}
	        };
	        
	        // Define the OnClickListener for the ToggleButtons.  All of the buttons
	        // will share the same listener since they all do essentially the same
	        // thing.  This listener handles what occurs when the user toggles the
	        // button, which in our case will record the "strike through" state of
	        // the passocde to the database.  We divorce this from the visual "strike
	        // through" effect so this only gets fired when the user initiates it,
	        // while the visual effect should occur even if we toggle the button
	        // programmatically.
	        OnClickListener onClickListener = new OnClickListener() {
				public void onClick(View v) {
					try {
						// Determine the row and column of the tapped button.  By using
						// the ID, we can assign a row/column value without resorting to
						// creating a subclass of ToggleButton.  If we store the card set
						// ID and card number at the activity level, we can map the four
						// values to we can store the toggle data in the database.
						int id = v.getId() - btnIdSeed;
						int row = id / btnRowOffset;
						int col = id - (row * btnRowOffset);
						// Store the toggle's new value in the database:
						DBHelper.tooglePasscode(cardSet.getCardsetId(),
								cardSet.getLastCard(), col, row);
						// Now get a reference to the clicked toggle button.  This should
						// be a safe cast since only toggle buttons will be assigned this
						// listener.
						ToggleButton tb = (ToggleButton)v;
						// Now lets get the state of the button.  If it's being struck
						// through (i.e. the state is now "on") and the user has set the
						// appropriate preference, copy the value of the passcode to the
						// clipboard so it can be easily pasted into whatever form
						// requires it.
						if (tb.isChecked() && theApp.copyPasscodesToClipboard()) {
							ClipboardManager clippy =
								(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
							clippy.setText(tb.getTextOn());
							Toast.makeText(v.getContext(), 
									getResources().getString(R.string.cardview_passcode_copied).replace(getResources().getString(R.string.meta_replace_token), letters[col - 1] + row),
									Toast.LENGTH_LONG).show();
						}
					// Hopefully we won't need this, but we definitely need to catch
					// potential bugs:
					} catch (Exception e) {
						Toast.makeText(getBaseContext(), "ERROR: " + e.getMessage(),
								Toast.LENGTH_LONG).show();
						
					}
				}
	        };
	        
	    	// Get the table layout where we'll build our card:
	        TableLayout tl = (TableLayout)findViewById(R.id.cardtable);
	        // We need some sort of placeholder to put as the toggle button on/off
	        // text.  We'll use a StringBuilder to make a string the same length
	        // as the passcodes.
	        StringBuilder sb = new StringBuilder(cardSet.getPasscodeLength());
	        for (int i = 0; i < cardSet.getPasscodeLength(); i++)
	        	sb.append("x");
	        String dummy = sb.toString();
	        // We'll loop through each row and build it dynamically.  Conveniently,
	        // row zero will be our header, which means our actual button rows will
	        // start at one, matching the value we want in the database.
	        for (int row = 0; row <= cardSet.getNumberOfRows(); row++) {
	        	// Create the table row and define a few parameters:
	        	TableRow tr = new TableRow(this);
	            tr.setLayoutParams(new LayoutParams(
	                    LayoutParams.FILL_PARENT,
	                    LayoutParams.WRAP_CONTENT));
	            // Now loop through the columns.  Like the rows, column zero will
	            // be our row header.
	            for (int col = 0; col <= cardSet.getNumberOfColumns(); col++) {
	            	// The first column always displays some sort of row header.  So
	            	// declare a TextView to hold the value to be displayed.
	            	if (col == 0) {
	            		TextView tv = new TextView(this);
	            		tv.setGravity(Gravity.CENTER);
	            		tv.setPadding(2, 2, 2, 2);
	            		// The upper left corner won't contain anything, while all
	            		// the other rows will have the row number.  Conveniently,
	            		// we don't need any math to get the row number value we want.
	            		if (row == 0) tv.setText("");
	            		else tv.setText(String.valueOf(row));
	            		// Add the view to the row:
	            		tr.addView(tv);
	            	// All the other columns will contain some sort of data:
	            	} else {
	            		// The first row will be the column headers.  Note that when
	            		// we hit the letter array to display the name, we need to
	            		// subtract one to fit the zero-based index of the array.
	            		if (row == 0) {
	            			TextView tv2 = new TextView(this);
	                		tv2.setGravity(Gravity.CENTER);
	                		tv2.setPadding(2, 2, 2, 2);
	                		tv2.setText(letters[col - 1]);
	                		tr.addView(tv2);
	            		// The rest of the rows will contain our actual ToggleButtons
	            		// and passcodes:
	            		} else {
	            			ToggleButton tb = new ToggleButton(this);
	            			// Set our ID to be the "seed" value, then factor in the
	            			// row and column.  We'll add the column value directly,
	            			// but multiply the row by some offset.  We'll remove the
	            			// seed and row offset later to decode the row and column
	            			// values.
	            			tb.setId(btnIdSeed + row * btnRowOffset + col);
	            			// We want the ToggleButtons to display the value of the
	            			// passcode.  By default, ToggleButtons display an "ON"
	            			// or "OFF" text value depending on the button's state.
	            			// Since we always want to display the passcode, we'll
	            			// set both of these strings to the value of the passcode.
	            			// Unfortunately, for some bizarre reason, Android doesn't
	            			// like doing this on the first run through.  No matter
	            			// what we set here, the values will remain "ON" or "OFF"
	            			// initially until something triggers a refresh.  Since
	            			// we'll be resetting these values later in rebuildCard(),
	            			// this isn't a major issue.  For now, set the text to
	            			// some place holder.
	            			tb.setTextOn(dummy);
	            			tb.setTextOff(dummy);
	            			tb.setGravity(Gravity.CENTER);
	                		tb.setPadding(2, 2, 2, 2);
	                		// I don't think the typeface is working either:
	            			tb.setTypeface(Typeface.MONOSPACE);
	            			// I'm not sure why, but setting the layout parameters
	            			// doesn't work here.  If they're set, nothing gets
	            			// displayed.  We'll leave this in for now but comment it
	            			// out until we find out why it doesn't work.
	            			//tb.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
	            			//		LayoutParams.WRAP_CONTENT));
	            			// Set the text color of the button to the primary text
	            			// display for the theme.  Note that ToggleButtons require
	            			// a ColorStateList, but we're setting that to a list that
	            			// has the same color for every state.  Thus, the text will
	            			// never change color and the primary indicator of whether
	            			// the button is checked or not will be the background.
	            			tb.setTextColor(ColorStateList.valueOf(getResources().getColor(android.R.color.secondary_text_dark)));
	            			// By default, "clear" the "strike through":
	            			tb.setChecked(false);
	            			tb.setBackgroundResource(toggleBgOff);
	            			// Set our listeners to implement our toggle behavior:
	            			tb.setOnClickListener(onClickListener);
	            			tb.setOnCheckedChangeListener(onCheckedChangeListener);
	            			// Now add the button to the table row:
	            			tr.addView(tb);
	            		}
	            	}
	            }
	            // Now that the row has been built, add it to the table:
	            tl.addView(tr, new TableLayout.LayoutParams(
	                    LayoutParams.FILL_PARENT,
	                    LayoutParams.WRAP_CONTENT));
	        }
	        // Finally, get the PPP engine to rebuild the actual passcode values
	        // and set the ToggleButton states from the database.  This gets done
	        // a lot, so we'll put that in its own method.  This has the added benefit
	        // of getting around the weird state text anomaly mentioned above.
	        rebuildCard();

	    // This should be prettier, but catch any errors and redisplay them to the
	    // user.  This needs to be cleaned up and the text internationalized before
	    // going public.
        } catch (Exception e) {
        	Toast.makeText(getBaseContext(), "ERROR: " + e.getMessage(),
					Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    protected Dialog onCreateDialog(int id)
    {
    	final Activity caller = this;
    	Dialog dialog = null;
    	// Figure out which dialog to create:
    	switch (id) {
    		// We'll need the progress dialog to do the grunt work of building the
    		// card passcodes.  Since this is just processor intensive enough to slow
    		// down a smartphone, we need to do this in a separate thread rather than
    		// the UI thread.
	    	case DIALOG_PROGRESS:
	    		progressDialog = new ProgressDialog(this);
	    		progressDialog.setOwnerActivity(this);
	    		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	    		progressDialog.setMax(totalPasscodes);
	            //progressDialog.setMessage(getResources().getString(R.string.sitelist_loading_message));
	            progressDialog.setMessage(getResources().getString(R.string.dialog_generating_passwords));
	            cardBuilderThread = new CardBuilderThread(handler);
	            cardBuilderThread.start();
	            dialog = progressDialog;
	            break;
	        // The Go To Card dialog gives us an interface to jump forward or backward
	        // to a specific card number.  Sadly, this is a every so slightly too
	        // complex to let us use AlertDialog.Builder, so we'll have to build a
	        // custom dialog box from scratch.
	    	case DIALOG_GOTO:
	    		// Create the dialog and set its layout and title:
	    		dialog = new Dialog(this);
	    		dialog.setContentView(R.layout.goto_card_dialog);
	    		dialog.setTitle(R.string.dialog_goto_card_title);
	    		// Get a reference to the edit text box and populate it with the
	    		// value of the current card.  Note that we have to convert the
	    		// card number to a string or we'll accidentally try to grab a
	    		// reference with that integer number.  Oops.
	    		EditText txtGoToCard =
	    			(EditText)dialog.findViewById(R.id.goto_dlg_card_num);
	    		txtGoToCard.setText(String.valueOf(cardSet.getLastCard()));
	    		// Define what the Go button should do:
	    		Button btnGoTo = (Button)dialog.findViewById(R.id.btn_goto_go);
	    		btnGoTo.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						// Get a reference to the text box:
						EditText txtGoTo =
							(EditText)(v.getRootView().findViewById(R.id.goto_dlg_card_num));
						// Wrap this in a try/catch because there's a chance the user
						// could input an invalid value:
						try {
							// Get its value and parse an integer from it:
							int newCard =
								Integer.parseInt(((Editable)txtGoTo.getText()).toString());
							// Make sure the card number is valid:
							if (newCard >= Cardset.FIRST_CARD && newCard <= Cardset.FINAL_CARD) {
								// Since this does a lot of the same work as the Next and
								// Previous buttons, we have to duplicate some of their
								// effort.  Set the last/current card on the card set to
								// the new value, then enable/disable the Next and Previous
								// buttons as appropriate.  We also want to set the card
								// number label so we know which card we're now on.
								cardSet.setLastCard(newCard);
								if (newCard == Cardset.FIRST_CARD)
									btnPrevious.setEnabled(false);
								else btnPrevious.setEnabled(true);
								if (newCard == Cardset.FINAL_CARD)
									btnNext.setEnabled(false);
								else btnNext.setEnabled(true);
						        lblCardNumber.setText(getResources().getString(R.string.cardview_card_num_prompt).replace(getResources().getString(R.string.meta_replace_token), String.valueOf(newCard)));
								// Save the card set to the database:
								DBHelper.saveCardset(cardSet);
						        // Remove this dialog, then set to work rebuilding the
						        // card:
								removeDialog(DIALOG_GOTO);
								rebuildCard();
							// If the number isn't valid, show an error:
							} else {
								removeDialog(DIALOG_GOTO);
								Toast.makeText(caller, R.string.error_invalid_card_num,
										Toast.LENGTH_LONG).show();
							}
						// The user entered an invalid card number:
						} catch (Exception e) {
							removeDialog(DIALOG_GOTO);
							Toast.makeText(caller, R.string.error_invalid_card_num,
									Toast.LENGTH_LONG).show();
						}
					}
	    		});
	    		// Define what the Cancel button should do:
	    		Button btnCancel = (Button)dialog.findViewById(R.id.btn_goto_cancel);
	    		btnCancel.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						// We'll force the application to completely dispose of this
						// dialog for now and rebuild it the next time.  Probably
						// not the most efficient thing to do, but it guarantees
						// we'll have a clean dialog the next time.
						removeDialog(DIALOG_GOTO);
					}
	    		});
	    		break;
	    	// The Clear Toggles dialog allows the user to clear all strike-outs for
	    	// either the currently displayed card or for the entire card set.  This
	    	// one is a bit different in that we give the user a set of radio button
	    	// choices within the dialog.
	    	case DIALOG_CLEAR_TOGGLES:
	    		// Start building the dialog:
	    		AlertDialog.Builder adb = new AlertDialog.Builder(this);
    			adb.setTitle(R.string.dialog_clear_strikes_title);
    			// Add the handler for the choices.  We want the user to select one
    			// of the choices, and we'll launch that action when they select
    			// the option.  I wonder if we should launch a confirmation dialog
    			// here instead of diving right in, but that might be argued as
    			// overkill.
    			adb.setSingleChoiceItems(R.array.clear_toggle_choices, -1,
    					new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface arg0, int item) {
						if (item == 0) {
							// Clear the strikes for the current card:
							if (DBHelper.clearTogglesForCard(cardSet.getCardsetId(),
									cardSet.getLastCard())) {
								Toast.makeText(caller,
										R.string.dialog_clear_strikes_current_success,
										Toast.LENGTH_LONG).show();
								rebuildCard();
							} else {
								Toast.makeText(caller,
										R.string.dialog_clear_strikes_current_failure,
										Toast.LENGTH_LONG).show();
							}
						// Clear the strikes for all cards:
						} else {
							if (DBHelper.clearAllTogglesForCardset(cardSet)) {
								Toast.makeText(caller,
										R.string.dialog_clear_strikes_all_success,
										Toast.LENGTH_LONG).show();
								rebuildCard();
							} else {
								Toast.makeText(caller,
										R.string.dialog_clear_strikes_all_failure,
										Toast.LENGTH_LONG).show();
							}
						}
						caller.dismissDialog(DIALOG_CLEAR_TOGGLES);
					}
    			});
    			// The user should be able to cancel out of the dialog without doing
    			// anything destructive:
    			adb.setCancelable(true);
    			adb.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
    			});
    			adb.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						caller.dismissDialog(DIALOG_CLEAR_TOGGLES);
					}
				});
    			dialog = (Dialog)adb.create();
	    		break;
    	}
    	// Return the selected dialog:
    	return dialog;
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	// Add the "Go To" menu item:
    	menu.add(0, OPTMENU_GOTO, Menu.NONE,
    		R.string.optmenu_goto).setIcon(android.R.drawable.ic_menu_directions);
    	// Add the "Clear Toggles" menu item:
    	menu.add(0, OPTMENU_CLEAR_TOGGLES, Menu.NONE,
    		R.string.optmenu_clear_strikes).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    	// Add the "Settings" menu item:
    	menu.add(0, OPTMENU_SETTINGS, Menu.NONE,
				R.string.optmenu_settings).setIcon(android.R.drawable.ic_menu_preferences);
    	// Add the "Help" menu item:
    	menu.add(0, OPTMENU_HELP, Menu.NONE,
            R.string.optmenu_help).setIcon(android.R.drawable.ic_menu_help);
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	// Which menu item did the user select?
    	switch (item.getItemId()) {
    		// The Go To item is pretty simple.  Rather than clutter up the main
    		// UI with this, we'll launch a separate dialog box to prompt the user
    		// for which card to jump to.  If they select the Go To menu item,
    		// launch that dialog:
	    	case OPTMENU_GOTO:
	    		showDialog(DIALOG_GOTO);
	    		return true;
	    	// Like the above, Clear Toggles launches the dialog to prompt the user
	    	// further:
	    	case OPTMENU_CLEAR_TOGGLES:
	    		showDialog(DIALOG_CLEAR_TOGGLES);
	    		return true;
	    	// Launch the settings activity:
	    	case OPTMENU_SETTINGS:
	    		Intent i = new Intent(this, SettingsActivity.class);
		    	startActivity(i);
	    		return true;
	    	// If the Help item is selected, open up the help page for this
	    	// Activity:
	    	case OPTMENU_HELP:
	        	//Intent i2 = new Intent(this, HelpActivity.class);
	        	//i2.putExtra("helptext", R.string.help_text_importexport);
	        	//startActivity(i2);
	    		Toast.makeText(this, R.string.error_not_implemented,
	    				Toast.LENGTH_LONG).show();
	    		return true;
    	}
    	return false;
    }

    
    /** This Handler receives status updates from the CardBuilderThread and updates
     *  the progress dialog accordingly. */
    final Handler handler = new Handler() {
    	public void handleMessage(Message msg) {
    		// Get the status code from the message:
    		int status = msg.getData().getInt("pccount");
    		// If we got any positive status, it's not an error, so update the
    		// progress dialog with the value:
    		if (status >= 0) progressDialog.setProgress(status);
    		// If we've reached the end of our passcodes, it's time to start updating
    		// the actual ToggleButtons.  We can't do this directly from the worker
    		// thread, so it has to wait until we get to here.
    		if (status >= totalPasscodes) {
    			try {
	    			// If we got any useful data, it's time to update the buttons:
	    			if (passcodes != null) {
	    				// Loop through the rows and columns:
	    				for (int row = 1; row <= cardSet.getNumberOfRows(); row++) {
	    					for (int col = 1; col <= cardSet.getNumberOfColumns(); col++) {
	    		        		// Identify the ToggleButton for this row/column.  We'll
	    						// take the row and column number values to rebuild the
	    						// button's ID.
	    		        		ToggleButton tb = (ToggleButton)findViewById(btnIdSeed +
	    		        				row * btnRowOffset + col);
	    		        		// The button text should always display the passcode value,
	    		        		// regardless of state:
	    		    			tb.setTextOn(passcodes[row - 1][col - 1]);
	    		    			tb.setTextOff(passcodes[row - 1][col - 1]);
	    		    			// If we got useful toggle data and this passcode has been
	    		    			// toggled, "strike through" this button so the passcode
	    		    			// remains struck.  Note that the toggle array indices are
	    		    			// zero-based, so we need to adjust our values to get the right
	    		    			// state.  If we didn't get any useful toggle data, default
	    		    			// all toggles to false or off.
	    		    			if (toggles != null && toggles[row - 1][col - 1])
	    		    				tb.setChecked(true);
	    		    			else tb.setChecked(false);
	    					}
	    				}
	    				// Now that they're no longer needed, clear the toggle and
	    				// passcode arrays to free up memory:
	    				toggles = null;
	    				passcodes = null;
	    			}
	    			// Now remove the progress dialog:
	    			removeDialog(DIALOG_PROGRESS);
    			} catch (Exception e) {
    				Toast.makeText(getBaseContext(), "ERROR: " + e.toString(),
                    		Toast.LENGTH_LONG).show();
    			}
    		// If we get a negative status, something went wrong.  Display an
    		// error message:
    		} else if (status < 0) {
    			Toast.makeText(getBaseContext(), R.string.error_card_build_failed,
                		Toast.LENGTH_LONG).show();
    		}
    	}
    };
    
    /**
     * This private Thread subclass does the grunt work of generating a card full of
     * passcodes.  Since this is very processor intensive, we need to do this in a
     * different thread than the UI thread.
     * @author Jeffrey T. Darlington
     * @version 1.0
     * @since 1.0
     */
    private class CardBuilderThread extends Thread {
    	/** The Handler to send messags to */
    	Handler handler = null;
    	/** The current count of passcodes generated */
        int counter = 0;
        
        CardBuilderThread(Handler handler) {
        	this.handler = handler;
        }

        @Override
        public void run() {
        	// Declare a Message and Bundle that we can reuse:
            Message msg = null;
            Bundle b = null;
            try {
            	// Get our toggle data for the current card from the database.  Note
            	// that this could return a null value if an error occurs.
            	toggles = DBHelper.getTogglesForLastCard(cardSet);
            	// Reset the passcode array to a new, empty array.  This will be how
            	// we pass the passcords back to the UI thread since we can't
            	// manipulate the ToggleButtons directly.
            	passcodes = new String[cardSet.getNumberOfRows()][cardSet.getNumberOfColumns()];
        		// Loop through our rows and columns.  Note that we go rows first, then
        		// columns:
    	    	for (int row  = 1; row <= cardSet.getNumberOfRows(); row++) {
    	        	for (int col = 1; col <= cardSet.getNumberOfColumns(); col++) {
    	        		// Generate the passcode using the PPP engine, feeding it the
    	        		// last/current card, column number, and row number.  Note that
    	        		// the engine already has all the other parameters.  Also note
    	        		// that we need to tweak the array indices since the array
    	        		// is zero-based.
    	        		passcodes[row - 1][col - 1] =
    	        			ppp.getPasscode(cardSet.getLastCard(), col, row);
    	    			// Notify the handler that we're finished with this button and
    	    			// we're ready to move to the next.  Note that we're only
    	    			// bothering sending status for the passcode generation step,
    	    			// as the toggle step below is much faster by comparison.
    	    			counter++;
    	    			msg = handler.obtainMessage();
    	                b = new Bundle();
    	                b.putInt("pccount", counter);
    	                msg.setData(b);
    	                handler.sendMessage(msg);
    	        	}
    	    	}
    	    // If anything blows up, return a negative status message to the Handler
    	    // to let it know that something didn't work correctly.
            } catch (Exception e) {
            	msg = handler.obtainMessage();
                b = new Bundle();
                b.putInt("pccount", -1);
                msg.setData(b);
                handler.sendMessage(msg);
            }
        }
        
    }
    
    /**
     * Rebuild the current card.  This gets the card set and card number data from
     * the activity's Cardset instance and the state of the toggle buttons from the
     * database, then steps through and generates each passcode and sets its toggle
     * button to the appropriate state.
     */
    private void rebuildCard() {
    	// I originally had all the card building code here, but that turned out to
    	// be too much for the poor Android emulator.  While most devices are faster
    	// than the emulator, that's a good indication that this is too much work to
    	// be doing in the UI thread and it needs to be moved to a worker thread.
    	// So this method is relatively simple:  Launch the progress dialog, which in
    	// turn will launch the worker thread and start the passcode generation
    	// process.
  		showDialog(DIALOG_PROGRESS);
    }
}
