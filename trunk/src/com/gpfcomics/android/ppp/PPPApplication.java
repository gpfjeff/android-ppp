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
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

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
	
	// ################### Private Constants #################################
	
	/** This constant is used to specify the name of our preferences file. */
	private static final String PREF_FILE = "PPPPrefs";

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
	
	/** The cryptographic key factory definition.  This will be used by most
	 *  cryptography functions throughout the application (with the exception
	 *  being the new cross-platform import/export format).  Note that this
	 *  will be a "password-based encryption" (PBE) cipher (specifically 
	 *  256-bit AES as of this writing), so take that into account when
	 *  using this value. */
	private static final String KEY_FACTORY = "PBEWITHSHA-256AND256BITAES-CBC-BC";

	/** The number of iterations used for cryptographic key generation, such
	 *  as in creating an AlgorithmParameterSpec.  Ideally, this should be
	 *  fairly high, but we'll use a modest value for performance. */
	private static final int KEY_ITERATION_COUNT = 50;

	/** The length of generated encryption keys.  This will be used for
	 *  generating encryption key specs. */
	private static final int KEY_LENGTH = 32;

	/** The cryptographic hash to use to generate encryption salts.  Pass this
	 *  into MessageDigest.getInstance() to get the MessageDigest for salt
	 *  generation. */
	private static final String SALT_HASH = "SHA-512";

	/** A random-ish string for salting our encryption salts. */
	private static final String SALT = "cSg6Vo1mV3hsENK6njMIkr8adrZ4lbGByu8fd8PClRknqhEC8DOmbDCtgUAtbir";
	
	/** The character encoding used to convert strings to binary data, primarily
	 *  in cryptographic hash operations. */
	private static final String ENCODING = "UTF-8";
	
	// ################### Private Members #################################
	
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
	
	/** An cipher for the encryption and decryption of sequence keys */
	private static Cipher cipher = null;
	
	/** The secret key used for encryption and decryption */
	private static SecretKey cipherKey = null;
	
	/** The cipher algorithm parameter spec */
	private static AlgorithmParameterSpec cipherAPS = null;
	
	// ################### Public Methods #################################
	
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
			prefs = getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
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
			// If there's currently a password set, we need to set up our ciphers
			// for encryption and decryption:
			if (promptForPassword()) createCiphers();
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
	
	// ################### Public Static Methods #################################
	
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
    
	// ################### Protected Methods #################################
	
	/**
	 * Set the user's password and store its encrypted value to the preferences file
	 * @param password The new plain-text password
	 * @return True on success, false on failure
	 */
	boolean setPassword(String password) {
		try {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(PREF_PASSWORD, encryptPassword(password));
			editor.commit();
			createCiphers();
			return true;
		} catch (Exception e) { return false; }
	}
	
	/**
	 * Clear the user's stored password.  Note that this should only be called if
	 * the user's database has been cleared first.
	 * @return True on success, false on failure
	 */
	boolean clearPassword() {
		try {
			SharedPreferences.Editor editor = prefs.edit();
			editor.remove(PREF_PASSWORD);
			editor.commit();
			createCiphers();
			return true;
		} catch (Exception e) { return false; }
	}
	
	/**
	 * Encrypt the specified sequence key string and return the encrypted result.
	 * If an application password has not be specified or the encryption fails
	 * for whatever reason, the original string will be returned.
	 * @param original The sequence key string to encrypt
	 * @return The encrypted sequence key string
	 */
	String encryptSequenceKey(String original) {
		if (original == null || cipherKey == null)
			return original;
		try {
			cipher = Cipher.getInstance(KEY_FACTORY);
			cipher.init(Cipher.ENCRYPT_MODE, cipherKey, cipherAPS);
			return bytesToHexString(cipher.doFinal(original.getBytes(ENCODING)));
		} catch (Exception e) {
			return original;
		}
	}

	/**
	 * Decrypt the specified sequence key string and return the plain text result.
	 * If an application password has not be specified or the decryption fails
	 * for whatever reason, the original string will be returned.
	 * @param original The sequence key string to decrypt
	 * @return The decrypted sequence key string
	 */
	String decryptSequenceKey(String original) {
		if (original == null || cipherKey == null)
			return original;
		try {
			cipher = Cipher.getInstance(KEY_FACTORY);
			cipher.init(Cipher.DECRYPT_MODE, cipherKey, cipherAPS);
			return bytesToHexString(cipher.doFinal(original.getBytes(ENCODING)));
		} catch (Exception e) {
			return original;
		}
	}

	// ################### Private Methods #################################
	
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
	    	MessageDigest hasher = MessageDigest.getInstance(SALT_HASH);
			byte[] digest = hasher.digest(password.getBytes(ENCODING));
			for (int i = 0; i < 9; i++) digest = hasher.digest(digest);
			return bytesToHexString(digest);
		// If anything blows up, return null as an error code:
    	} catch (Exception e) {
    		return null;
    	}
    }
    
    /**
     * Create the encryption and decryption ciphers needed to securely store and
     * retrieve encrypted sequence keys in the database.  Note that these ciphers
     * will only be created if the user's password is set; otherwise, the ciphers
     * will default to null.
     */
    private void createCiphers() {
		// Asbestos underpants:
		try
		{
			// Get the stored password.  Note that this isn't the actual password
			// the user set, but the "encrypted" password stored in the preferences
			// generated by encryptPassword().  If no password is set, we should
			// get a null here.
			String password = prefs.getString(PREF_PASSWORD, null);
			// Only proceed if the password was set:
			if (password != null) {
				// First we need to start of by creating our salt.  We're not going
				// to hold on to this after creating the ciphers, so we'll just
				// declare it locally here.
				byte[] salt = null;
		        // To try and make this unique per device, we'll use the device's
				// unique ID string.  To avoid the whole deprecation issue surrounding
		        // Settings.System.ANDROID_ID vs. Settings.Secure.ANDROID_ID, we'll
		        // wrap the call to this property inside the AndroidID class.  See
		        // that class for more details.
		        String uniqueID = null;
		        try {
		        	AndroidID id = AndroidID.newInstance(this);
		        	uniqueID = id.getAndroidID();
		        } catch (Exception e1) { }
		        // Check the unique ID we just fetched.  If we didn't get anything,
		        // we'll just make up a hard-coded random-ish string and use that as
		        // our starting point.  Of course, if we're using this, our salt will
		        // *NOT* be unique per device, but that's the best we can do.
		    	if (uniqueID == null) uniqueID = SALT;
		    	// If we *did* get a unique ID above, go ahead and concatenate our
		    	// salt string on to the end of it as well.  That should give us
		    	// a salt for our salt.
		    	else uniqueID = uniqueID.concat(SALT);
		        // Now get the unique ID string as raw bytes.  We'll use UTF-8 since
		    	// everything we get should work with that encoding.
	    		salt = uniqueID.getBytes(ENCODING);
		        // Ideally, we don't want to use the raw ID by itself; that's too
		        // easy to guess.  Rather, let's hash this a few times to give us
		        // something less predictable.
				MessageDigest hasher = MessageDigest.getInstance(SALT_HASH);
				for (int i = 0; i < KEY_ITERATION_COUNT; i++)
					salt = hasher.digest(salt);
				// Now, for good measure, let's obscure our password so we won't be
				// using the value stored in the preferences directly.  We'll
				// concatenate the unique ID generated above into the "encrypted"
				// password, convert that to bytes, and hash it multiple times as
				// well.  Since the encryption routines below need a character array
				// for the password, we'll then conver that back to a string of
				// hex digits.
				byte[] passbytes = password.concat(uniqueID).getBytes(ENCODING);
				for (int i = 0; i < KEY_ITERATION_COUNT; i++)
					passbytes = hasher.digest(passbytes);
				password = bytesToHexString(passbytes);
				// I had a devil of a time getting this to work, but I eventually
				// peeked at the Google "Secrets" application source code to get
				// to this setup.  The Password Based Key (PBE) spec lets us
				// specify a password to generate keys from.  We'll use the obscured
				// password and salt generated above.
				PBEKeySpec pbeKeySpec =	new PBEKeySpec(password.toCharArray(),
						salt, KEY_ITERATION_COUNT, KEY_LENGTH);
				// Next we'll need a key factory to actually build the key:
				SecretKeyFactory keyFac =
					SecretKeyFactory.getInstance(KEY_FACTORY);
				// The key is generated from the key factory:
				cipherKey = keyFac.generateSecret(pbeKeySpec);
				// The cipher needs some parameter specs to know how to use
				// the key:
				cipherAPS = new PBEParameterSpec(salt, KEY_ITERATION_COUNT);
				// Now that we have all of this information, it's time to actually
				// create the cipher:
				cipher = Cipher.getInstance(KEY_FACTORY);
			} else {
				// If the password wasn't found, null out the cipher so they
				// won't do anything useful.
				cipher = null;
				cipherKey = null;
				cipherAPS = null;
			}
		}
		// If anything blew up, null out the cipher as well:
		catch (Exception e)
		{
			cipher = null;
			cipherKey = null;
			cipherAPS = null;
		}
    }

}
