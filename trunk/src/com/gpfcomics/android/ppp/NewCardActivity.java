/* NewCardActivity.java
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
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class NewCardActivity extends Activity {
	
	/** The passcode spinner contains a list of valid numbers for the length of the
	 *  passcode.  By the definitions at GRC.com, this should be 2-16 characters.
	 *  The exact values are specified within the res/values/strings.xml file, but
	 *  these will just be strings of numbers.  Fortunately, we can do a bit of simple
	 *  math to get the actual value of the spinner from the position index.  Since
	 *  the first item will be 2, which is in position 0, we can add 2 to the position
	 *  to get the actual value.  Conversely, you can get the position from the
	 *  passcode length by subtracting 2.  This constant lets us define this "offset"
	 *  in a single place should it ever need to change. */
	private static final int PASSCODE_SPINNER_OFFSET = 2;
	
	/** The card set name text box */
	private EditText txtName = null;
	
	/** The number of columns text box */
	private EditText txtNumColumns = null;
	
	/** The number of rows text box */
	private EditText txtNumRows = null;
	
	/** The passcode length Spinner */
	private Spinner spinPasscodeLength = null;
	
	/** The alphabet text box */
	private EditText txtAlphabet = null;
	
	/** The sequence key text box */
	private EditText txtSequenceKey = null;
	
	/** The Add New Card Set button */
	private Button btnAdd = null;
	
	/** A reference back to our parent application */
	private PPPApplication theApp = null;
	
	/** A reference to our card database helper */
	private CardDBAdapter DBHelper = null;
	
	/** A Cardset object, which will store the current state of the card set */
	private Cardset cardSet = null;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	// Asbestos underpants
    	try {
    		// Do the normal start-up stuff, like defining layout:
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.cardset_edit_layout);
	        
	        // Get references to our application and database helper:
	        theApp = (PPPApplication)this.getApplication();
	        DBHelper = theApp.getDBHelper();
	        
	        // Declare a new card set object.  This initializes all our paramters
	        // to the defaults and generates a new, random sequence key.
	        cardSet = new Cardset();
	        
	        // Get our references to our input UI elements:
	        txtName = (EditText)findViewById(R.id.cardset_edit_name_edit);
	        txtNumColumns = (EditText)findViewById(R.id.cardset_edit_columns_edit);
	        txtNumRows = (EditText)findViewById(R.id.cardset_edit_rows_edit);
	        spinPasscodeLength = (Spinner)findViewById(R.id.cardset_edit_pclength_spin);
	        txtAlphabet = (EditText)findViewById(R.id.cardset_edit_alpha_edit);
	        txtSequenceKey = (EditText)findViewById(R.id.cardset_edit_seqkey_edit);
	        btnAdd = (Button)findViewById(R.id.cardset_edit_add_btn);
	
	        // Populate the input boxes with the data from the card set object:
	        txtName.setText(cardSet.getName());
	        txtNumColumns.setText(String.valueOf(cardSet.getNumberOfColumns()));
	        txtNumRows.setText(String.valueOf(cardSet.getNumberOfRows()));
	        txtAlphabet.setText(cardSet.getAlphabet());
	        txtSequenceKey.setText(cardSet.getSequenceKey());
	        spinPasscodeLength.setSelection(cardSet.getPasscodeLength() -
	        		PASSCODE_SPINNER_OFFSET);
	        
	        // Declare the name text box focus change listener.  Whenever this box
	        // loses focus, it will check its value to see that the name is a valid
	        // string.  If it isn't, it will warn the user and force them to change
	        // the value.
	        txtName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				public void onFocusChange(View v, boolean hasFocus) {
					if (!hasFocus) {
						String name = ((Editable)txtName.getText()).toString();
						if (!Cardset.isValidName(name)) {
							Toast.makeText(v.getContext(), R.string.error_name_field_empty,
									Toast.LENGTH_LONG).show();
							//v.requestFocus();
						} else {
							// The trim() here eliminates any extraneous white space
							// at the beginning and end of the name.  No point saving
							// useless data, right?
							cardSet.setName(name.trim());
							txtName.setText(cardSet.getName());
						}
					}
				}
	        });
	        
	        // Like the above, check the number of columns box when it loses focus.
	        // Here, we want to make sure the value is a positive integer, and that
	        // the product of it and the passcode length must be less than the
	        // maximum we'll allow.  Otherwise, we won't be able to display it.
	        txtNumColumns.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				public void onFocusChange(View v, boolean hasFocus) {
					if (!hasFocus) {
						// First, is this a possible valid number of columns?
						String numColumns = ((Editable)txtNumColumns.getText()).toString();
						if (Cardset.isValidNumberOfColumns(numColumns)) {
							// Now look to see if this and the passcode length will
							// still fit in a card we can display:
							int numCols = Integer.parseInt(numColumns);
							if (Cardset.fitsMaxCardWidth(numCols, 
									cardSet.getPasscodeLength())) {
								cardSet.setNumberOfColumns(numCols);
							// We can't display a card this big:
							} else {
								Toast.makeText(v.getContext(), R.string.error_invalid_card_width,
										Toast.LENGTH_LONG).show();
								//v.requestFocus();
							}
						// The number is not valid:
						} else {
							Toast.makeText(v.getContext(), R.string.error_invalid_num_columns,
									Toast.LENGTH_LONG).show();
							//v.requestFocus();
						}
					}
				}
	        });
	        
	        // Like number of columns, check the number of rows if that box loses
	        // focus:
	        txtNumRows.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				public void onFocusChange(View v, boolean hasFocus) {
					if (!hasFocus) {
						String numRows = ((Editable)txtNumRows.getText()).toString();
						if (Cardset.isValidNumberOfRows(numRows)) {
							int numRowsI = Integer.parseInt(numRows);
							cardSet.setNumberOfRows(numRowsI);
						} else {
							Toast.makeText(v.getContext(), R.string.error_invalid_num_rows,
									Toast.LENGTH_LONG).show();
							//v.requestFocus();
						}
					}
				}
	        });
	        
	        // Alphabet is like name; make sure it's non-empty and not all white
	        // space:
	        txtAlphabet.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				public void onFocusChange(View v, boolean hasFocus) {
					if (!hasFocus) {
						String alpha = ((Editable)txtAlphabet.getText()).toString();
						if (!Cardset.isValidAlphabet(alpha)) {
							Toast.makeText(v.getContext(), R.string.error_invalid_alphabet,
									Toast.LENGTH_LONG).show();
							//v.requestFocus();
						} else {
							cardSet.setAlphabet(alpha.trim());
							txtAlphabet.setText(cardSet.getAlphabet());
						}
					}
				}
	        });
	        
	        // The sequence key is a bit more complex.  We have strict rules here;
	        // it must be a hexadecimal string 64 characters long.  The Cardset
	        // class defines a handy regular expression for testing this.  While
	        // the Cardset regex permits lower case letters, we'll force the letters
	        // to be uppper case for consistency.
	        txtSequenceKey.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				public void onFocusChange(View v, boolean hasFocus) {
					if (!hasFocus) {
						String seqKey = ((Editable)txtSequenceKey.getText()).toString();
						if (!Cardset.isValidSequenceKey(seqKey)) {
							Toast.makeText(v.getContext(), R.string.error_invalid_seqkey,
									Toast.LENGTH_LONG).show();
							//v.requestFocus();
						} else {
							cardSet.setSequenceKey(seqKey);
							txtSequenceKey.setText(seqKey.toUpperCase());
						}
					}
				}
	        });
	        
	        // When the passcode length spinner changes, the only thing we really need
	        // to validate is the maximum width of the card:
	        spinPasscodeLength.setOnItemSelectedListener(new OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View v,
						int position, long id) {
					// Decode the passcode length from the item position:
					int passcodeLength = position + PASSCODE_SPINNER_OFFSET;
					// Now check to see if it still works when combined with the
					// number of columns.  If it does, set the new passcode length:
					if (Cardset.fitsMaxCardWidth(cardSet.getNumberOfColumns(),
							passcodeLength))
						cardSet.setPasscodeLength(passcodeLength);
					// Otherwise, we need to complain.  We'll also set the reset
					// the spinner back to the original value; otherwise the number
					// of columns validation won't work properly.
					else {
						Toast.makeText(parent.getContext(),
								R.string.error_invalid_card_width,
								Toast.LENGTH_LONG).show();
						spinPasscodeLength.setSelection(cardSet.getPasscodeLength() -
								PASSCODE_SPINNER_OFFSET);
					}
				}
				// I'm not sure if this is needed:
				public void onNothingSelected(AdapterView<?> arg0) {
					//spinPasscodeLength.setSelection(Cardset.DEFAULT_PASSCODE_LENGTH -
					//		PASSCODE_SPINNER_OFFSET);
				}
	        	
	        });
	        
	        // Now define the Add button's click listener.  This will, in affect,
	        // duplicate much of the effort of the focus listeners above, but that's
	        // necessary since we can't guarantee they'll fire when this button is
	        // pressed.  We'll double-check our inputs and if they look good, we'll
	        // update the card set and save it to the database.
	        btnAdd.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					// Get string representations of each box's value: 
					String name = ((Editable)txtName.getText()).toString();
					String numColumns = ((Editable)txtNumColumns.getText()).toString();
					String numRows = ((Editable)txtNumRows.getText()).toString();
					String alpha = ((Editable)txtAlphabet.getText()).toString();
					String seqKey = ((Editable)txtSequenceKey.getText()).toString();
					// Is the name valid?
					if (!Cardset.isValidName(name)) {
						Toast.makeText(v.getContext(), R.string.error_name_field_empty,
								Toast.LENGTH_LONG).show();
						//txtName.requestFocus();
					// Is the number of columns valid?
					} else if (!Cardset.isValidNumberOfColumns(numColumns)) {
						Toast.makeText(v.getContext(), R.string.error_invalid_num_columns,
								Toast.LENGTH_LONG).show();
						//txtNumColumns.requestFocus();
					// What about the number of rows?
					} else if (!Cardset.isValidNumberOfRows(numRows)) {
						Toast.makeText(v.getContext(), R.string.error_invalid_num_rows,
								Toast.LENGTH_LONG).show();
						//txtNumRows.requestFocus();
					// The alphabet needs to be valid too:
					} else if (!Cardset.isValidAlphabet(alpha)) {
						Toast.makeText(v.getContext(), R.string.error_invalid_alphabet,
								Toast.LENGTH_LONG).show();
						txtAlphabet.requestFocus();
					// The sequence key is vitally important:
					} else if (!Cardset.isValidSequenceKey(seqKey)) {
						Toast.makeText(v.getContext(), R.string.error_invalid_seqkey,
								Toast.LENGTH_LONG).show();
						//txtSequenceKey.requestFocus();
					// We don't need to validate the passcode length spinner since it
					// can only be values that we specify.  If all the inputs look good
					// so far:
					} else {
						// These should now be safe parses:
						int numCols = Integer.parseInt(numColumns);
						int numRowsI = Integer.parseInt(numRows);
						// And we're going to need the passcode length for the
						// next test:
						int passcodeLength = spinPasscodeLength.getSelectedItemPosition()
							+ PASSCODE_SPINNER_OFFSET;
						// There's one final validation step to take.  Make sure the
						// combined number of columns and passcode length will actually
						// fit in the card view display:
						if (!Cardset.fitsMaxCardWidth(numCols, passcodeLength)) {
							Toast.makeText(v.getContext(), R.string.error_invalid_card_width,
									Toast.LENGTH_LONG).show();
						// If we passed that step, it's time to create our card set:
						} else {
							// Populate the card set object with the data from the UI.
							// Note that we trim the name and alphabet to remove white
							// space; we don't need to do this with the sequence key since
							// the regex won't permit white space.
							cardSet.setName(name.trim());
							cardSet.setNumberOfColumns(numCols);
							cardSet.setNumberOfRows(numRowsI);
							cardSet.setAlphabet(alpha.trim());
							cardSet.setSequenceKey(seqKey);
							cardSet.setPasscodeLength(passcodeLength);
							// Now that the card set object holds the data, try to save
							// it to the database.  This will return the card set's internal
							// DB ID if successful, or the Cardset.NOID constant if it
							// fails.  Give the user feedback on our success.
							cardSet.setCardsetId(DBHelper.saveCardset(cardSet));
							if (cardSet.getCardsetId() != Cardset.NOID) {
								Toast.makeText(v.getContext(), 
										getResources().getString(R.string.new_card_success).replace(getResources().getString(R.string.meta_replace_token), cardSet.getName()),
										Toast.LENGTH_LONG).show();
								setResult(MainMenuActivity.RESPONSE_SUCCESS);
							} else {
								Toast.makeText(v.getContext(),
										getResources().getString(R.string.new_card_failure).replace(getResources().getString(R.string.meta_replace_token), cardSet.getName()),
										Toast.LENGTH_LONG).show();
								setResult(MainMenuActivity.RESPONSE_ERROR);
							}
							// At this point we've done all we can.  Close this activity
							// and return to the main menu:
							finish();
						}
					}
				}
	    	});
	    // If anything blew up, notify the user:
    	} catch (Exception e) {
    		Toast.makeText(this, R.string.error_new_card_launch, Toast.LENGTH_LONG).show();
			setResult(MainMenuActivity.RESPONSE_ERROR);
			finish();
    	}
    }
    
}
