/* CardViewState.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          March 7, 2011
 * PROJECT:       Perfect Paper Passwords for Android
 * ANDROID V.:	  1.1
 * 
 * This class is used to pass state information from one instance of the Card View
 * Activity to another during a configuration change (such as rotating the screen).
 * This preserves the current card set, the generated passcodes for the current card,
 * and toggle/strike-out information for the current card.
 * 
 * This class was added primarily because I had problems with the progress dialog on
 * the Card View Activity, especially during screen rotations and especially with
 * extra-wide card sets that could only be displayed in landscape orientation.  When
 * the orientation changed, especially when we forced it into landscape mode, the
 * progress dialog would come up and just sit there in what seemed like its initial
 * state.  It could be easily cleared by tapping Back, but that was not obvious nor
 * intuitive.  The solution was to keep the current state of the card view--namely
 * the defining Cardset object, the passcodes displayed, and the strike-outs--and
 * hold onto that while the system destroyed the old activity and recreated it.  By
 * restoring this data, we shortcut the process of building the card, rather than
 * force a rebuild on every pass.
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

/**
 * This class is used to pass state information from one instance of the Card View
 * Activity to another during a configuration change (such as rotating the screen).
 * This preserves the current card set, the generated passcodes for the current card,
 * and toggle/strike-out information for the current card.
 * @author Jeffrey T. Darlington
 * @version 1.0
 * @since 1.0
 */
public class CardViewState {

	/** A Cardset object representing the card set we are currently using. */
	private Cardset cardSet = null;

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

	/**
	 * Constructor
	 * @param cardSet The Cardset object to store
	 * @param toggles The toggle or strike-out array
	 * @param passcodes The passcode array
	 */
	public CardViewState(Cardset cardSet, boolean[][] toggles, String[][] passcodes) {
		this.cardSet = cardSet;
		this.toggles = toggles;
		this.passcodes = passcodes;
	}
	
	/**
	 * Get the stored Cardset object
	 * @return The Cardset
	 */
	public Cardset getCardset() { return cardSet; }
	
	/**
	 * Get the stored toggle or strike-out array
	 * @return A two-dimensional array of boolean values representing the state of
	 * all the toggle buttons on the current card
	 */
	public boolean[][] getToggles() { return toggles; }
	
	/**
	 * Get the stored passcode array
	 * @return A two-dimensional array of Strings containing the passcodes for the
	 * current card
	 */
	public String[][] getPasscodes() { return passcodes; }
	
}
