/* CardDBAdapter.java
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * The CardDBAdapter encapsulates all database actions for the Perfect Paper Passwords
 * application.
 * @author Jeffrey T. Darlington
 * @version 1.0
 * @since 1.0
 */
public class CardDBAdapter {

	/** I *think* this is used for the SQLiteOpenHelper.onUpgrade() log and
     *  nowhere else.  That said, I'm not sure what other purpose this
     *  constant may serve. */
    private static final String TAG = "PPP-CardDBAdapter";
    /** An instance of our internal DatabaseHelper class*/
    private DatabaseHelper mDbHelper;
    /** A reference to the underlying SQLiteDatabase */
    private SQLiteDatabase mDb;
    
    /** A constant representing the name of the database. */
    private static final String DATABASE_NAME = "ppp";
    /** A constant representing the card set data table in the database. */
    private static final String DATABASE_TABLE_CARDSETS = "cardsets";
    /** A constant representing the "strike outs" data table in the database. */
    private static final String DATABASE_TABLE_STRIKEOUTS = "strikeouts";
    /** The version of this database. */
    private static final int DATABASE_VERSION = 1;
    
    public static final String KEY_CARDSETID = "_id";
    public static final String KEY_NAME = "name";

    /** The SQL statement to create the card sets database table */
    private static final String DATABASE_CREATE_CARDSETS_SQL =
            "create table " + DATABASE_TABLE_CARDSETS +
            	" (" + KEY_CARDSETID + " integer primary key autoincrement, "
        		+ KEY_NAME + " text not null, sequence_key text not null, "
        		+ "alphabet text not null, columns integer not null, "
        		+ "rows integer not null, passcode_length integer not null, "
        		+ "last_card integer not null);";
    
    /** The SQL statement to create the "strike outs" database table */
    private static final String DATABASE_CREATE_STRIKEOUTS_SQL =
        	"create table " + DATABASE_TABLE_STRIKEOUTS +
        		" (" + KEY_CARDSETID + " integer primary key autoincrement, "
        		+ "cardset_id integer not null, "
        		+ "card integer not null, col integer not null, "
        		+ "row integer not null);";
    
    /** The SQL statement to create the primary index on the "strike out" table.
     *  This index creates a unique index on all four primary columns, which forces
     *  only one row per combination. */
    private static final String DATABASE_CREATE_INDEX1_SQL =
        	"create unique index strikeindxmain on " + DATABASE_TABLE_STRIKEOUTS +
        		" (cardset_id, card, col, row);";
    
    /** The SQL statement to create the card set index on the "strike out" table.
     *  This will be used identify all strike out data for a given card set. */
    private static final String DATABASE_CREATE_INDEX2_SQL =
    		"create index strikeindxcardset on " + DATABASE_TABLE_STRIKEOUTS +
    			" (cardset_id);";
    
    /** The SQL statement to create the card set and card number index on the "strike
     *  out table.  This will be used to find all strikes for a given card in a given
     *  card set. */
    private static final String DATABASE_CREATE_INDEX3_SQL =
        	"create index strikeindxcardsetcard on " + DATABASE_TABLE_STRIKEOUTS +
    			" (cardset_id, card);";


    /** Our calling Context. */
    private final Context mCtx;

    /**
     * This helper wraps a little bit of extra functionality around the
     * default SQLiteOpenHelper, giving it a bit more code specific to
     * how Cryptnos works.
     * @author Jeffrey T. Darlington
	 * @version 1.0
	 * @since 1.0
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	// While sqlite lets us have multiple tables per database, Android's
        	// SQLiteDatabase class doesn't like multiple SQL statements in the
        	// same execSQL() command.  Thus, we need to break out each statement
        	// into a separate call.  Otherwise, only the first statement will be
        	// executed and the rest will be ignored.  It took me forever (and a
        	// few Google searches) to figure this out.
    		db.execSQL(DATABASE_CREATE_CARDSETS_SQL);
    		db.execSQL(DATABASE_CREATE_STRIKEOUTS_SQL);
    		db.execSQL(DATABASE_CREATE_INDEX1_SQL);
    		db.execSQL(DATABASE_CREATE_INDEX2_SQL);
    		db.execSQL(DATABASE_CREATE_INDEX3_SQL);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        	// This is pretty much a tweak of many sample "upgrades" in various
        	// Android projects and is very destructive.  It basically drops and
        	// recreates the database.  In the future, we'll want to tweak this
        	// to preserve data by altering tables rather than destroying them.
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            // Drop the indices first, then the tables:
            db.execSQL("DROP INDEX IF EXISTS strikeindxmain;");
            db.execSQL("DROP INDEX IF EXISTS strikeindxcardset;");
            db.execSQL("DROP INDEX IF EXISTS strikeindxcardsetcard;");
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_CARDSETS);
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_STRIKEOUTS);
            // Now rebuild the tables from scratch:
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public CardDBAdapter(Context ctx) {
        this.mCtx = ctx;
    }
    
    /* ##### Public Methods ##### */
    
    /**
     * Open the PPP database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public CardDBAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    /**
     * Close the PPP database.
     */
    public void close() {
        mDbHelper.close();
    }
    
    /**
     * Given a Cardset object, save it to the database.  If the card set does not
     * exist, a new record will be created and a new card set ID assigned.  If the
     * card set already exists, it will be updated.
     * @param cardset The Cardset object to save 
     * @return Upon success, returns the card set ID number in the database.  This
     * should be the same value as the original ID if the card set already existed,
     * or the new ID if the card is new.  If the save fails, this returns Cardset.NOID.
     */
    public long saveCardset(Cardset cardset) {
    	// Check to see if the card set already has an ID assigned:
    	if (cardset.getCardsetId() != Cardset.NOID) {
    		// If so, make sure it actually exists in the database:
    		Cardset c = getCardset(cardset.getCardsetId());
    		// If it does, try to update the existing card set.  Return the
    		// card set's ID if success for or NOID if it fails.
    		if (c != null) {
    			if (updateCardset(cardset)) return cardset.getCardsetId();
    			else return Cardset.NOID;
    		// Here, the card set says it has an ID but we couldn't find it.
    		// Go ahead and try to add the card set as new and return its new
    		// ID number.
    		} else return addCardset(cardset);
    	// If the card set has no ID, try to add it as new and return its new ID:
    	} else return addCardset(cardset);
    }

    /**
     * Delete the specified card set from the database.  Note that this will delete
     * all card set parameters, as well as any toggle data associated with its
     * individual cards.
     * @param cardsetId The internal database ID of the card set to delete
     * @return True on success, false on failure.
     */
    public boolean deleteCardset(long cardsetId) {
    	// Try to delete the toggle data first:
    	if (clearAllTogglesForCardset(cardsetId))
    		// If that succeeded, delete the actual card set parameters:
    		return mDb.delete(DATABASE_TABLE_CARDSETS, 
    				KEY_CARDSETID + "=" + cardsetId, null) > 0;
    	else return false;
    }
    
    /**
     * Delete the specified card set from the database.  Note that this will delete
     * all card set parameters, as well as any toggle data associated with its
     * individual cards.
     * @param cardset A Cardset object representing an existing card set in the
     * database
     * @return True on success, false on failure.
     */
    public boolean deleteCardset(Cardset cardset) {
    	// No sense reinventing the wheel:
    	return deleteCardset(cardset.getCardsetId());
    }
    
    public boolean deleteAllCardsets() {
        mDb.execSQL("DROP INDEX IF EXISTS strikeindxmain;");
        mDb.execSQL("DROP INDEX IF EXISTS strikeindxcardset;");
        mDb.execSQL("DROP INDEX IF EXISTS strikeindxcardsetcard;");
        mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_CARDSETS);
        mDb.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_STRIKEOUTS);
        mDb.execSQL(DATABASE_CREATE_CARDSETS_SQL);
        mDb.execSQL(DATABASE_CREATE_STRIKEOUTS_SQL);
        mDb.execSQL(DATABASE_CREATE_INDEX1_SQL);
        mDb.execSQL(DATABASE_CREATE_INDEX2_SQL);
        mDb.execSQL(DATABASE_CREATE_INDEX3_SQL);
    	return true;
    }
    
    /**
     * Retrieve a card set from the database
     * @param cardsetId The internal database ID of the card set to retrieve
     * @return A Cardset object containing the card set's parameters
     */
    public Cardset getCardset(long cardsetId) {
    	// Asbestos underpants:
    	try {
    		// Query the DB to see if we can get the card set:
    		Cursor c = mDb.rawQuery("select * from " + DATABASE_TABLE_CARDSETS +
    				" where " + KEY_CARDSETID + " = " + cardsetId + ";", null);
    		// If the query was successful, create a new Cardset object and
    		// populate it:
    		if (c != null) {
    			c.moveToFirst();
    			Cardset cs = new Cardset(
    					c.getLong(c.getColumnIndex(KEY_CARDSETID)),
    					c.getString(c.getColumnIndex(KEY_NAME)),
    					c.getInt(c.getColumnIndex("columns")),
    					c.getInt(c.getColumnIndex("rows")), 
    					c.getInt(c.getColumnIndex("passcode_length")),
    					c.getString(c.getColumnIndex("alphabet")),
    					c.getString(c.getColumnIndex("sequence_key")),
    					c.getInt(c.getColumnIndex("last_card")));
    			c.close();
    			return cs;
    		// If the query didn't return anything, return null:
    		} else return null;
    	// If anything blew up, return null;
    	} catch (Exception e) {
    		return null;
    	}
    }
    
    /**
     * Change a card set's display name
     * @param cardsetId The internal database ID of the card set to rename
     * @param newName The new display name
     * @return True on success, false on failure
     */
    public boolean renameCardset(long cardsetId, String newName) {
    	try {
    		ContentValues values = new ContentValues();
    		values.put(KEY_NAME, newName);
    		return mDb.update(DATABASE_TABLE_CARDSETS, values, KEY_CARDSETID + "=" +
    				cardsetId, null) > 0;
    	} catch (Exception e) {
    		return false;
    	}
    }
    
    /**
     * Clear all toggle or "strike out" data for the specified card set
     * @param cardsetId The internal database ID for the specified card set
     * @return True on success, false on failure
     */
    public boolean clearAllTogglesForCardset(long cardsetId) {
    	// Originally, I just blindly attempted a delete here and returned the
    	// boolean result.  Unfortunately, if we call this in a situation where
    	// there are no toggles, that returns false.  Unfortunately, I planned
    	// to use this as a means of reusing code when deleting the entire card
    	// set, but the false was throwing me off.  So now we'll actually check
    	// to see if there's data to delete first.  If not, go ahead and return
    	// true; otherwise, attempt the delete and return that result.
    	Cursor c = mDb.rawQuery("select * from " + DATABASE_TABLE_STRIKEOUTS +
    			" where " + KEY_CARDSETID + " = " + cardsetId + ";", null);
    	if (c != null) {
    		if (c.getCount() > 0) {
    			c.close();
    	    	return mDb.delete(DATABASE_TABLE_STRIKEOUTS,
    	    			"cardset_id = " + cardsetId, null) > 0;
    		} else {
    			c.close();
    			return true;
    		}
    	} else return true;
    }
    
    /**
     * Clear all toggle or "strike out" data for the specified card set
     * @param cardset A Cardset object representing a card set already saved to
     * the database
     * @return True on success, false on failure
     */
    public boolean clearAllTogglesForCardset(Cardset cardset) {
    	return clearAllTogglesForCardset(cardset.getCardsetId());
    }
    
    /**
     * Clear all toggle or "strike out" data for the specified card in the specified
     * card set
     * @param cardsetId The internal database ID for the selected card set
     * @param card The card number
     * @return True on success, false on failure
     */
    public boolean clearTogglesForCard(long cardsetId, int card) {
    	return mDb.delete(DATABASE_TABLE_STRIKEOUTS,
    			"cardset_id = " + cardsetId + " and card = " + card, null) > 0; 
    }
    
    /**
     * Clear all toggle or "strike out" data for the last (i.e. current) card in
     * the specified card set
     * @param cardset A Cardset object
     * @return True on success, false on failure
     */
    public boolean clearTogglesForLastCard(Cardset cardset) {
    	return clearTogglesForCard(cardset.getCardsetId(), cardset.getLastCard());
    }
    
    /**
     * Get all toggle or "strike out" data for the last (i.e. current) card in the
     * specified card set.
     * @param cardSet The Cardset to get the data for.  This card set must have been
     * saved to the database and thus have been assigned an internal database ID.
     * If the card set has not been saved, this method will always return an array
     * with every value set to false.
     * @return A two-dimensional boolean array containing whether or not a given
     * passcode on the card has been toggled. True indicates that passcode is "struck
     * through", while false indicates it is "clear". The first dimension is the row,
     * while the second dimension is the column. Note that the arrays are zero-based,
     * so you may need to adjust indexes to get the right value.  Also note that if
     * an error occurs, a null value may be returned.
     */
    public boolean[][] getTogglesForLastCard(Cardset cardSet) {
    	// Asbestos underpants:
    	try {
    		// Start by declaring our array of boolean values, then initializing them
    		// all to false.  It might be that the initialization loop is unnecessary,
    		// as Java may default uninitialized booleans to false, but it's better to
    		// be safe than sorry.
    		boolean[][] toggles =
    			new boolean[cardSet.getNumberOfRows()][cardSet.getNumberOfColumns()];
    		for (int row = 0; row < cardSet.getNumberOfRows(); row++) {
    			for (int col = 0; col < cardSet.getNumberOfColumns(); col++) {
    				toggles[row][col] = false;
    			}
    		}
    		// If the specified card set does not have an internal ID set yet, we
    		// can't have any toggle data stored in the database.  Go ahead and
    		// short-cut things here and return the all-false array:
    		if (cardSet.getCardsetId() == Cardset.NOID) return toggles;
    		// Now query the database to get our column and row data based on the
    		// card set ID and current card.  Rows will only be stored in the database
    		// if a given passcode has been toggled, so if a given row/column pair
    		// is not returned, it defaults to false.
    		Cursor c = mDb.rawQuery("select col, row from " +
    				DATABASE_TABLE_STRIKEOUTS + " where cardset_id = " +
    				cardSet.getCardsetId() + " and card = " +
    				cardSet.getLastCard() + ";", null);
    		// If we got any useful data, start moving through it:
    		if (c != null && c.getCount() > 0) {
    			c.moveToFirst();
    			while (!c.isAfterLast()) {
    				// Get the row and column value from the result set row and
    				// toggle the boolean.  Note that we need to adjust the array
    				// indices since they're zero-based. 
    				int row = c.getInt(c.getColumnIndex("row"));
    				int col = c.getInt(c.getColumnIndex("col"));
    				toggles[row - 1][col - 1] = true;
    				c.moveToNext();
    			}
    		}
    		// Close the cursor if we got anything, then return the toggle array:
    		if (c != null) c.close();
    		return toggles;
    	// If anything blows up, return a null array to indicate an error:
    	} catch (Exception e) {
    		return null;
    	}
    }
    
    /**
     * Toggle the "strike out" state of a given passcode on the specified card.
     * @param cardsetId The internal database ID of the card set containing the card
     * @param card The card within the card set where the passcode is located
     * @param column The column of the toggled passcode
     * @param row The row of the toggled passcode
     * @return True on success, false on failure
     */
    public boolean tooglePasscode(long cardsetId, int card, int column, int row) {
    	try {
    		// First we have to see if the specified combination of card set, card,
    		// column, and row exists.  If it does, we'll need to delete it; if it
    		// doesn't, we'll need to add it.
    		Cursor c = mDb.rawQuery("select * from " + DATABASE_TABLE_STRIKEOUTS +
    				" where cardset_id = " + cardsetId + " and card = " + card +
    				" and col = " + column + " and row = " + row + ";", null);
    		// If we got a result, delete the "strike out" from the database:
    		if (c != null && c.getCount() >= 1) {
    			c.close();
    			return mDb.delete(DATABASE_TABLE_STRIKEOUTS, "cardset_id = " + 
    					cardsetId + " and card = " + card + " and col = " + column +
    					" and row = " + row, null) > 0;
    		// If we didn't get a result, the "strike out" does not exist so it
    		// must be created:
    		} else {
    			if (c != null) c.close();
    			ContentValues values = new ContentValues();
        		values.put("cardset_id", cardsetId);
        		values.put("card", card);
        		values.put("col", column);
        		values.put("row", row);
    			return mDb.insert(DATABASE_TABLE_STRIKEOUTS, null, values) > 0L;
    		}
    	// If anything blows up, return failure:
    	} catch (Exception e) {
    		return false;
    	}
    }
    
    /**
     * Get a Cursor to drive the main menu activity's list view
     * @return A Cursor containing the name and card set ID of all card sets currently
     * stored in the database.  This may return null if no card sets were found.
     */
    public Cursor getCardsetListMenuItems() {
    	try {
    		Cursor c = mDb.rawQuery("select " + KEY_NAME + ", " + KEY_CARDSETID +
    				" from " + DATABASE_TABLE_CARDSETS + " order by " + KEY_NAME +
    				" asc;", null);
    		if (c != null && c.getCount() >= 1) return c; 
    		else return null;
    	} catch (Exception e) {
    		return null;
    	}
    }
    
    /* ##### Private Methods ##### */
    
    /**
     * Add a new Cardset to the database where we know it does not already exist.
     * @param cardset The Cardset object representing the values to add to the database
     * @return The internal database ID of the newly saved card set. If the save
     * fails, return Cardset.NOID.
     */
    private long addCardset(Cardset cardset) {
    	// Simple enough: Try to add the card set to the database.  If it works,
    	// return the new ID; if it doesn't, return NOID:
    	try {
    		ContentValues initialValues = new ContentValues();
    		initialValues.put(KEY_NAME, cardset.getName());
    		initialValues.put("sequence_key", cardset.getSequenceKey());
    		initialValues.put("alphabet", cardset.getAlphabet());
    		initialValues.put("columns", cardset.getNumberOfColumns());
    		initialValues.put("rows", cardset.getNumberOfRows());
    		initialValues.put("passcode_length", cardset.getPasscodeLength());
    		initialValues.put("last_card", cardset.getLastCard());
    		return mDb.insert(DATABASE_TABLE_CARDSETS, null, initialValues);
    	} catch (Exception e) {
    		return Cardset.NOID;
    	}
    }
    
    /**
     * Update an existing card set's data in the database
     * @param cardset A Cardset object representing the data to update.
     * @return True on success, false on failure
     */
    private boolean updateCardset(Cardset cardset) {
    	try {
    		ContentValues values = new ContentValues();
    		values.put(KEY_NAME, cardset.getName());
    		values.put("sequence_key", cardset.getSequenceKey());
    		values.put("alphabet", cardset.getAlphabet());
    		values.put("columns", cardset.getNumberOfColumns());
    		values.put("rows", cardset.getNumberOfRows());
    		values.put("passcode_length", cardset.getPasscodeLength());
    		values.put("last_card", cardset.getLastCard());
    		return mDb.update(DATABASE_TABLE_CARDSETS, values, KEY_CARDSETID + "=" +
    				cardset.getCardsetId(), null) > 0;
    	} catch (Exception e) {
    		return false;
    	}
    }
    

}
