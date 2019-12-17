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

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Test data creation or edition activity.
 */
public class TestEditionActivity extends ServicesActivity
{
   /* Intent parameter */
   public static final String ID = "id";
   public static final String PIGMENTATION = "pigmentation";
   public static final String FILE = "file";
   public static final String NOTE = "note";

   static final int REQUEST_SELECTDATE = 1;
   static final int REQUEST_EDITCYCLE = 2;


   /* State parameter */
   static final String CURRENT_TEST = "current_test";
   static final String CYCLE_AUTO = "cycle_auto";
   static final String CYCLE_NEW = "cycle_new";
   static final String CYCLE_DATE = "cycle_date";

   /**
    * Test data.
    */
   TestsData.Test test;

   /**
    * Whether if the cycle is calculated automatically.
    */
   boolean cycleAuto;

   /**
    * Whether if a new cycle should be created or not.
    */
   boolean cycleNew;

   /**
    * Date of the test cycle.
    */
   long cycleDate;

   /**
    * Ovulation type button.
    */
   RadioButton buttonTypeOvul;

   /**
    * Pregnancy type button.
    */
   RadioButton buttonTypePreg;

   /**
    * Test date formatter.
    */
   SimpleDateFormat testDateFormatter;

   /**
    * Cycle date formatter.
    */
   SimpleDateFormat cycleDateFormatter;

   /**
    * Instance state or null if none.
    */
   Bundle inState;


   /**
    * Called when the activity is starting. This is where most initialization should go: calling
    * setContentView(int) to inflate the activity's UI, using findViewById(int) to programmatically
    * interact with widgets in the UI
    */
   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_test_edition);
      inState = savedInstanceState;

      Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);

      ActionBar bar = getSupportActionBar();
      assert bar != null;
      bar.setDisplayShowTitleEnabled(false);
      bar.setDisplayHomeAsUpEnabled(true);

      installTypeButtons();
      testDateFormatter = new SimpleDateFormat("MMM dd, yyyy - HH:mm");
      cycleDateFormatter = new SimpleDateFormat("MMM dd, yyyy");


      if(inState != null)
      {
         test = (TestsData.Test) inState.getSerializable(CURRENT_TEST);
         cycleAuto = inState.getBoolean(CYCLE_AUTO);
         cycleNew = inState.getBoolean(CYCLE_NEW);
         cycleDate = inState.getLong(CYCLE_DATE);
      }
      else
      {
         test = new TestsData.Test();
         Intent intent = getIntent();
         test.id = intent.getLongExtra(ID, -1);
         if(test.id < 0)
         {
            test.date = System.currentTimeMillis();
            setType(TestsData.TYPES_TYPE_OVULATION);
            test.pigmentation = intent.getIntExtra(PIGMENTATION, 0);
            test.photo = intent.getStringExtra(FILE);
            test.note = intent.getStringExtra(NOTE);
         }

         cycleAuto = true;
         cycleNew = false;
      }

      // Most of the interface has to be updated when the data becomes available.
      init();

      ImageButton img_btn = (ImageButton) findViewById(R.id.btn_brand_rem);
      img_btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            EditText edit = (EditText) findViewById(R.id.edit_brand);
            edit.setText("");
         }
      });

      Button btn = (Button) findViewById(R.id.btn_test_date);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            Intent intent = new Intent(TestEditionActivity.this, DateSelectionActivity.class);
            intent.putExtra(DateSelectionActivity.DATA_DATE, test.date);
            startActivityForResult(intent, REQUEST_SELECTDATE);
         }
      });

      btn = (Button) findViewById(R.id.btn_cycle_date);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            editCycle();
         }
      });

      img_btn = (ImageButton) findViewById(R.id.btn_cycle_reassign);
      img_btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View view)
         {
            calculateCycleAuto();
            updateInterface(true);
         }
      });

      img_btn = (ImageButton) findViewById(R.id.btn_note_rem);
      img_btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            EditText edit = (EditText) findViewById(R.id.edit_note);
            edit.setText("");
         }
      });

      FloatingActionButton done = (FloatingActionButton) findViewById(R.id.btn_done);
      done.setEnabled(false);
      done.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            if(save())
            {
               setResult(RESULT_OK);
               finish();
            }
         }
      });

      btn = (Button) findViewById(R.id.btn_open_img);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View view)
         {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = FileProvider.getUriForFile(TestEditionActivity.this, getApplicationContext().getPackageName() + ".provider", new File(test.photo));
            intent.setDataAndType(uri, "image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
         }
      });
   }

   @Override
   protected void onDestroy()
   {
      super.onDestroy();
   }

   /**
    * Called just before the activity starts interacting with the user. At this point the activity
    * is at the top of the activity stack, with user input going to it.
    */
   @Override
   protected void onResume()
   {
      super.onResume();
   }

   /**
    * Called to retrieve per-instance state from an activity before being killed so that the state
    * can be restored in onCreate(Bundle) or onRestoreInstanceState(Bundle) (the Bundle populated
    * by this method will be passed to both).
    */
   @Override
   protected void onSaveInstanceState(Bundle outState)
   {
      super.onSaveInstanceState(outState);

      outState.putSerializable(CURRENT_TEST, test);
      outState.putBoolean(CYCLE_AUTO, cycleAuto);
      outState.putBoolean(CYCLE_NEW, cycleNew);
      outState.putLong(CYCLE_DATE, cycleDate);
   }

   /**
    * Event invoked when the data is ready.
    */
   @Override
   protected void onServicesReady()
   {
      if(inState != null)
      {
         updateInterface(true);
      }
      else
      {
         if(test.id >= 0)
            test = data.loadTest(test.id);

         calculateCycleAuto();

         updateInterface(false);
      }
   }

   /**
    * Install the type buttons functionality.
    */
   void installTypeButtons()
   {
      buttonTypeOvul = (RadioButton) findViewById(R.id.btn_type_ovulation);
      buttonTypePreg = (RadioButton) findViewById(R.id.btn_type_pregnancy);

      buttonTypeOvul.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            setType(TestsData.TYPES_TYPE_OVULATION);
         }
      });

      buttonTypePreg.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            setType(TestsData.TYPES_TYPE_PREGNANCY);
         }
      });
   }

   /**
    * This hook is called whenever an item in your options menu is selected.
    *
    * @return false to allow normal menu processing to proceed, true to consume it here.
    */
   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      setResult(RESULT_CANCELED);
      finish();
      return true;
   }

   /**
    * Sets the test type and its interpretation.
    */
   void setType(int type)
   {
      test.type = type;

      FloatingActionButton done = (FloatingActionButton) findViewById(R.id.btn_done);
      EditText edit = (EditText) findViewById(R.id.edit_brand);
      if(type == TestsData.TYPES_TYPE_OVULATION)
      {
         buttonTypeOvul.setChecked(true);
         buttonTypePreg.setChecked(false);
         setInterpret(TestsData.interpret(test));
         done.setEnabled(true);
         if(test.id < 0 && edit.getText().length() == 0)
         {
            SharedPreferences prefs = Config.getPrefs(this);
            edit.setText(prefs.getString(Config.PREF_DEFAULT_OVULATION_BRAND, ""));
         }
      }
      else if(type == TestsData.TYPES_TYPE_PREGNANCY)
      {
         buttonTypeOvul.setChecked(false);
         buttonTypePreg.setChecked(true);
         setInterpret(TestsData.interpret(test));
         done.setEnabled(true);
         if(test.id < 0 && edit.getText().length() == 0)
         {
            SharedPreferences prefs = Config.getPrefs(this);
            edit.setText(prefs.getString(Config.PREF_DEFAULT_PREGNANCY_BRAND, ""));
         }
      }
      else
      {
         setInterpret(0);
         done.setEnabled(false);
      }
   }

   /**
    * Sets the interpretation text.
    *
    * @param text Interpretation id or 0 to hide the message.
    */
   void setInterpret(int text)
   {
      View interp = findViewById(R.id.panel_interpret);
      if(text > 0)
      {
         interp.setVisibility(View.VISIBLE);

         TextView interpret = (TextView) findViewById(R.id.text_interpret);
         interpret.setText(text);
      }
      else
      {
         interp.setVisibility(View.GONE);
      }
   }

   /**
    * Updates the interface with the test and cycle data.
    */
   void updateInterface(boolean in_edition)
   {
      TextView txt = (TextView) findViewById(R.id.pigment_percent);
      txt.setText(test.pigmentation + "%");

      setType(test.type);

      Button btn = (Button) findViewById(R.id.btn_test_date);
      btn.setText(testDateFormatter.format(new Date(test.date)));

      if(!in_edition)
      {
         EditText edit = (EditText) findViewById(R.id.edit_brand);
         if(test.brand != null)
            edit.setText(test.brand);
         else
            edit.setText("");

         edit = (EditText) findViewById(R.id.edit_note);
         if(test.note != null)
            edit.setText(test.note);
         else
            edit.setText("");
      }

      btn = (Button) findViewById(R.id.btn_open_img);
      if(test.photo != null)
      {
         File f = new File(test.photo);
         if(f.exists())
            btn.setEnabled(true);
         else
            btn.setEnabled(false);
      }
      else
         btn.setEnabled(false);

      txt = (TextView) findViewById(R.id.text_cycle);
      if(cycleNew)
         txt.setText(R.string.text_new_cycle);
      else
         txt.setText(R.string.text_cycle);

      btn = (Button) findViewById(R.id.btn_cycle_date);
      btn.setText(cycleDateFormatter.format(new Date(cycleDate)));

      ImageButton img_btn = (ImageButton) findViewById(R.id.btn_cycle_reassign);
      if(cycleAuto)
         img_btn.setVisibility(View.INVISIBLE);
      else
         img_btn.setVisibility(View.VISIBLE);

      btn = (Button) findViewById(R.id.btn_open_img);
      btn.setEnabled(test.photo != null);
   }

   /**
    * Calculates the cycle automatically.
    */
   void calculateCycleAuto()
   {
      cycleAuto = true;

      cycleDate = data.loadCycleOfTest(test.date);
      if(cycleDate > 0)
      {
         cycleNew = false;
      }
      else
      {
         cycleNew = true;
         cycleDate = TestsData.clearDate(test.date);
      }
   }

   /**
    * Brings up the cycle edition activity.
    */
   void editCycle()
   {
      Intent intent = new Intent(TestEditionActivity.this, CycleEditionActivity.class);

      String explain = "";
      long current = cycleDate;
      long min_date = data.loadCycleOfTest(test.date);
      if(min_date > 0)
      {
         min_date = TestsData.clearDate(min_date + 1000 * 60 * 60 * 24);
         if(min_date > TestsData.clearDate(test.date))
         {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FtaAlertDialogStyle);
            builder.setTitle(getString(R.string.mcycles_expl4));
            builder.setIcon(R.drawable.ico_warning_grey);
            builder.setMessage(getString(R.string.ted_expl1));
            // builder.setMessage("Sorry, there is no room for a new cycle. This test is just at the beginning of another cycle.");
            builder.setPositiveButton("Ok", null);
            builder.create().show();
            return;
         }

         intent.putExtra(CycleEditionActivity.DATA_MIN_DATE, min_date);
         if(current < min_date)
            current = test.date;
         explain = getString(R.string.ted_expl2);
      }

      intent.putExtra(CycleEditionActivity.DATA_MAX_DATE, test.date);
      explain += getString(R.string.ted_expl3);

      intent.putExtra(CycleEditionActivity.DATA_EXPLANATION, explain);
      intent.putExtra(CycleEditionActivity.DATA_DATE, current);

      startActivityForResult(intent, REQUEST_EDITCYCLE);
   }

   /**
    * Called when an activity you launched exits, giving you the requestCode you started it with,
    * the resultCode it returned, and any additional data from it.
    * You will receive this call immediately before onResume() when your activity is re-starting.
    */
   @Override
   public void onActivityResult(int requestCode, int resultCode, Intent intent)
   {
      super.onActivityResult(requestCode, resultCode, intent);

      if(requestCode == REQUEST_SELECTDATE && resultCode == RESULT_OK)
      {
         long date = intent.getLongExtra(DateSelectionActivity.DATA_DATE, 0L);
         if(date <= 0)
            return;
         test.date = date;

         long end = data.loadCycleEnd(cycleDate);
         if(test.date < cycleDate || (end > 0 && TestsData.clearDate(test.date) >= end))
            calculateCycleAuto();

         updateInterface(true);
      }
      else if(requestCode == REQUEST_EDITCYCLE && resultCode == RESULT_OK)
      {
         long date = intent.getLongExtra(CycleEditionActivity.DATA_DATE, 0L);
         if(date <= 0)
            return;

         cycleAuto = false;
         cycleNew = true;
         cycleDate = date;

         updateInterface(true);
      }
   }

   /**
    * Saves the test details.
    *
    * @return true on success.
    */
   boolean save()
   {
      if(data == null)
         return false;

      if(test.type < 0)
         return false;

      EditText edit = (EditText) findViewById(R.id.edit_brand);
      test.brand = edit.getText().toString();
      if(test.brand.isEmpty())
         test.brand = null;

      edit = (EditText) findViewById(R.id.edit_note);
      test.note = edit.getText().toString();
      if(test.note.isEmpty())
         test.note = null;

      data.saveTest(test);

      data.setTestType(test.type);

      if(cycleNew)
      {
         data.createCycle(cycleDate);
      }

      return true;
   }
}
