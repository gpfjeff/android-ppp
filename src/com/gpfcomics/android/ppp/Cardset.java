/* Cardset.java
 * 
 * PROGRAMMER:    Jeffrey T. Darlington
 * DATE:          February 23, 2011
 * PROJECT:       Perfect Paper Passwords for Android
 * ANDROID V.:	  1.1
 * 
 * The Cardset class represents the defining parameters and current basic state of
 * a given set of cards within Perfect Paper Passwords.  This class contains all
 * the necessary parameters to define a given set of cards, such as its sequence key,
 * alphabet, number of rows and columns on each card, and the passcode length.  It
 * also defines the current state of the card set within our application, giving it
 * a name, an internal ID for the database, and the last card that we used.
 * 
 * To help put all the validation code in one place, there is a series of public static
 * methods near the bottom to validate strings and, in some cases, the base primitive
 * types for each parameter of the card set.  This way, UI elements such as the New
 * Card Activity can call on the same validation code as this class uses internally.
 * These all return Boolean values which, while not giving detailed feedback on why the
 * validation failed, does give a simple pass/fail check. 
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

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * The Cardset class represents the defining parameters and current basic state of
 * a given set of cards within Perfect Paper Passwords.  This class contains all
 * the necessary parameters to define a given set of cards, such as its sequence key,
 * alphabet, number of rows and columns on each card, and the passcode length.  It
 * also defines the current state of the card set within our application, giving it
 * a name, an internal ID for the database, and the last card that we used.
 * @author Jeffrey T. Darlington
 * @version 1.0
 * @since 1.0
 */
public class Cardset {
	
    /* ####### Private Constants ####### */
    
	/** A regular expression for determining whether or not a string is to be
	 *  considered empty.  If the string has no characters or consists entirely of
	 *  white space, it is considered empty.  Note that testing for a null value
	 *  will be an separate check. */
	private static final String EMPTY_STRING_REGEX = "^\\s*$";
	
	/** A regular expression for identifying positive integers.  There must be no
	 *  white space, alphabetic characters, or symbols; only one or more digits.
	 *  Note that this does not test for null strings, which must be a separate
	 *  check, nor does it currently test to see if the first digit is a zero. */
	private static final String POS_INTEGER_REGEX = "^\\d+$";
	
	/** A regular expression to identify whether or not the tested string (which we
	 *  assume has passed the POS_INTEGER_REGEX test above) contains leading zeros. */
	private static final String LEADING_ZEROS_REGEX = "^0+";
	
    /* ####### Public Constants ####### */
    
	/** The official internal ID number for card sets that have not been saved to
	 *  the database.  This will be the default ID for all cards until they have
	 *  been saved and assigned a new ID by the database. */
	public static final long NOID = -1L;
	/** The number of the first card in the sequence.  Card numbers may not be less
	 *  than this value, and all new card sets will start with this card number by
	 *  default. */
	public static final int FIRST_CARD = 1;
	/** In theory, a card set may have an infinite number of cards.  In reality, we're
	 *  using signed integers, meaning we can have Integer.MAX_VALUE cards.  While it
	 *  is extremely unlikely anyone will ever hit this value, we'll cap it here as
	 *  an extra layer of security.  A card set cannot exceed this number of cards,
	 *  and any attempt to move beyond it will fail. */
	public static final int FINAL_CARD = Integer.MAX_VALUE;
	/** The default number of columns per card.  This should match the value of the
	 *  PPP demonstration page on the GRC site. */
	public static final int DEFAULT_COLUMNS = 7;
	/** The default number of rows per card.  This should match the value of the
	 *  PPP demonstration page on the GRC site. */
	public static final int DEFAULT_ROWS = 10;
	/** The default passcode length.  This should match the value of the
	 *  PPP demonstration page on the GRC site. */
	public static final int DEFAULT_PASSCODE_LENGTH = 4;
	/** The default alphabet.  This should match the "standard and conservative"
	 *  64-character alphabet defined on the PPP demonstration page on the GRC site. */
	public static final String DEFAULT_ALPHABET =
		"!#%+23456789:=?@ABCDEFGHJKLMNPRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
	/** The "visually aggressive" 88-character alphabet defined on the PPP
	 *  demonstration page on the GRC site. */
	public static final String AGGRESSIVE_ALPHABET =
		"!\"#$%&'()*+,-./23456789:;<=>?@ABCDEFGHJKLMNOPRSTUVWXYZ[\\]^_abcdefghijkmnopqrstuvwxyz{|}~";
	/** A regular expression pattern used for testing sequence keys values to
	 *  make sure they are valid.  Sequence keys must be 64-character hex strings. */
	public static final String SEQUENCE_KEY_REGEX = "^[0-9a-fA-F]{64}$";
	/** The maximum practical width in displayed characters that will fit into
	 *  portrait orientation on a typical Android device screen.  Any product of the
	 *  number of columns and the passcode length that is greater than this should
	 *  force landscape orientation in the card view display. */
	public static final int MAX_PORTRAIT_WIDTH = 28; 
	/** The maximum practical width in displayed characters of any card.  The product
	 *  of the number of columns and the passcode length must be less than or equal
	 *  to this value or we won't be able to display it. */
	public static final int MAX_CARD_WIDTH = 52;
	
    /* ####### Private Members ####### */

	/** The card set's internal ID number */
	private long cardsetId = NOID;
	/** The card set's user-assigned name */
	private String name = "Unnamed Card Set";
	/** The number of columns per card */
	private int numColumns = DEFAULT_COLUMNS;
	/** The number of rows per card */
    private int numRows = DEFAULT_ROWS;
	/** The passcode length */
    private int passcodeLength = DEFAULT_PASSCODE_LENGTH;
	/** The alphabet for this card set */
    private String alphabet = DEFAULT_ALPHABET;
	/** The card set sequence key as a hexadecimal string */
    private String sequenceKey = null;
	/** The current (i.e. last) card number */
    private int lastCard = FIRST_CARD;
    
    /* ####### Constructors ####### */
    
    /**
     * Default constructor. Creates a new card set with all the default values, a
     * place-holder name, and a randomly generated sequence key. 
     */
    public Cardset() {
    	// All the other member variables above are pretty well initialized with
    	// good defaults.  The sequence key, however, needs to be something fairly
    	// random.  Odds are the user will overwrite whatever we set with a value
    	// provided by the service they wish to authenticate with, but we should
    	// still give them something to start with if they want to start by
    	// defining a "default" they can overwrite.
    	//
    	// java.security.SecureRandom is a much better source of random data than
    	// java.util.Random, which is too predictable.  The default constructor
    	// should give us a seed based on /dev/urandom or a similar source.  Get
    	// 32 bytes from there, then encode those bytes into hex format.
    	SecureRandom prng = new SecureRandom();
    	byte[] seqBytes = new byte[32];
    	prng.nextBytes(seqBytes);
    	sequenceKey = PPPApplication.bytesToHexString(seqBytes);
    }
	
    /**
     * Primary constructor.  Create a new card set with the specified parameters.
     * @param cardsetId The internal database ID
     * @param name A display name
     * @param numColumns The number of columns per card
     * @param numRows The number of rows per card
     * @param passcodeLength The length of each passcode
     * @param alphabet The alphabet to use for generating passcodes
     * @param sequenceKey The sequence key to seed the passcode sequence, specified
     * in hexadecimal characters
     * @param lastCard The last (i.e. current) card used
     * @throws IllegalArgumentException Thrown if any of the parameters are deemed
     * to be invalid. See the message text for additional details.
     */
    public Cardset(long cardsetId, String name, int numColumns, int numRows, 
    		int passcodeLength, String alphabet, String sequenceKey, int lastCard) {
    	if (!isValidCardsetId(cardsetId))
    		throw new IllegalArgumentException("Invalid card set ID number");
    	else if (!isValidNumberOfColumns(numColumns))
    		throw new IllegalArgumentException("Invalid number of columns");
    	else if (!isValidNumberOfRows(numRows))
    		throw new IllegalArgumentException("Invalid number of rows");
    	else if (!isValidPasscodeLength(passcodeLength))
    		throw new IllegalArgumentException("Invalid passcode length");
    	else if (!isValidCardNumber(lastCard))
    		throw new IllegalArgumentException("Invalid card number");
    	else if (!isValidName(name))
    		throw new IllegalArgumentException("Name is empty");
    	else if (!isValidSequenceKey(sequenceKey))
    		throw new IllegalArgumentException("Sequence Key is invalid");
    	else if (!isValidAlphabet(alphabet))
    		throw new IllegalArgumentException("Alphabet is empty");
    	else if (!fitsMaxCardWidth(numColumns, passcodeLength))
    		throw new IllegalArgumentException("Exceeds maximum card width");
    	else {
	    	this.cardsetId = cardsetId;
	    	this.name = name;
	    	this.numColumns = numColumns;
	    	this.numRows = numRows;
	    	this.passcodeLength = passcodeLength;
	    	this.alphabet = alphabet;
	    	this.sequenceKey = sequenceKey.toUpperCase();
	    	this.lastCard = lastCard;
    	}
    }
    
    /* ####### Public Methods ####### */
    
    /**
     * Get the internal database ID for this card set.  If set to Cardset.NOID, this
     * card set has not been saved to the database yet.
     * @return The internal database ID for this card set
     */
    public long getCardsetId() { return cardsetId; }
    
    /**
     * Get the display name for this card set
     * @return The display name for this card set
     */
    public String getName() { return name; }
    
    /**
     * Get the number of columns per card
     * @return The number of columns per card
     */
    public int getNumberOfColumns() { return numColumns; }

    /**
     * Get the number of rows per card
     * @return The number of rows per card
     */
    public int getNumberOfRows() { return numRows; }
    
    /**
     * Get the passcode length
     * @return The passcode length
     */
    public int getPasscodeLength() { return passcodeLength; }
    
    /**
     * Get the alphabet used to generate passcodes
     * @return The alphabet used to generate passcodes
     */
    public String getAlphabet() { return alphabet; }
    
    /**
     * Get the sequence key used to seed passcode generation, represented as a string
     * of hexadecimal characters
     * @return The sequence key used to seed passcode generation
     */
    public String getSequenceKey() { return sequenceKey; }
    
    /**
     * Get the last (i.e. current) card displayed in this card set
     * @return The last card displayed
     */
    public int getLastCard() { return lastCard; }

    /**
     * Set the internal database ID for this card set
     * @param cardsetId The new internal ID
     * @throws IllegalArgumentException Thrown if the specified ID number is invalid
     */
    public void setCardsetId(long cardsetId) {
    	if (isValidCardsetId(cardsetId)) this.cardsetId = cardsetId;
    	else throw new IllegalArgumentException("Invalid card set ID number");
    }
    
    /**
     * Set the display name for this card set
     * @param name The new display name
     * @throws IllegalArgumentException Thrown if the specified name is empty
     */
    public void setName(String name) {
    	if (isValidName(name)) this.name = name;
    	else throw new IllegalArgumentException("Name is empty");
    }
    
    /**
     * Set the number of columns per card.  As a practical limitation, the number
     * of columns is limited to 13, which is the practical limit for many Android
     * displays.
     * @param numColumns The new number of columns
     * @throws IllegalArgumentException Thrown if the specified number is invalid
     * or if the specified value and the passcode length will produce a card that is
     * too wide for us to display
     */
    public void setNumberOfColumns(int numColumns) {
    	if (!isValidNumberOfColumns(numColumns))
    		throw new IllegalArgumentException("Invalid number of columns");
    	if (!fitsMaxCardWidth(numColumns, passcodeLength))
    		throw new IllegalArgumentException("Exceeds maximum card width");
   		this.numColumns = numColumns;
    }
    
    /**
     * Set the number of rows per card
     * @param numRows The new number of rows
     * @throws IllegalArgumentException Thrown if the specified number is invalid
     */
    public void setNumberOfRows(int numRows) {
    	if (isValidNumberOfRows(numRows)) this.numRows = numRows;
    	else throw new IllegalArgumentException("Invalid number of rows");
    }
    
    /**
     * Set the passcode length.  Passcodes must be between 2 and 16 characters long.
     * @param passcodeLength The new passcode length
     * @throws IllegalArgumentException Thrown if the specified number is invalid
     * or if the specified value and the number of columns will produce a card that is
     * too wide for us to display
     */
    public void setPasscodeLength(int passcodeLength) {
    	if (!isValidPasscodeLength(passcodeLength))
    		throw new IllegalArgumentException("Invalid passcode length");
    	if (!fitsMaxCardWidth(numColumns, passcodeLength))
    		throw new IllegalArgumentException("Exceeds maximum card width");
   		this.passcodeLength = passcodeLength;
   	}
    
    /**
     * Set the alphabet used for generating passcodes
     * @param alphabet The new alphabet
     * @throws IllegalArgumentException Thrown if the alphabet is empty
     */
    public void setAlphabet(String alphabet) {
    	if (isValidAlphabet(alphabet)) this.alphabet = alphabet;
    	else throw new IllegalArgumentException("Alphabet is empty");
    }
    
    /**
     * Set the sequence key used for generating passcodes. Sequence keys must be
     * specified as a string of hexadecimal characters.
     * @param sequenceKey The new sequence key
     * @throws IllegalArgumentException Throw if the sequence key is invalid
     */
    public void setSequenceKey(String sequenceKey) {
    	if (isValidSequenceKey(sequenceKey))
    		this.sequenceKey = sequenceKey.toUpperCase();
    	else throw new IllegalArgumentException("Sequence key is invalid");
    }
    
    /**
     * Set the last (i.e. current) card displayed for this card set
     * @param lastCard The number of the last card displayed
     * @throws IllegalArgumentException Thrown if the card number is invalid
     */
    public void setLastCard(int lastCard) {
    	if (isValidCardNumber(lastCard)) this.lastCard = lastCard;
    	else throw new IllegalArgumentException("Invalid card number");
    }
    
    /**
     * Move the last (i.e. current) card to the card previous to the current one.
     * This method will never move beyond the first card.
     */
    public void previousCard() {
    	lastCard--;
    	if (lastCard < FIRST_CARD) lastCard = FIRST_CARD;
    }
    
    /**
     * Move the last (i.e. current) card to the card after the current one.  This
     * method will never move beyond the practical final card limit.
     */
    public void nextCard() {
    	if (lastCard != FINAL_CARD) lastCard++;
    }
    
    /* ####### Public Static Methods ####### */
    
    /**
     * Validate an internal card set ID.  IDs must be a long value that is a positive
     * integer or must be the special NOID constant value.
     * @param id The long value to validate
     * @return True if valid, false otherwise
     */
    public static boolean isValidCardsetId(long id) {
    	if (id == NOID || id > 0) return true;
    	else return false;
    }
    
    /**
     * Validate a potential card set name.  Names must be non-empty and cannot consist
     * entirely of white space.  Note that this doesn't check to see if there is any
     * unnecessary white space on the ends; you should trim the name before using it. 
     * @param name The name string to validate
     * @return True if valid, false otherwise
     */
    public static boolean isValidName(String name) {
		if (name == null || name.length() == 0 ||
				Pattern.matches(EMPTY_STRING_REGEX, name))
			return false;
		else return true;
    }
    
    /**
     * Validate a potential number of columns.  The number of columns must be a
     * positive integer less than or equal to 26.  The cap at 26 is an implementation
     * detail unique to our app, rather than a requirement of the PPP spec; any number
     * of columns beyond 26 cannot be practically displayed on many Android screens.
     * See Cardset.fitsMaxCardWidth().
     * @param number A number String to validate
     * @return True if valid, false otherwise
     */
    public static boolean isValidNumberOfColumns(String number) {
    	if (!isValidPositiveIntegerString(number)) return false;
    	int integer = Integer.parseInt(number);
    	// Why 26?  See isValidNumberOfColumns(int) below:
    	if (integer > 26) return false;
    	else return true;
    }
    
    /**
     * Validate a potential number of columns.  The number of columns must be a
     * positive integer less than or equal to 26.  The cap at 26 is an implementation
     * detail unique to our app, rather than a requirement of the PPP spec; any number
     * of columns beyond 26 cannot be practically displayed on many Android screens.
     * See Cardset.fitsMaxCardWidth().
     * @param number An integer to validate
     * @return True if valid, false otherwise
     */
    public static boolean isValidNumberOfColumns(int number) {
    	// Why 26?  Well, the maximum card width in characters is MAX_CARD_WIDTH (52).
    	// This is the product of the default number of columns (7) and the default
    	// passcode length (4).  Passcodes by definition can not be shorter than 2
    	// characters, giving us a maximum number of columns of 52 / 2 = 26.  Note
    	// that this alone does not validate if a given number of columns are OK;
    	// this just checks the outer bounds.  We also need to check fitsMaxCardWidth()
    	// to be absolutely certain.
    	if (number < 1 || number > 26) return false;
    	else return true;
    }    
    
    /**
     * Validate a potential number of rows.  The number of rows must be a positive
     * integer greater than zero.
     * @param number A number String to validate
     * @return True if valid, false otherwise
     */
    public static boolean isValidNumberOfRows(String number) {
    	if (!isValidPositiveIntegerString(number)) return false;
    	else return true;
    }
    
    /**
     * Validate a potential number of rows.  The number of rows must be a positive
     * integer greater than zero.
     * @param number An integer to validate
     * @return True if valid, false otherwise
     */
    public static boolean isValidNumberOfRows(int number) {
    	if (number < 1) return false;
    	else return true;
    }
    
    /**
     * Validate a potential passcode length.  Passcodes must be between 2 and 16
     * characters in length by definition from the PPP spec.
     * @param number A number String to validate
     * @return True if valid, false otherwise
     */
    public static boolean isValidPasscodeLength(String number) {
    	try {
    		int num = Integer.parseInt(number);
    		return isValidPasscodeLength(num);
    	} catch (Exception e) { return false; }
    }
    
    /**
     * Validate a potential passcode length.  Passcodes must be between 2 and 16
     * characters in length by definition from the PPP spec.
     * @param number An integer to validate
     * @return True if valid, false otherwise
     */
    public static boolean isValidPasscodeLength(int number) {
    	if (number < 2 || number > 16) return false;
    	else return true;
    }
    
    /**
     * Validate an alphabet string.  Alphabets must be non-empty, must not consist
     * entirely of white space, must be at least two characters long, and each
     * character may only appear once in the string.
     * @param alphabet The string to test
     * @return True if valid, false otherwise
     */
    public static boolean isValidAlphabet(String alphabet) {
    	// First off, make sure the alphabet string is not null, empty, or consists
    	// entirely of white space:
		if (alphabet == null || alphabet.length() < 2 ||
				Pattern.matches(EMPTY_STRING_REGEX, alphabet))
			return false;
		// To test whether or not characters appear multiple times, we'll split the
		// the string into a character array and declare a HashSet of characters.
		// Then we'll walk through the array, adding each character to the HashSet
		// one by one.  HashSet.add() returns false if we try to add the same item
		// more than once, so that should be a decent test.
		char[] chars = alphabet.toCharArray();
		HashSet<Character> charHash = new HashSet<Character>();
		for (int i = 0; i < chars.length; i++) {
			if (!charHash.add(new Character(chars[i]))) return false;
		}
		return true;
    }
    
    /**
     * Validate a sequence key string.  Sequence keys must consist of a string of
     * hexadecimal digits 64 characters long.  Anything else is considered invalid.
     * Both upper and lower case letter digits are allowed.
     * @param seqKey The string to text
     * @return True if valid, false otherwise
     */
    public static boolean isValidSequenceKey(String seqKey) {
    	if (seqKey == null) return false;
    	return Pattern.matches(SEQUENCE_KEY_REGEX, seqKey);
    }
    
    /**
     * Validate a card number.  Card number must be a positive integer between 1 and
     * the maximum value of an Integer.  Zero and negative numbers are not allowed.
     * @param card The integer to validate
     * @return True if valid, false otherwise
     */
    public static boolean isValidCardNumber(int card) {
    	if (card >= FIRST_CARD && card <= FINAL_CARD) return true;
    	else return false;
    }
    
    /**
     * Check the specified number of columns and the passcode length and see if the
     * card they will produce will fit within the typical Android display.  Note that
     * this assumes the input values are valid for their respective constraints, so
     * they should be validated with the other isValid...() methods first before
     * calling this.  If the combination of columns and length will not fit, the
     * user should be forced to change these values before the card set can be
     * created.
     * @param numColumns The number of columns
     * @param passcodeLength The password length
     * @return True if the specified combination will fit, false otherwise.
     */
    public static boolean fitsMaxCardWidth(int numColumns, int passcodeLength) {
    	if (numColumns * passcodeLength <= MAX_CARD_WIDTH) return true;
    	else return false;
    }
    
    /* ####### Private Static Methods ####### */
    
    /**
     * Validate a number field.  Number strings here must be positive integers, i.e.
     * no alphabetics or symbols, no sign, no white space.  It may not have leading
     * zeros.
     * @param number The string to test
     * @return True if valid, false otherwise
     */
    private static boolean isValidPositiveIntegerString(String number) {
		if (number == null || number.length() == 0 ||
				!Pattern.matches(POS_INTEGER_REGEX, number) ||
				Pattern.matches(LEADING_ZEROS_REGEX, number)) 
			return false;
		// This should be a safe parse, because if the string passes the regex
		// above, it shouldn't fail to parse:
		int numRowsI = Integer.parseInt(number);
		if (numRowsI < 1) return false;
		else return true;
    }
  
}
