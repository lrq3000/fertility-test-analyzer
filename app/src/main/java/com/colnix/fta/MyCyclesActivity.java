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

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity that shows a list of tests to the user.
 */
public class MyCyclesActivity extends NavigationActivity
{
   static final String TAG = "MyCyclesActivity";

   static final int REQUEST_EDITCYCLE = 1;

   static final String CURRENT_DIALOG = "current_dialog";


   /**
    * List data adapter.
    */
   protected TestsAdapter adapter;

   /**
    * Says if a list refresh is needed.
    */
   protected boolean needsRefresh;

   /**
    * Ovulation type button.
    */
   protected RadioButton buttonTypeOvul;

   /**
    * Pregnancy type button.
    */
   protected RadioButton buttonTypePreg;

   /**
    * Pigment percent text box or null if no preview.
    */
   protected TextView pigmentText;

   /**
    * Test interpretation text box or null if no preview.
    */
   protected TextView interpretText;

   /**
    * Test brand text box or null if no preview.
    */
   protected TextView brandText;

   /**
    * Test note text box or null if no preview.
    */
   protected TextView noteText;

   /**
    * Dialog displayed.
    */
   enum DialogId
   {
      DLG_NONE, DLG_LIKE, DLG_RATE_STAR, DLG_RATE_BABY, DLG_FEEDBACK
   }

   /**
    * Current dialog displayed.
    */
   DialogId currentDialog;

   /**
    * Activity input state.
    */
   Bundle inState;


   /**
    * Called when the activity is starting. This is where most initialization should go: calling
    * setContentView(int) to inflate the activity's UI, using findViewById(int) to programmatically
    * interact with widgets in the UI
    */
   @Override
   protected void onCreate(Bundle state)
   {
      super.onCreate(state);
      setContentView(R.layout.activity_my_cycles);
      inState = state;

      ListView list = (ListView) findViewById(R.id.list_my_tests);
      //list.setHeaderDividersEnabled(false);

      adapter = new TestsAdapter(this);
      list.setAdapter(adapter);

      installTypeButtons();

      View btn = findViewById(R.id.btn_add);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            Intent intent = new Intent(MyCyclesActivity.this, NewTestActivity.class);
            startActivity(intent);
         }
      });

      pigmentText = (TextView) findViewById(R.id.pigment);
      interpretText = (TextView) findViewById(R.id.interpret);
      brandText = (TextView) findViewById(R.id.brand);
      noteText = (TextView) findViewById(R.id.note);

      btn = findViewById(R.id.btn_edit);
      if(btn != null)
      {
         btn.setOnClickListener(new View.OnClickListener()
         {
            @Override
            public void onClick(View v)
            {
               Intent intent = new Intent(MyCyclesActivity.this, TestEditionActivity.class);
               intent.putExtra(TestEditionActivity.ID, adapter.getSelectedId());
               startActivity(intent);
            }
         });
         btn.setEnabled(false);
      }

      needsRefresh = true;
      currentDialog = DialogId.DLG_NONE;
      if(state != null)
      {
         currentDialog = DialogId.valueOf(state.getString(CURRENT_DIALOG, DialogId.DLG_NONE.name()));
      }
      init();
   }

   @Override
   protected void onDestroy()
   {
      super.onDestroy();
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

      outState.putString(CURRENT_DIALOG, currentDialog.name());
   }

   /**
    * Updates the type buttons state.
    */
   protected void updateType()
   {
      if(data == null)
         return;

      if(data.getTestType() == TestsData.TYPES_TYPE_OVULATION)
      {
         buttonTypeOvul.setChecked(true);
         buttonTypePreg.setChecked(false);
      }
      else
      {
         buttonTypeOvul.setChecked(false);
         buttonTypePreg.setChecked(true);
      }
   }

   /**
    * Install the type buttons functionality.
    */
   protected void installTypeButtons()
   {
      buttonTypeOvul = (RadioButton) findViewById(R.id.btn_type_ovulation);
      buttonTypePreg = (RadioButton) findViewById(R.id.btn_type_pregnancy);

      buttonTypeOvul.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            if(data == null)
            {
               buttonTypeOvul.setChecked(false);
            }
            else
            {
               buttonTypeOvul.setChecked(true);
               buttonTypePreg.setChecked(false);

               data.setOvulation();
               refreshList();
            }
         }
      });

      buttonTypePreg.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            if(data == null)
            {
               buttonTypePreg.setChecked(false);
            }
            else
            {
               buttonTypeOvul.setChecked(false);
               buttonTypePreg.setChecked(true);

               data.setPregnancy();
               refreshList();
            }
         }
      });
   }

   /**
    * Called just before the activity starts interacting with the user. At this point the activity
    * is at the top of the activity stack, with user input going to it.
    */
   @Override
   protected void onResume()
   {
      super.onResume();

      if(data == null)
         return;

      if(needsRefresh)
      {
         updateType();
         refreshList();
      }
   }

   /**
    * Called when the system is about to start resuming another activity. This method is typically
    * used to commit unsaved changes to persistent data, stop animations and other things that may
    * be consuming CPU, and so on. It should do whatever it does very quickly, because the next
    * activity will not be resumed until it returns.
    */
   @Override
   protected void onPause()
   {
      super.onPause();

      needsRefresh = true;
      adapter.changeCursors(null, null);
   }

   /**
    * Event invoked when the data is ready.
    */
   @Override
   protected void onServicesReady()
   {
      super.onServicesReady();

      updateType();
      if(pigmentText != null)
         adapter.setSelected(0);
      refreshList();
   }

   /**
    * Sets the preview data.
    */
   protected void setPreview(long id)
   {
      if(id >= 0)
      {
         TestsData.Test test = data.loadTest(id);
         pigmentText.setText(Integer.toString(test.pigmentation) + "%");
         if(interpretText != null)
            interpretText.setText(TestsData.interpret(test));
         if(brandText != null)
            brandText.setText(test.brand);
         if(noteText != null)
            noteText.setText(test.note);

         View btn = findViewById(R.id.btn_edit);
         btn.setEnabled(true);
      }
      else
      {
         pigmentText.setText("0%");
         if(interpretText != null)
            interpretText.setText("");
         if(brandText != null)
            brandText.setText("");
         if(noteText != null)
            noteText.setText("");

         View btn = findViewById(R.id.btn_edit);
         btn.setEnabled(false);
      }
   }

   /**
    * Re-queries the list of cycles and updates the list.
    */
   protected void refreshList()
   {
      if(data == null)
         return;

      refreshNavBar();

      Cursor tests = data.listTests();
      Cursor cycles = data.listCycles();
      adapter.changeCursors(tests, cycles);

      long id = adapter.getSelectedId();
      if(pigmentText != null)
         setPreview(id);

      needsRefresh = false;
   }

   /**
    * Event of a cycle item selected.
    */
   public void onCycleSelected(View view, long id)
   {
      long start_date = data.loadCycleStart(id);
      if(start_date < 0)
         return;

      Intent intent = new Intent(this, CycleEditionActivity.class);
      intent.putExtra(CycleEditionActivity.DATA_ID, id);
      intent.putExtra(CycleEditionActivity.DATA_DATE, start_date);

      String explain = "";
      long min_date = data.loadCyclePrevious(start_date);
      if(min_date > 0)
      {
         min_date = TestsData.clearDate(min_date + 1000 * 60 * 60 * 24);
         intent.putExtra(CycleEditionActivity.DATA_MIN_DATE, min_date);
         explain = getString(R.string.mcycles_expl1);
      }

      long max_date_test = -1;
      if(min_date <= 0)    // Editing the first cycle
      {
         max_date_test = data.firstTest();
         if(max_date_test > 0)
            max_date_test = TestsData.clearDate(data.firstTest());
      }
      long max_date_cycle = data.loadCycleEnd(start_date);

      if(max_date_test > 0 && max_date_cycle > 0 && max_date_test > max_date_cycle)
         max_date_test = -1;  // This cycle is the first but is empty, so the limit is defined by the next.

      long max_date = -1;
      if(max_date_test > 0)
      {
         max_date = max_date_test;
         intent.putExtra(CycleEditionActivity.DATA_MAX_DATE, max_date);
         explain += getString(R.string.mcycles_expl2);
      }
      else if(max_date_cycle > 0)
      {
         max_date = TestsData.clearDate(max_date_cycle - 1000 * 60 * 60 * 24);
         intent.putExtra(CycleEditionActivity.DATA_MAX_DATE, max_date);
         explain += getString(R.string.mcycles_expl3);
      }
      intent.putExtra(CycleEditionActivity.DATA_EXPLANATION, explain);

      if(min_date > 0 && max_date > 0 && min_date > max_date)
      {
         AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FtaAlertDialogStyle);
         builder.setTitle(getString(R.string.mcycles_expl4));
         builder.setIcon(R.drawable.ico_warning_grey);
         builder.setMessage(getString(R.string.mcycles_expl5));
         builder.setPositiveButton(R.string.mcycles_expl5, null);
         builder.create().show();
         return;
      }

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

      if(requestCode == REQUEST_EDITCYCLE && resultCode == RESULT_OK)
      {
         long id = intent.getLongExtra(CycleEditionActivity.DATA_ID, -1);
         long date = intent.getLongExtra(CycleEditionActivity.DATA_DATE, -1);
         if(id < 0 || date < 0)
            return;

         data.saveCycle(id, date);
         refreshList();
      }
   }

   /**
    * Event received when the button of seeing cycle details is clicked.
    */
   public void onCycleDetailsClick(final long id)
   {
      Intent intent = new Intent(this, ChartsActivity.class);
      intent.putExtra(ChartsActivity.ID, id);
      startActivity(intent);
   }

   /**
    * Event received when the button of removing a cycle is clicked.
    */
   public void onCycleRemoveClick(final long id)
   {
      if(data == null)
         return;

      if(data.countTestsOfCycle(id) == 0)
      {
         AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FtaAlertDialogStyle);
         builder.setTitle(getString(R.string.delete_cycle_title));
         builder.setIcon(R.drawable.ico_warning_grey);
         builder.setMessage(getString(R.string.delete_empty_cycle_message));
         builder.setNeutralButton(getString(R.string.delete_no), null);
         builder.setPositiveButton(getString(R.string.delete_yes), new DialogInterface.OnClickListener()
         {
            public void onClick(DialogInterface dialog, int i)
            {
               data.removeCycle(id);
               refreshList();
            }
         });
         builder.create().show();
         return;
      }

      if(data.isFirstCycle(id))     // We cannot merge tests if it's the first cycle.
      {
         AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FtaAlertDialogStyle);
         builder.setTitle(getString(R.string.delete_cycle_title));
         builder.setIcon(R.drawable.ico_warning_grey);
         builder.setMessage(getString(R.string.delete_first_cycle_message));
         builder.setNeutralButton(getString(R.string.delete_no), null);
         builder.setPositiveButton(getString(R.string.delete_all), new DialogInterface.OnClickListener()
         {
            public void onClick(DialogInterface dialog, int i)
            {
               data.removeCycleWithTests(id);
               refreshList();
            }
         });
         builder.create().show();
         return;
      }

      AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FtaAlertDialogStyle);
      builder.setTitle(getString(R.string.delete_cycle_title));
      builder.setIcon(R.drawable.ico_warning_grey);
      builder.setMessage(getString(R.string.delete_cycle_message));
      builder.setNeutralButton(getString(R.string.delete_no), null);
      builder.setNegativeButton(getString(R.string.delete_merge), new DialogInterface.OnClickListener()
      {
         @Override
         public void onClick(DialogInterface dialogInterface, int i)
         {
            data.removeCycle(id);
            refreshList();
         }
      });
      builder.setPositiveButton(getString(R.string.delete_all), new DialogInterface.OnClickListener()
      {
         public void onClick(DialogInterface dialog, int i)
         {
            data.removeCycleWithTests(id);
            refreshList();
         }
      });
      builder.create().show();
   }

   /**
    * Event of a test item selected.
    */
   public void onTestSelected(View view, long id, int position)
   {
      if(pigmentText == null)
      {
         Intent intent = new Intent(this, TestEditionActivity.class);
         intent.putExtra(TestEditionActivity.ID, id);
         startActivity(intent);
      }
      else
      {
         adapter.setSelected(position);
         adapter.notifyDataSetChanged();

         setPreview(id);
      }
   }

   /**
    * Event of a test item remove button clicked.
    */
   public void onTestRemoveClicked(long id)
   {
      if(data == null)
         return;

      final long test_id = id;

      AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FtaAlertDialogStyle);
      builder.setTitle(getString(R.string.delete_test_title));
      builder.setIcon(R.drawable.ico_warning_grey);
      builder.setMessage(getString(R.string.delete_test_message));
      builder.setNegativeButton(getString(R.string.delete_no), null);
      builder.setPositiveButton(getString(R.string.delete_yes), new DialogInterface.OnClickListener()
      {
         public void onClick(DialogInterface dialog, int id)
         {

            TestsData.Test test = data.loadTest(test_id);
            if(test != null)
            {
               data.removeTest(test_id);
               Toast.makeText(getApplicationContext(), getString(R.string.mcycles_deleted), Toast.LENGTH_SHORT).show();
            }

            refreshList();
         }
      });

      AlertDialog dialog = builder.create();
      dialog.show();
   }
}
