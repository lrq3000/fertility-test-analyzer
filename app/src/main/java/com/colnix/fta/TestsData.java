/*
 * Copyright 2019 Colnix Technology
 *
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.colnix.fta;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * This class manages and exposes the tests information to the rest of the application.
 * <p>
 * Assumes single thread operation!
 */
public class TestsData extends SQLiteOpenHelper
{
   static final String TAG = "TestsData";

   /**
    * Database constants.
    */
   public static final int VERSION = 12;
   public static final String DATABASE = "FTA";

   /**
    * Types table and its fields.
    */
   public static final String TYPES_TABLE = "Types";
   public static final String TYPES_TYPE = "type";

   // Enumeration of the test entry types
   public static final int TYPES_TYPE_OVULATION = 1;
   public static final int TYPES_TYPE_PREGNANCY = 2;

   // Interpretation thresholds
   public static final int POSITIVE_OVULATION = 95;
   public static final int POSITIVE_PREGNANCY = 5;

   /**
    * Tests table and its fields.
    */
   public static final String TESTS_TABLE = "Tests";
   public static final String TESTS_ID = "_id";
   public static final String TESTS_DATE = "date";
   public static final String TESTS_TYPE = "type";
   public static final String TESTS_PIGMENTATION = "pigmentation";
   public static final String TESTS_PHOTO = "photo";
   public static final String TESTS_BRAND = "brand";
   public static final String TESTS_NOTE = "note";

   public static final String[] TESTS_COLUMNS = new String[]{TESTS_ID, TESTS_DATE, TESTS_TYPE, TESTS_PIGMENTATION, TESTS_PHOTO, TESTS_BRAND, TESTS_NOTE};

   public static final String[] DATA_COLUMNS = new String[]{TESTS_DATE, TESTS_PIGMENTATION};

   /**
    * Cycles table and its fields.
    */
   public static final String CYCLES_TABLE = "Cycles";
   public static final String CYCLES_ID = "_id";
   public static final String CYCLES_START_DATE = "start_date";

   public static final String[] CYCLES_COLUMNS = new String[]{CYCLES_ID, CYCLES_START_DATE};

   public static final String COUNT = "count";
   public static final String[] COUNT_COLUMNS = new String[]{"COUNT(*) as " + COUNT};

   /**
    * Test data structure.
    */
   static class Test implements Serializable
   {
      public long id;
      public long date;
      public int type;
      public int pigmentation;
      public String photo;
      public String brand;
      public String note;

      public String typeName()
      {
         switch(type)
         {
            case TYPES_TYPE_OVULATION:
               return "ovulation";
            case TYPES_TYPE_PREGNANCY:
               return "pregnancy";
            default:
               return "unknown";
         }
      }
   }


   /**
    * Removes the time component of a date.
    */
   static long clearDate(long date)
   {
      GregorianCalendar cal = new GregorianCalendar();
      cal.setTimeInMillis(date);
      cal.set(GregorianCalendar.HOUR_OF_DAY, 0);
      cal.set(GregorianCalendar.MINUTE, 0);
      cal.set(GregorianCalendar.SECOND, 0);
      cal.set(GregorianCalendar.MILLISECOND, 0);
      return cal.getTimeInMillis();
   }


   /**
    * Computes the interpretation text id.
    *
    * @return Interpretation text id.
    */
   static int interpret(Test test)
   {
      if(test.type == TYPES_TYPE_OVULATION)
      {
         if(test.pigmentation >= POSITIVE_OVULATION)
            return R.string.ovulating;
         else
            return R.string.not_ovulating;
      }
      else if(test.type == TYPES_TYPE_PREGNANCY)
      {
         if(test.pigmentation >= POSITIVE_PREGNANCY)
            return R.string.pregnant;
         else
            return R.string.not_pregnant;
      }
      return 0;
   }


   /**
    * Type of test, namely TYPES_TYPE_OVULATION or TYPES_TYPE_PREGNANCY.
    */
   int testType;


   /**
    * Prepares the database.
    */
   public TestsData(Context ctx)
   {
      super(ctx, DATABASE, null, VERSION);

      testType = TYPES_TYPE_OVULATION;
   }


   /**
    * Called when the database is created for the first time. This is where the
    * creation of tables and the initial population of the tables should happen.
    */
   @Override
   public void onCreate(SQLiteDatabase db)
   {
      db.execSQL("CREATE TABLE " + TYPES_TABLE + " (" + TYPES_TYPE + " INTEGER PRIMARY KEY NOT NULL)");

      db.execSQL("INSERT INTO " + TYPES_TABLE + "(" + TYPES_TYPE + ") VALUES (" + TYPES_TYPE_OVULATION + ")");
      db.execSQL("INSERT INTO " + TYPES_TABLE + "(" + TYPES_TYPE + ") VALUES (" + TYPES_TYPE_PREGNANCY + ")");

      db.execSQL("CREATE TABLE " + TESTS_TABLE + " (" + TESTS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," + TESTS_DATE + " INTEGER UNIQUE NOT NULL," + TESTS_TYPE + " INTEGER NOT NULL REFERENCES " + TYPES_TABLE + "(" + TYPES_TYPE + ")," + TESTS_PIGMENTATION + " INTEGER," + TESTS_PHOTO + " TEXT," + TESTS_BRAND + " TEXT," + TESTS_NOTE + " TEXT)");

      db.execSQL("CREATE INDEX " + TESTS_TABLE + "_" + TESTS_DATE + " ON " + TESTS_TABLE + "(" + TESTS_DATE + ")");

      db.execSQL("CREATE TABLE " + CYCLES_TABLE + " (" + CYCLES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," + CYCLES_START_DATE + " INTEGER UNIQUE NOT NULL)");

      db.execSQL("CREATE INDEX " + CYCLES_TABLE + "_" + CYCLES_START_DATE + " ON " + CYCLES_TABLE + "(" + CYCLES_START_DATE + ")");
   }

   /**
    * Called when the database needs to be upgraded. The implementation
    * should use this method to drop tables, add tables, or do anything else it
    * needs to upgrade to the new schema version.
    * This method executes within a transaction. If an exception is thrown, all changes
    * will automatically be rolled back.
    */
   @Override
   public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
   {
      if(oldVersion == 11 && newVersion == 12)
      {
         db.execSQL("ALTER TABLE " + TESTS_TABLE + " ADD COLUMN " + TESTS_BRAND + " TEXT");

         return;
      }

      throw new UnsupportedOperationException();

      /* Be careful with this, it's only for development.
      db.execSQL("DROP TABLE IF EXISTS " + CYCLES_TABLE);
      db.execSQL("DROP TABLE IF EXISTS " + TESTS_TABLE);
      db.execSQL("DROP TABLE IF EXISTS " + TYPES_TABLE);

      onCreate(db);
      */
   }

   /**
    * Sets the application type of test to Ovulation.
    */
   public void setOvulation()
   {
      testType = TYPES_TYPE_OVULATION;
   }

   /**
    * Sets the application type of test to Pregnancy.
    */
   public void setPregnancy()
   {
      testType = TYPES_TYPE_PREGNANCY;
   }

   /**
    * Type of test, namely TYPES_TYPE_OVULATION or TYPES_TYPE_PREGNANCY.
    */
   public int getTestType()
   {
      return testType;
   }

   /**
    * Sets the type of test, namely TYPES_TYPE_OVULATION or TYPES_TYPE_PREGNANCY.
    */
   public void setTestType(int type)
   {
      testType = type;
   }


   /**
    * Returns a cursor to the list of cycles.
    */
   public Cursor listCycles()
   {
      SQLiteDatabase db = getReadableDatabase();

      return db.query(CYCLES_TABLE, CYCLES_COLUMNS, null, null, null, null, CYCLES_START_DATE + " DESC");
   }

   /**
    * Returns the total count of cycles.
    */
   public int countCycles()
   {
      SQLiteDatabase db = getReadableDatabase();

      Cursor cursor = db.query(CYCLES_TABLE, COUNT_COLUMNS, null, null, null, null, null);

      cursor.moveToFirst();
      return cursor.getInt(cursor.getColumnIndexOrThrow(TestsData.COUNT));
   }

   /**
    * Creates a new cycle on the specified date.
    */
   void createCycle(long date)
   {
      SQLiteDatabase db = getWritableDatabase();

      ContentValues values = new ContentValues();
      values.put(CYCLES_START_DATE, clearDate(date));

      db.insert(CYCLES_TABLE, null, values);
   }

   /**
    * Saves a new starting date for a cycle.
    */
   void saveCycle(long id, long date)
   {
      SQLiteDatabase db = getWritableDatabase();

      ContentValues values = new ContentValues();
      values.put(CYCLES_START_DATE, clearDate(date));

      try
      {
         db.update(CYCLES_TABLE, values, CYCLES_ID + " = " + id, null);
      }
      catch(Exception e)
      {
         Log.e(TAG, "saveCycle error.", e);
      }
   }

   /**
    * Returns the start date of the cycle or -1 if not found.
    *
    * @param id Cycle id.
    */
   public long loadCycleStart(long id)
   {
      SQLiteDatabase db = getReadableDatabase();

      Cursor cursor = db.query(CYCLES_TABLE, CYCLES_COLUMNS, CYCLES_ID + " = " + id, null, null, null, null);
      if(!cursor.moveToFirst())
         return -1;

      return cursor.getLong(cursor.getColumnIndexOrThrow(TestsData.CYCLES_START_DATE));
   }

   /**
    * Returns the end date (excluding one day) of the cycle or -1 if not found (last cycle).
    * It actually returns the starting date of the next cycle.
    */
   public long loadCycleEnd(long start)
   {
      SQLiteDatabase db = getReadableDatabase();

      Cursor cursor = db.query(CYCLES_TABLE, CYCLES_COLUMNS, CYCLES_START_DATE + " > " + start, null, null, null, CYCLES_START_DATE + " ASC", "1");
      if(!cursor.moveToFirst())
         return -1;

      return cursor.getLong(cursor.getColumnIndexOrThrow(TestsData.CYCLES_START_DATE));
   }

   /**
    * Returns the date of the cycle previous to the given date or -1 if not found (first cycle).
    */
   public long loadCyclePrevious(long start)
   {
      SQLiteDatabase db = getReadableDatabase();

      Cursor cursor = db.query(CYCLES_TABLE, CYCLES_COLUMNS, CYCLES_START_DATE + " < " + start, null, null, null, CYCLES_START_DATE + " DESC", "1");
      if(!cursor.moveToFirst())
         return -1;

      return cursor.getLong(cursor.getColumnIndexOrThrow(TestsData.CYCLES_START_DATE));
   }

   /**
    * Returns the date of the cycle of the specified test or -1 if not found (before first cycle).
    */
   public long loadCycleOfTest(long test_date)
   {
      SQLiteDatabase db = getReadableDatabase();

      Cursor cursor = db.query(CYCLES_TABLE, CYCLES_COLUMNS, CYCLES_START_DATE + " <= " + test_date, null, null, null, CYCLES_START_DATE + " DESC", "1");
      if(!cursor.moveToFirst())
         return -1;

      return cursor.getLong(cursor.getColumnIndexOrThrow(TestsData.CYCLES_START_DATE));
   }

   /**
    * Checks if the specified cycle is the first one.
    *
    * @return true if it's the first and false if there is at least on cycle older.
    * On error returns false.
    */
   public boolean isFirstCycle(long id)
   {
      SQLiteDatabase db = getReadableDatabase();

      // Get starting date
      Cursor cursor = db.query(CYCLES_TABLE, CYCLES_COLUMNS, CYCLES_ID + " = " + id, null, null, null, null);
      if(!cursor.moveToFirst())
         return false;
      long start = cursor.getLong(cursor.getColumnIndexOrThrow(TestsData.CYCLES_START_DATE));

      // Find other previous
      cursor = db.query(CYCLES_TABLE, CYCLES_COLUMNS, CYCLES_START_DATE + " < " + start, null, null, null, CYCLES_START_DATE + " DESC", "1");
      if(!cursor.moveToFirst())
         return true;
      return false;
   }

   /**
    * Removes a cycle permanently.
    */
   public void removeCycle(long id)
   {
      SQLiteDatabase db = getWritableDatabase();
      db.delete(CYCLES_TABLE, CYCLES_ID + " = " + id, null);
   }

   /**
    * Removes a cycle permanently and all the tests contained within.
    */
   public void removeCycleWithTests(long id)
   {
      SQLiteDatabase db = getWritableDatabase();

      // Get start and end of the cycle
      Cursor cursor = db.query(CYCLES_TABLE, CYCLES_COLUMNS, CYCLES_ID + " = " + id, null, null, null, null);
      if(!cursor.moveToFirst())
         return;
      long cycle_start = cursor.getLong(cursor.getColumnIndexOrThrow(TestsData.CYCLES_START_DATE));

      cursor = db.query(CYCLES_TABLE, CYCLES_COLUMNS, CYCLES_START_DATE + " > " + cycle_start, null, null, null, CYCLES_START_DATE + " ASC", "1");
      long cycle_end = -1;
      if(cursor.moveToFirst())
         cycle_end = cursor.getLong(cursor.getColumnIndexOrThrow(TestsData.CYCLES_START_DATE));

      // Delete tests in this range
      db.delete(TESTS_TABLE, TESTS_DATE + " >= " + cycle_start + ((cycle_end > 0) ? " AND " + TESTS_DATE + " < " + cycle_end : ""), null);

      // Delete cycle itself
      db.delete(CYCLES_TABLE, CYCLES_ID + " = " + id, null);
   }


   /**
    * Returns a cursor with the list of tests of the current type.
    */
   public Cursor listTests()
   {
      SQLiteDatabase db = getReadableDatabase();

      return db.query(TESTS_TABLE, TESTS_COLUMNS, TESTS_TYPE + " = " + testType, null, null, null, TESTS_DATE + " DESC");
   }

   /**
    * Returns a cursor with the list of tests of the current type which belong to one cycle.
    *
    * @param end date or <= 0 for unset
    */
   public Cursor listData(long start, long end)
   {
      SQLiteDatabase db = getReadableDatabase();

      return db.query(TESTS_TABLE, DATA_COLUMNS, TESTS_TYPE + " = " + testType + " AND " + TESTS_DATE + " >= " + start + ((end <= 0) ? "" : " AND " + TESTS_DATE + " < " + end), null, null, null, TESTS_DATE + " ASC");
   }

   /**
    * Returns the total count of tests.
    */
   public int countTests()
   {
      SQLiteDatabase db = getReadableDatabase();

      Cursor cursor = db.query(TESTS_TABLE, COUNT_COLUMNS, null, null, null, null, null);

      cursor.moveToFirst();
      return cursor.getInt(cursor.getColumnIndexOrThrow(TestsData.COUNT));
   }

   /**
    * Returns the count of tests of one cycle.
    */
   public int countTestsOfCycle(long cycle_id)
   {
      SQLiteDatabase db = getReadableDatabase();

      // Get cycle start (and id)
      Cursor cursor = db.query(CYCLES_TABLE, CYCLES_COLUMNS, CYCLES_ID + " = " + cycle_id, null, null, null, null);
      if(!cursor.moveToFirst())
         return 0;
      long cycle_start = cursor.getLong(cursor.getColumnIndexOrThrow(TestsData.CYCLES_START_DATE));

      // Get load end if not last
      cursor = db.query(CYCLES_TABLE, CYCLES_COLUMNS, CYCLES_START_DATE + " > " + cycle_start, null, null, null, CYCLES_START_DATE + " ASC", "1");
      long cycle_end = -1;
      if(cursor.moveToFirst())
         cycle_end = cursor.getLong(cursor.getColumnIndexOrThrow(TestsData.CYCLES_START_DATE));

      // Count tests in cycle
      cursor = db.query(TESTS_TABLE, COUNT_COLUMNS, TESTS_DATE + " >= " + cycle_start + ((cycle_end <= 0) ? "" : " AND " + TESTS_DATE + " < " + cycle_end), null, null, null, null);
      cursor.moveToFirst();
      return cursor.getInt(cursor.getColumnIndexOrThrow(TestsData.COUNT));
   }

   /**
    * Returns the date of the first test or -1 if there is none.
    */
   public long firstTest()
   {
      SQLiteDatabase db = getReadableDatabase();

      Cursor cursor = db.query(TESTS_TABLE, new String[]{TESTS_DATE}, null, null, null, null, TESTS_DATE + " ASC", "1");
      if(!cursor.moveToFirst())
         return -1;

      return cursor.getLong(cursor.getColumnIndexOrThrow(TestsData.TESTS_DATE));
   }

   /**
    * Loads a test from the database.
    */
   public Test loadTest(long id)
   {
      SQLiteDatabase db = getReadableDatabase();

      Cursor cursor = db.query(TESTS_TABLE, TESTS_COLUMNS, TESTS_ID + " = " + id, null, null, null, null);
      if(!cursor.moveToFirst())
         return null;

      Test test = new Test();
      test.id = id;
      test.date = cursor.getLong(cursor.getColumnIndexOrThrow(TestsData.TESTS_DATE));
      test.type = cursor.getInt(cursor.getColumnIndexOrThrow(TestsData.TESTS_TYPE));
      test.pigmentation = cursor.getInt(cursor.getColumnIndexOrThrow(TestsData.TESTS_PIGMENTATION));
      test.photo = cursor.getString(cursor.getColumnIndexOrThrow(TestsData.TESTS_PHOTO));
      test.brand = cursor.getString(cursor.getColumnIndexOrThrow(TestsData.TESTS_BRAND));
      test.note = cursor.getString(cursor.getColumnIndexOrThrow(TestsData.TESTS_NOTE));
      return test;
   }

   /**
    * Removes a test permanently.
    */
   public void removeTest(long id)
   {
      SQLiteDatabase db = getWritableDatabase();

      db.delete(TESTS_TABLE, TESTS_ID + " = " + id, null);
   }

   /**
    * Removes a test permanently and the cycle it belongs if the deletion makes it orphan.
    */
   public void removeTestAndCycleIfEmpty(long id)
   {
      SQLiteDatabase db = getWritableDatabase();

      // Get test date
      Cursor cursor = db.query(TESTS_TABLE, TESTS_COLUMNS, TESTS_ID + " = " + id, null, null, null, null);
      if(!cursor.moveToFirst())
         return;
      long test_date = cursor.getLong(cursor.getColumnIndexOrThrow(TestsData.TESTS_DATE));

      // Get cycle start (and id)
      cursor = db.query(CYCLES_TABLE, CYCLES_COLUMNS, CYCLES_START_DATE + " <= " + test_date, null, null, null, CYCLES_START_DATE + " DESC", "1");
      cursor.moveToFirst();
      long cycle_id = cursor.getLong(cursor.getColumnIndexOrThrow(TestsData.CYCLES_ID));
      long cycle_start = cursor.getLong(cursor.getColumnIndexOrThrow(TestsData.CYCLES_START_DATE));

      // Get load end if not last
      cursor = db.query(CYCLES_TABLE, CYCLES_COLUMNS, CYCLES_START_DATE + " > " + cycle_start, null, null, null, CYCLES_START_DATE + " ASC", "1");
      long cycle_end = -1;
      if(cursor.moveToFirst())
         cycle_end = cursor.getLong(cursor.getColumnIndexOrThrow(TestsData.CYCLES_START_DATE));

      // Delete test
      db.delete(TESTS_TABLE, TESTS_ID + " = " + id, null);

      // Count remaining tests in cycle
      cursor = db.query(TESTS_TABLE, COUNT_COLUMNS, TESTS_DATE + " >= " + cycle_start + ((cycle_end <= 0) ? "" : " AND " + TESTS_DATE + " < " + cycle_end), null, null, null, null);
      cursor.moveToFirst();
      if(cursor.getInt(cursor.getColumnIndexOrThrow(TestsData.COUNT)) == 0)
      {
         // Delete cycle if empty
         db.delete(CYCLES_TABLE, CYCLES_ID + " = " + cycle_id, null);
      }
   }

   /**
    * Stores a test into the database.
    * If the test does not exist (the id is less than 0), it creates a new entry and fills the id.
    *
    * @return true if created and false if updated.
    */
   public boolean saveTest(Test test)
   {
      SQLiteDatabase db = getWritableDatabase();

      ContentValues values = new ContentValues();
      values.put(TESTS_DATE, test.date);
      values.put(TESTS_TYPE, test.type);
      values.put(TESTS_BRAND, test.brand);
      values.put(TESTS_NOTE, test.note);

      if(test.id < 0)
      {
         values.put(TESTS_PIGMENTATION, test.pigmentation);
         values.put(TESTS_PHOTO, test.photo);
         test.id = db.insert(TESTS_TABLE, null, values);
         return true;
      }
      else
      {
         db.update(TESTS_TABLE, values, TESTS_ID + " = " + test.id, null);
         return false;
      }
   }

   /**
    * Formats a string to be exported as CSV
    */
   String exportString(String str)
   {
      if(str == null)
         return "";

      if(str.indexOf(',') < 0 && str.indexOf('"') < 0)
         return str;

      return "\"" + str.replace("\"", "\"\"") + "\"";
   }

   /**
    * Formats a the photo path to be exported as CSV
    */
   String exportPhoto(String str)
   {
      if(str == null)
         return "";

      int i = str.lastIndexOf('/');
      if(i >= 0)
         str = str.substring(i + 1);
      return exportString(str);
   }

   /**
    * Breaks a CSV line into (unescaped) fields.
    */
   String[] importFields(String line)
   {
      if(line == null || line.length() == 0)
         return null;

      List<String> fields = new ArrayList<>();
      StringBuffer sb = new StringBuffer();
      boolean field_start = true;
      boolean escaping = false;
      boolean prev_quote = false;
      for(int i = 0; i < line.length(); i++)
      {
         char c = line.charAt(i);

         if(field_start)
         {
            field_start = false;

            if(c == ',')
            {
               fields.add(sb.toString());
               sb.setLength(0);
               field_start = true;
               escaping = false;
            }
            else if(c == '"')
            {
               escaping = true;
               prev_quote = false;
            }
            else
            {
               sb.append(c);
            }
         }
         else if(escaping)
         {
            if(prev_quote)
            {
               prev_quote = false;

               if(c == '"')
               {
                  // Double quote
                  sb.append('"');
               }
               else if(c == ',')
               {
                  fields.add(sb.toString());
                  sb.setLength(0);
                  field_start = true;
                  escaping = false;
               }
               else
               {
                  // Relaxed input
                  sb.append('"');
                  sb.append(c);
               }
            }
            else
            {
               if(c == '"')
               {
                  prev_quote = true;
               }
               else
               {
                  sb.append(c);
               }
            }
         }
         else
         {
            if(c == ',')
            {
               fields.add(sb.toString());
               sb.setLength(0);
               field_start = true;
               escaping = false;
            }
            else
            {
               sb.append(c);
            }
         }
      }
      // An escaped field not finished is accepted anyway (relaxed input)
      fields.add(sb.toString());

      String[] raw_fields = new String[fields.size()];
      for(int i = 0; i < fields.size(); i++)
         raw_fields[i] = fields.get(i);
      return raw_fields;
   }

   /**
    * Exports all the data to a CSV file.
    *
    * @return true on success
    */
   public boolean exportData(File csv_file)
   {
      SQLiteDatabase db = getReadableDatabase();

      try(PrintWriter csv = new PrintWriter(new FileWriter(csv_file)))
      {
         // Tests
         try(Cursor cursor = db.query(TESTS_TABLE, TESTS_COLUMNS, null, null, null, null, TESTS_DATE + " DESC"))
         {
            csv.println(TESTS_DATE + "," + TESTS_TYPE + "," + TESTS_PIGMENTATION + "," + TESTS_PHOTO + "," + TESTS_BRAND + "," + TESTS_NOTE);

            while(cursor.moveToNext())
            {
               csv.println(cursor.getLong(cursor.getColumnIndexOrThrow(TESTS_DATE)) + "," + cursor.getInt(cursor.getColumnIndexOrThrow(TESTS_TYPE)) + "," + cursor.getInt(cursor.getColumnIndexOrThrow(TESTS_PIGMENTATION)) + "," + exportPhoto(cursor.getString(cursor.getColumnIndexOrThrow(TESTS_PHOTO))) + "," + exportString(cursor.getString(cursor.getColumnIndexOrThrow(TESTS_BRAND))) + "," + exportString(cursor.getString(cursor.getColumnIndexOrThrow(TESTS_NOTE))));
            }
         }

         csv.println();

         // Cycles
         try(Cursor cursor = db.query(CYCLES_TABLE, CYCLES_COLUMNS, null, null, null, null, CYCLES_START_DATE + " DESC"))
         {
            csv.println(CYCLES_START_DATE);

            while(cursor.moveToNext())
            {
               csv.println(cursor.getLong(cursor.getColumnIndexOrThrow(CYCLES_START_DATE)));
            }
         }
      }
      catch(IOException e)
      {
         Log.d(TAG, "Data exportation error.", e);
         return false;
      }
      return true;
   }

   /**
    * Imports all the data from a CSV file.
    *
    * @return true on success
    */
   public boolean importData(File media_dir, File csv_file)
   {
      //Log.d(TAG, "importData: " + csv_file.toString());

      SQLiteDatabase db = getWritableDatabase();

      try(BufferedReader csv = new BufferedReader(new FileReader(csv_file)))
      {
         String line = csv.readLine();
         if(line == null)
            throw new Exception("Unexpected end of file.");
         if(!line.equals(TESTS_DATE + "," + TESTS_TYPE + "," + TESTS_PIGMENTATION + "," + TESTS_PHOTO + "," + TESTS_BRAND + "," + TESTS_NOTE))
            throw new Exception("Invalid data header.");

         // Delete previous data
         db.delete(TESTS_TABLE, null, null);
         db.delete(CYCLES_TABLE, null, null);

         while(true)
         {
            line = csv.readLine();
            if(line == null)
               throw new Exception("Unexpected end of file.");
            if(line.length() == 0)
               break;

            String[] fields = importFields(line);
            if(fields == null || fields.length != 6)
               throw new Exception("Row processing error.");

            long date = Long.parseLong(fields[0]);
            int type = Integer.parseInt(fields[1]);
            int pigmentation = Integer.parseInt(fields[2]);
            String photo = fields[3];
            if(photo.length() == 0)
               photo = null;
            else
               photo = media_dir.toString() + "/" + photo;
            String brand = fields[4];
            if(brand.length() == 0)
               brand = null;
            String note = fields[5];
            if(note.length() == 0)
               note = null;

            ContentValues values = new ContentValues();
            values.put(TESTS_DATE, date);
            values.put(TESTS_TYPE, type);
            values.put(TESTS_PIGMENTATION, pigmentation);
            values.put(TESTS_PHOTO, photo);
            values.put(TESTS_BRAND, brand);
            values.put(TESTS_NOTE, note);
            db.insert(TESTS_TABLE, null, values);
         }

         line = csv.readLine();
         if(line == null)
            throw new Exception("Unexpected end of file.");
         if(!line.equals(CYCLES_START_DATE))
            throw new Exception("Invalid data second header.");

         while(true)
         {
            line = csv.readLine();
            if(line == null)
               break;

            long start_date = Long.parseLong(line);

            ContentValues values = new ContentValues();
            values.put(CYCLES_START_DATE, clearDate(start_date));
            db.insert(CYCLES_TABLE, null, values);
         }
      }
      catch(Exception e)
      {
         Log.d(TAG, "Data importation error.", e);
         return false;
      }

      return true;
   }
}
