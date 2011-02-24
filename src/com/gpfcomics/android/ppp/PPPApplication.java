/* PPPApplication.java
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

import java.security.MessageDigest;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * This class encapsulates functionality common to all activities within the Perfect
 * Paper Passwords application.  It controls the user's preferences as well as a
 * single, common instance of the database.
 * @author Jeffrey T. Darlington
 * @version 1.0
 * @since 1.0
 */
public class PPPApplication extends Application {
	
	/** This constant is used in the preferences file to identify the version of
	    Perfect Paper Passwords that is last wrote to the file. */
	private static final String PREF_VERSION = "version";
	
	/** This constant is used in the preferences file to identify the user's
	    password, if set.  If this option is not found in the preferences, there
		is no current password. */
	private static final String PREF_PASSWORD = "password";
	
	/** This constant is used in the preferences file to identify the user's
	    preference with regard to whether or not passcodes should be copied to
		the clipboard when they are "struck through". */
	private static final String PREF_COPY_PASSCODES_TO_CLIPBOARD = "pass_to_clip";
	
	/** A reference to the application's database helper.  Activities will get
	    copies of this reference, but the application will own the master copy. */
	private static CardDBAdapter DBHelper = null;
	
	/** A referenceto the application's shared preferences.  Activities will get
	    copies of this reference, but the application will own the master copy. */
	private static SharedPreferences prefs = null;
	
	/** Whether or not to copy passcodes to the clipboard when they are "struck
	    through" in the card view. */
	private static boolean copyPasscodes = true;
	
	/** A convenience reference to our numeric version code */
	private static int versionCode = -1;
	
	/** A convenience reference to our version "number" string */
	private static String versionName = null;
	
	@Override
	public void onCreate()
	{
		// Do whatever the super needs to do:
		super.onCreate();
		// Open the database using the DB adaptor so we can access the
		// database:
		DBHelper = new CardDBAdapter(this);
		DBHelper.open();
		try {
			// Get a copy of our preferences, which is primarily where we'll store
			// the user's password and settings:
			prefs = getSharedPreferences("PPPPrefs", Context.MODE_PRIVATE);
			// Get our current version number from the package manager.  We'll use
			// the integer version to simplify comparisons.
			PackageInfo info =
	        	getPackageManager().getPackageInfo(getPackageName(),
        			PackageManager.GET_META_DATA);
			versionCode = info.versionCode;
			versionName = info.versionName;
	        // Now get the version number, if any, from the preferences.  Since we
	        // should never have a negative version number, we'll use that as a flag
	        // to indicate that the preferences file does not exist.
	        int oldVersion = prefs.getInt(PREF_VERSION, -1);
	        // Now run a few tests on our version numbers.  If we got a negative value
	        // above, no preferences have been written.  Write the defaults to the
	        // new preferences file and save them.  Note that there is no password
	        // setting here, as there should be no password until the user sets one.
			if (oldVersion == -1) {
				SharedPreferences.Editor editor = prefs.edit();
				editor.putInt(PREF_VERSION, versionCode);
				editor.putBoolean(PREF_COPY_PASSCODES_TO_CLIPBOARD, copyPasscodes);
				editor.commit();
			// Is the old version number newer than the current one?  It could be we're
			// running an older copy of the app with a newer set of preferences:
			} else if (oldVersion > versionCode) {
				// This is technically an error condition.  The version number in the
				// preferences file should not be newer that the current version of
				// the app.  However, this shouldn't be a problem for now since this
				// is our first version.  We should raise some sort of error here
				// if this ever occurs.
			// Is our version number newer than the previous one?  Time to upgrade:
			} else if (versionCode > oldVersion) {
				// There shouldn't be anything to do here yet...
			// If the version number matches, restore the user's preferences:
			} else {
				copyPasscodes = prefs.getBoolean(PREF_COPY_PASSCODES_TO_CLIPBOARD,
						true);
			}
		} catch (Exception e) {
			// I'm not sure what to do here if something blows up. :\
		}
	}
	
	/**
	 * Get the common database helper
	 * @return The common database helper
	 */
	public CardDBAdapter getDBHelper() { return DBHelper; }
	
	/**
	 * Should we prompt the user for their password?
	 * @return True if the user has set a password and they should be prompted for
	 * it, false if no password has been set.
	 */
	public boolean promptForPassword() {
		String password = prefs.getString(PREF_PASSWORD, null);
		return password != null;
	}
	
	/**
	 * Set the user's password and store its encrypted value to the preferences file
	 * @param password The new plain-text password
	 * @return True on success, false on failure
	 */
	public boolean setPassword(String password) {
		try {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(PREF_PASSWORD, encryptPassword(password));
			editor.commit();
			return true;
		} catch (Exception e) { return false; }
	}
	
	/**
	 * Clear the user's stored password.  Note that this should only be called if
	 * the user's database has been cleared first.
	 * @return True on success, false on failure
	 */
	public boolean clearPassword() {
		try {
			SharedPreferences.Editor editor = prefs.edit();
			editor.remove(PREF_PASSWORD);
			editor.commit();
			return true;
		} catch (Exception e) { return false; }
	}
	
	/**
	 * Validate the supplied password to make sure it matches the password stored
	 * in the preferences file
	 * @param password The plain-text password to validate
	 * @return True if the password matches, false otherwise
	 */
	public boolean isValidPassword(String password) {
		try {
			String stored_password = prefs.getString(PREF_PASSWORD, null);
			if (stored_password == null) return false;
			String enc_password = encryptPassword(password);
			return enc_password.compareTo(stored_password) == 0;
		} catch (Exception e) { return false; }
	}
	
	/**
	 * Should we copy a passcode to the system clipboard when the user "strikes
	 * through" it in the card view activity?
	 * @return True or false
	 */
	public boolean copyPasscodesToClipboard() { return copyPasscodes; }
	
	/**
	 * Toggle the "copy passcode to the clipboard" setting and store the new value
	 * to the system preferences
	 * @return True on success, false on failure
	 */
	public boolean toggleCopyPasscodesSetting() {
		try {
			copyPasscodes = !copyPasscodes;
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(PREF_COPY_PASSCODES_TO_CLIPBOARD, copyPasscodes);
			editor.commit();
			return true;
		} catch (Exception e) { return false; }
	}

    /**
     * Convert an array of bytes into the equivalent hexadecimal string
     * @param bytes An array of bytes
     * @return The equivalent string in upper-case hexadecimal digits.  If the
     * input array happens to be null, this will also return a null string.
     */
    public static String bytesToHexString(byte[] bytes) {
    	if (bytes == null) return null;
    	StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%1$02X", b));
        return sb.toString().toUpperCase();
    }
    
    /**
     * Get the numeric application version code number
     * @return
     */
    public static int getVersionCode() { return versionCode; }
    
    /**
     * Get the user-friendly application version "number" string for display
     * @return
     */
    public static String getVersionName() { return versionName; }
    
    /**
     * Encrypt the supplied password using a common one-way algorithm
     * @param password The plain-text password to encrypt
     * @return The encrypted password, or null on failure
     */
    private static String encryptPassword(String password) {
    	try {
    		// OK, this is technically not encryption.  That said, it's a very
    		// effective way of storing our password securely without resorting
    		// to something external.  We'll hash the password multiple times using
    		// a strong cryptographic hash, then return the hex-encoded result.
    		// Note that we'll hard-code the text encoding as UTF-8 rather than
    		// rely on whatever the system default might be, to make sure we are
    		// consistent.
	    	MessageDigest hasher = MessageDigest.getInstance("SHA-512");
			byte[] digest = hasher.digest(password.getBytes("UTF-8"));
			for (int i = 0; i < 9; i++) digest = hasher.digest(digest);
			return bytesToHexString(digest);
		// If anything blows up, return null as an error code:
    	} catch (Exception e) {
    		return null;
    	}
    }

}
