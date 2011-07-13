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
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.engines.RijndaelEngine;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.ParametersWithIV;

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
	
	/** The number of iterations used for cryptographic key generation, such
	 *  as in creating an AlgorithmParameterSpec.  Ideally, this should be
	 *  fairly high, but we'll use a modest value for performance. */
	private static final int KEY_ITERATION_COUNT = 50;

	/** The cryptographic hash to use to generate encryption salts.  Pass this
	 *  into MessageDigest.getInstance() to get the MessageDigest for salt
	 *  generation. */
	private static final String SALT_HASH = "SHA-512";

	/** A random-ish string for salting our encryption salts. */
	private static final String SALT = "cSg6Vo1mV3hsENK6njMIkr8adrZ4lbGByu8fd8PClRknqhEC8DOmbDCtgUAtbir";
	
	/** The character encoding used to convert strings to binary data, primarily
	 *  in cryptographic hash operations. */
	private static final String ENCODING = "UTF-8";
	
	/** The size of the AES encryption key in bits */
	private static final int KEY_SIZE = 256;

	/** The size of the AES encryption initialization vector (IV) in bits */
	private static final int IV_SIZE = 128;
	
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
	private static BufferedBlockCipher cipher = null;
	
	/** The initialization vector (IV) used by our cipher */
	private static ParametersWithIV iv = null;
	
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
			// If there's currently a password set, we need to set up our cipher
			// for encryption and decryption:
			if (promptForPassword()) createCipher();
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
    	// A StringBuilder should be a lot more efficient that creating a
    	// bunch of throw-away String objects:
    	StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%1$02X", b));
        return sb.toString().toUpperCase();
    }
    
    /**
     * Convert a string of hexadecimal characters to the equivalent byte array
     * @param hex A string of hexadecimal characters.  The alphabetic characters
     * may be in upper or lower case, and the length of the string must be
     * divisible by two.
     * @return A byte array containing the decoded data.
     * @throws IllegalArgumentException Thrown if the input string is not a valid
     * hexadecimal string
     */
    public static byte[] hexStringToBytes(String hex) {
    	// This is lifted mostly from the equivalent code out of
    	// com.gpfcomics.android.ppp.jppp.PPPengine, although abstracted a bit
    	// to allow for an arbitrary length hex string.  It could probably use
    	// some efficiency tweaks as the substring creates a lot of throw-away
    	// String objects, but it still ought to work.
    	//
    	// If the input string is null or its length is not a multiple of two,
    	// it isn't valid:
    	if (hex == null || hex.length() == 0 || hex.length() % 2 != 0)
    		throw new IllegalArgumentException("Invalid hexadecimal string");
    	// If the input string does not consist entirely of hex digits,
    	// it isn't valid:
    	if (!Pattern.matches("^[0-9a-fA-F]+$", hex))
    		throw new IllegalArgumentException("Invalid hexadecimal string");
    	// Now that that's out of the way, this should be pretty straight-forward.
    	// The output size will be the length of the hex string divided by two,
    	// since two hex digits are equivalent to a byte.  We'll put this size
    	// into a variable so we won't have to recompute it.  Next, allocate the
    	// output array at that size and create a temporary String to hold each
    	// two-digit substring.
    	int outputSize = hex.length() / 2;
    	byte[] out = new byte[outputSize];
    	String temp = null;
    	// Step through the string, breaking it into two-digit chunks:
    	for (int i = 0; i < outputSize; ++i) {
		    temp = hex.substring(i * 2, i * 2 + 2);
		    // Try to convert the string to binary by using the convenient
		    // Integer.parseInt() method.  Since we're only dealing with
		    // byte-sized chunks, we should then be able to cast that to a
		    // byte with no problem.
		    try {
				int b = Integer.parseInt(temp, 16);
				out[i] = (byte)b;
			// This should only happen if we failed the tests above, but if
			// the parse fails, complain:
		    } catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid hexadecimal string");
		    }
    	}
    	// Return the output array:
	    return out;
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
			// Store the "encrypted" password to the preferences:
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(PREF_PASSWORD, encryptPassword(password));
			editor.commit();
			// Create the cipher and IV objects to enable crypto:
			createCipher();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Clear the user's stored password.  Note that this should only be called if
	 * the user's database has been cleared first.
	 * @return True on success, false on failure
	 */
	boolean clearPassword() {
		try {
			// Remove the password from the preferences:
			SharedPreferences.Editor editor = prefs.edit();
			editor.remove(PREF_PASSWORD);
			editor.commit();
			// Note that this will null out the cipher and IV, disabling all
			// cryptographic functions on sequence keys:
			createCipher();
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
		try {
			// Since cryptSeqKey() requires a byte array, convert the input string
			// to bytes using the default encoding, then enable encryption mode:
			byte[] output = cryptSeqKey(original.getBytes(ENCODING),
					Cipher.ENCRYPT_MODE);
			// If cryptSeqKey() blows up, we'll get a null.  In that case, return
			// the original string as described above.  Otherwise, convert the
			// byte array result from above to a hex string and return it.
			if (output == null) return original;
			else return bytesToHexString(output);
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
		try {
			// The input string is a hex string that we encrypted earlier using
			// encryptSequenceKey().  In order to get anything out of that,
			// we'll need to convert that hex string back to bytes using
			// hexStringToBytes().  We'll pass that to cryptSeqKey() to do the
			// dirty work and tell it to decrypt the data.
			byte[] output = cryptSeqKey(hexStringToBytes(original),
					Cipher.DECRYPT_MODE);
			// If cryptSeqKey() blows up, we'll get a null.  In that case, return
			// the original string as described above.  Otherwise, we'll need to
			// convert the byte data from above into the original sequence key
			// string.  This is a bit tricky because we'll likely have some extra
			// null data at the end of the string.  First we'll convert the bytes
			// to a string using the default encoding, then we'll trim that string
			// to remove any extraneous nulls that may be found.
			if (output == null) return original;
			else {
				String outString = new String(output, ENCODING);
				return outString.trim();
			}
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
     * Create the encryption cipher needed to securely store and retrieve encrypted
     * sequence keys in the database.  Note that this cipher will only be created if
     * the user's password is set; otherwise, the cipher will default to null.
     */
    private void createCipher() {
		// Asbestos underpants:
		try
		{
			// The first thing we need to do is check to see if we have a password
			// set.  There's no point doing anything if there's no password.
			String password = prefs.getString(PREF_PASSWORD, null);
			if (password != null) {
				// OK, we've got a password.  Let's start by generating our salt.
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
		        // Check the unique ID we just fetched.  It's possible that we didn't
		        // get anything useful; it's up to manufacturers to set the Android ID
		        // property, and not everybody does it.  If we didn't get anything,
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
		    	byte[] salt = uniqueID.getBytes(ENCODING);
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
				// well.
				byte[] pwd = password.concat(uniqueID).getBytes(ENCODING);
				for (int i = 0; i < KEY_ITERATION_COUNT; i++)
					pwd = hasher.digest(pwd);
				// From the BC JavaDoc: "Generator for PBE derived keys and IVs as
				// defined by PKCS 5 V2.0 Scheme 2. This generator uses a SHA-1
				// HMac as the calculation function."  This is apparently a standard.
				PKCS5S2ParametersGenerator generator =
					new PKCS5S2ParametersGenerator();
				// Initialize the generator with our password and salt.  Note the
				// iteration count value.  Examples I found around the Net set this
				// as a hex value, but I'm not sure why advantage there is to that.
				// I changed it to decimal for clarity.  Ideally, this should be a
				// very large number, but experiments seem to show that setting this
				// too high makes the program sluggish.  We'll stick to the same
				// key iteration count we've been using.
				generator.init(pwd, salt, KEY_ITERATION_COUNT);
				// Generate our parameters.  We want to do AES-256, so we'll set
				// that as our key size.  That also implies a 128-bit IV.
				iv = ((ParametersWithIV)generator.generateDerivedParameters(KEY_SIZE,
						IV_SIZE));
				// Create our AES (i.e. Rijndael) engine and create the actual
				// cipher object from it.  We'll use CBC padding.
				RijndaelEngine engine = new RijndaelEngine();
				cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(engine));
			// If the password was not set, we'll null out the cipher and IV to
			// prevent encryption from taking place:
			} else {
				cipher = null;
				iv = null;
			}
		}
		// If anything blew up, null out the cipher and IV as well:
		catch (Exception e)
		{
			cipher = null;
			iv = null;
		}
    }

    /**
     * Either encrypt or decrypt the specified byte array.  Whichever mode
     * we use depends on the mode specified.  I pulled this out into a single
     * private method because the process is the same either way with the exception
     * of the mode (encrypt or decyrpt).  Other classes will use the protected
     * encryptSequenceKey() and decryptSequenceKey() methods instead, which will
     * make sure the right mode gets called.
	 * @param original The sequence key to encrypt/decrypt.  Note that this is a
	 * byte array and not a string.  When encrypting, use String.getBytes() to
	 * convert the string to raw bytes first.  When decrypting, use
	 * hexStringToBytes() to convert the encrypted string in hex format to bytes.
     * @param mode The mode.  Must be either Cipher.ENCRYPT_MODE or
     * Cipher.DECRYPT_MODE.
     * @return The encrypted or decrypted data as a byte array.  For encrypted data,
     * this may be converted to a string of hexadecimal characters suitable for
     * passing back into this method (via decryptSequenceKey()).  For decrypted data,
     * this data can be converted back to a string using the String(byte[], encoding)
     * constructor, although you should also do a String.trim() on that result to
     * remove extraneous nulls from the end.
     */
    private byte[] cryptSeqKey(byte[] original, int mode) {
    	// If either the original data or the cipher object are null, return null:
		if (original == null || original.length == 0 || cipher == null)
			return null;
		// Asbestos underpants:
		try {
			// Pick our mode, encryption or decryption:
			if (mode == Cipher.ENCRYPT_MODE) cipher.init(true, iv);
			else cipher.init(false, iv);
			// Perform the crypto and return the result:
			byte[] result = new byte[cipher.getOutputSize(original.length)];
			int bytesSoFar = cipher.processBytes(original, 0,
					original.length, result, 0);
			cipher.doFinal(result, bytesSoFar);
			return result;
		// If anything blew up, return null:
		} catch (Exception e) {
			return null;
		}
    }
}
