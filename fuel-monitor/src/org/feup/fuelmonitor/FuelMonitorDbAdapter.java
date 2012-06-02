/*
 * Copyright (C) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.feup.fuelmonitor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Simple notes database access helper class. Defines the basic CRUD operations
 * for the notepad example, and gives the ability to list all notes as well as
 * retrieve or modify a specific note.
 * 
 * This has been improved from the first version of this tutorial through the
 * addition of better error handling and also using returning a Cursor instead
 * of using a collection of inner classes (which is less scalable and not
 * recommended).
 */
public class FuelMonitorDbAdapter {

	private static final String TAG = "FuelMonitorDbAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	/**
	 * Database creation sql statement
	 */
	private static final String FUELTYPE_CREATE = "CREATE TABLE FuelType ("
			+ "  _id INTEGER PRIMARY KEY,"
			+ "  name nvarchar2 NOT NULL UNIQUE);";
	private static final String MAKE_CREATE = "CREATE TABLE Make ("
			+ "  _id INTEGER PRIMARY KEY,"
			+ "  name nvarchar2 NOT NULL UNIQUE);";
	private static final String VEHICLE_CREATE = "CREATE TABLE Vehicle ("
			+ "  _id INTEGER PRIMARY KEY," + "  kms integer NOT NULL ,"
			+ "  year integer NOT NULL ," + "  fuelCapacity integer NOT NULL ,"
			+ "  registration nvarchar2 NOT NULL UNIQUE ON CONFLICT IGNORE,"
			+ "  model nvarchar2 NOT NULL," + "  idMake integer NOT NULL,"
			+ "  idFuelType integer REFERENCES FuelType ON DELETE CASCADE);";
	private static final String FUELING_CREATE = "CREATE TABLE Fueling ("
			+ "  _id INTEGER PRIMARY KEY," + "  date date NOT NULL ,"
			+ "  kmsAtFueling integer NOT NULL ,"
			+ "  fuelStation nvarchar2 NOT NULL ,"
			+ "  quantity double NOT NULL ," + "  cost float NOT NULL ,"
			+ "  courseTypeCity integer NOT NULL ,"
			+ "  courseTypeRoad integer NOT NULL ,"
			+ "  courseTypeFreeway integer NOT NULL ,"
			+ "  drivingStyle integer NOT NULL ,"
			+ "  idVehicle integer REFERENCES Vehicle ON DELETE CASCADE);";

	private static final String DATABASE_NAME = "data";
	private static final int DATABASE_VERSION = 1;

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		private final Context mCtx;

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mCtx = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			Log.i(TAG, "Creating FuelType table");
			db.execSQL(FUELTYPE_CREATE);
			Log.i(TAG, "Creating Make table");
			db.execSQL(MAKE_CREATE);
			Log.i(TAG, "Creating Vehicle table");
			db.execSQL(VEHICLE_CREATE);
			Log.i(TAG, "Creating Fueling table");
			db.execSQL(FUELING_CREATE);

			// TODO Ship app with pre-populated and created database
			Log.i(TAG, "Populating FuelType table");
			ContentValues fuelTypes = new ContentValues();
			String[] fuelTypesList = mCtx.getResources().getStringArray(
					R.array.fuelType_database_list);

			for (int i = 0; i < fuelTypesList.length; i++) {
				fuelTypes.put("name", fuelTypesList[i]);
				db.insert("fuelType", null, fuelTypes);
			}

			Log.i(TAG, "Populating Make table");
			ContentValues makes = new ContentValues();
			String[] makeList = mCtx.getResources().getStringArray(
					R.array.make_database_list);

			for (int i = 0; i < makeList.length; i++) {
				makes.put("name", makeList[i]);
				db.insert("make", null, makes);
			}

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS Fueling;");
			db.execSQL("DROP TABLE IF EXISTS FuelType;");
			db.execSQL("DROP TABLE IF EXISTS Make;");
			db.execSQL("DROP TABLE IF EXISTS Model;");
			db.execSQL("DROP TABLE IF EXISTS Vehicle;");
			onCreate(db);
		}
	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public FuelMonitorDbAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	/**
	 * Open the Fuel Monitor database. If it cannot be opened, try to create a
	 * new instance of the database. If it cannot be created, throw an exception
	 * to signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public FuelMonitorDbAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();

		if (!mDb.isReadOnly())
			// Enable foreign key constraints (MAY NOT WORK ON < 2.1)
			mDb.execSQL("PRAGMA foreign_keys=ON;");

		return this;
	}

	public void close() {
		mDbHelper.close();
	}

	public long addVehicle(long make, String model, long l, short fuelCapacity,
			String registration, short year, int kms) {
		ContentValues vehicle = new ContentValues();
		vehicle.put("idMake", make);
		vehicle.put("model", model);
		vehicle.put("idFuelType", l);
		vehicle.put("fuelCapacity", fuelCapacity);
		vehicle.put("registration", registration);
		vehicle.put("year", year);
		vehicle.put("kms", kms);
		return mDb.insert("vehicle", null, vehicle);
	}

	public long editVehicle(long rowId, long make, String model, long l,
			short fuelCapacity, String registration, short year, int kms) {
		ContentValues vehicle = new ContentValues();
		vehicle.put("idMake", make);
		vehicle.put("model", model);
		vehicle.put("idFuelType", l);
		vehicle.put("fuelCapacity", fuelCapacity);
		vehicle.put("registration", registration);
		vehicle.put("year", year);
		vehicle.put("kms", kms);
		return mDb.update("vehicle", vehicle, "_id = ?",
				new String[] { String.valueOf(rowId) });
	}

	public long addFueling(String date, int kms, String fuelStation,
			float quantity, float cost, int courseTypeCity, int courseTypeRoad,
			int courseTypeFreeway, int drivingStyle, long vehicle) {
		ContentValues fueling = new ContentValues();
		fueling.put("date", date);
		fueling.put("kmsAtFueling", kms);
		fueling.put("fuelStation", fuelStation);
		fueling.put("quantity", quantity);
		fueling.put("cost", cost);
		fueling.put("courseTypeCity", courseTypeCity);
		fueling.put("courseTypeRoad", courseTypeRoad);
		fueling.put("courseTypeFreeway", courseTypeFreeway);
		fueling.put("drivingStyle", drivingStyle);
		fueling.put("idVehicle", vehicle);
		return mDb.insert("fueling", null, fueling);
	}

	public long editFueling(long rowId, String date, int kms,
			String fuelStation, float quantity, float cost, int courseTypeCity,
			int courseTypeRoad, int courseTypeFreeway, int drivingStyle,
			long vehicle) {
		ContentValues fueling = new ContentValues();
		fueling.put("date", date);
		fueling.put("kmsAtFueling", kms);
		fueling.put("fuelStation", fuelStation);
		fueling.put("quantity", quantity);
		fueling.put("cost", cost);
		fueling.put("courseTypeCity", courseTypeCity);
		fueling.put("courseTypeRoad", courseTypeRoad);
		fueling.put("courseTypeFreeway", courseTypeFreeway);
		fueling.put("drivingStyle", drivingStyle);
		fueling.put("idVehicle", vehicle);
		return mDb.update("fueling", fueling, "_id = ?",
				new String[] { String.valueOf(rowId) });
	}

	public Cursor fetchFuelingTypes() {

		return mDb.query("FuelType", new String[] { "_id", "name" }, null,
				null, null, null, null);
	}

	public Cursor fetchMakes() {

		return mDb.query("Make", new String[] { "_id", "name" }, null, null,
				null, null, null);
	}

	public Cursor fetchVehicles() {
		return mDb
				.rawQuery(
						"SELECT V._id, model, M.name as makeName, registration FROM Make M, Vehicle V WHERE V.idmake = M._id",
						null);
	}

	public Cursor getVehicleByID(long rowId) {
		return mDb.query("Vehicle", null, "_id=?",
				new String[] { String.valueOf(rowId) }, null, null, null);
	}

	public Cursor getFuelingByID(long rowId) {
		return mDb.query("Fueling", null, "_id=?",
				new String[] { String.valueOf(rowId) }, null, null, null);
	}

	public String getRegistrationByID(long rowId) {
		Cursor result = mDb.query("Vehicle", new String[] { "registration" },
				"_id=?", new String[] { String.valueOf(rowId) }, null, null,
				null);
		result.moveToFirst();
		return result.getString(0);
	}

	/*
	 * public long getIDByRegistration(String registration) { Cursor result =
	 * mDb.query("Vehicle", new String[] { "_id" }, "registration=?", new
	 * String[] { registration }, null, null, null); result.moveToFirst();
	 * return result.getLong(0); }
	 */

	public boolean deleteVehicle(long rowId) {
		return mDb.delete("vehicle", "_id=?",
				new String[] { String.valueOf(rowId) }) > 0;
	}

	public int getNumVehicles() {
		Cursor result = mDb.rawQuery("SELECT COUNT(*) FROM Vehicle", null);
		result.moveToFirst();
		return result.getInt(0);
	}

	public boolean deleteFueling(long rowId) {
		return mDb.delete("fueling", "_id=?",
				new String[] { String.valueOf(rowId) }) > 0;
	}

	public Cursor fetchFuelingsByVehicleID(long rowId) {
		return mDb.query("Fueling", new String[] { "_id", "quantity", "cost" },
				"idVehicle=?", new String[] { String.valueOf(rowId) }, null,
				null, null);
	}

	/*
	 * TO IMPLEMENT
	 * 
	 * /** Create a new note using the title and body provided. If the note is
	 * successfully created return the new rowId for that note, otherwise return
	 * a -1 to indicate failure.
	 * 
	 * @param title the title of the note
	 * 
	 * @param body the body of the note
	 * 
	 * @return rowId or -1 if failed
	 * 
	 * public long createNote(String title, String body) { ContentValues
	 * initialValues = new ContentValues(); initialValues.put(KEY_TITLE, title);
	 * initialValues.put(KEY_BODY, body);
	 * 
	 * return mDb.insert(DATABASE_TABLE, null, initialValues); }
	 * 
	 * /** Delete the note with the given rowId
	 * 
	 * @param rowId id of note to delete
	 * 
	 * @return true if deleted, false otherwise
	 * 
	 * public boolean deleteNote(long rowId) {
	 * 
	 * return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0; }
	 * 
	 * /** Return a Cursor over the list of all notes in the database
	 * 
	 * @return Cursor over all notes
	 * 
	 * public Cursor fetchAllNotes() {
	 * 
	 * return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TITLE,
	 * KEY_BODY}, null, null, null, null, null); }
	 * 
	 * /** Return a Cursor positioned at the note that matches the given rowId
	 * 
	 * @param rowId id of note to retrieve
	 * 
	 * @return Cursor positioned to matching note, if found
	 * 
	 * @throws SQLException if note could not be found/retrieved
	 * 
	 * public Cursor fetchNote(long rowId) throws SQLException {
	 * 
	 * Cursor mCursor =
	 * 
	 * mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TITLE,
	 * KEY_BODY}, KEY_ROWID + "=" + rowId, null, null, null, null, null); if
	 * (mCursor != null) { mCursor.moveToFirst(); } return mCursor;
	 * 
	 * }
	 * 
	 * /** Update the note using the details provided. The note to be updated is
	 * specified using the rowId, and it is altered to use the title and body
	 * values passed in
	 * 
	 * @param rowId id of note to update
	 * 
	 * @param title value to set note title to
	 * 
	 * @param body value to set note body to
	 * 
	 * @return true if the note was successfully updated, false otherwise
	 * 
	 * public boolean updateNote(long rowId, String title, String body) {
	 * ContentValues args = new ContentValues(); args.put(KEY_TITLE, title);
	 * args.put(KEY_BODY, body);
	 * 
	 * return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) >
	 * 0; }
	 */
}
