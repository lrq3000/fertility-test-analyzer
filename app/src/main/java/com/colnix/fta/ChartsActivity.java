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
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.RadioButton;

import java.util.ArrayList;

/**
 * Activity that shows a list of cycles to the user and draws a chart with the evolution of the
 * pigmentation in time.
 */
public class ChartsActivity extends NavigationActivity
{
   /* Intent parameter */
   public static final String ID = "id";

   /* State parameter */
   static final String CYCLE_IDS = "ids";


   /**
    * List data adapter.
    */
   protected CursorAdapter adapter;

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
    * Cycle(s) chart.
    */
   protected CycleChartView chart;

   /**
    * Instance state or null if none.
    */
   protected Bundle inState;


   /**
    * Called when the activity is starting. This is where most initialization should go: calling
    * setContentView(int) to inflate the activity's UI, using findViewById(int) to programmatically
    * interact with widgets in the UI
    */
   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(inState);
      inState = savedInstanceState;
      setContentView(R.layout.activity_charts);

      ListView list = (ListView) findViewById(R.id.list_my_cycles);
      //list.setHeaderDividersEnabled(false);
      
      chart = (CycleChartView) findViewById(R.id.chart_cycle);
      if(chart == null)
      {
         chart = (CycleChartView) getLayoutInflater().inflate(R.layout.chart_cycle, list, false);
         list.addHeaderView(chart, null, true);
      }

      View list_header = getLayoutInflater().inflate(R.layout.item_chart_header, list, false);
      list.addHeaderView(list_header, null, true);

      adapter = new CyclesAdapter(this, null);
      list.setAdapter(adapter);

      installTypeButtons();

      needsRefresh = true;
      init();
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

      chart.setType(data.getTestType());
   }

   /**
    * Install the type buttons functionality.
    */
   protected void installTypeButtons()
   {
      buttonTypeOvul = (RadioButton) findViewById(R.id.btn_type_ovulation);
      buttonTypePreg = (RadioButton) findViewById(R.id.btn_type_pregnancy);

      buttonTypeOvul.setOnClickListener(new View.OnClickListener() {
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
               chart.setType(data.getTestType());
               reload();
            }
         }
      });

      buttonTypePreg.setOnClickListener(new View.OnClickListener() {
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
               chart.setType(data.getTestType());
               reload();
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

      ArrayList<CycleChartView.CycleData> cycles = chart.getCycles();

      long[] ids = new long[cycles.size()];
      for(int i = 0; i < cycles.size(); i++)
         ids[i] = cycles.get(i).id;

      outState.putLongArray(CYCLE_IDS, ids);
   }

   /**
    * Event invoked when the data is ready.
    */
   @Override
   protected void onServicesReady()
   {
      super.onServicesReady();

      updateType();

      // Restore state
      if(inState != null)
      {
         for(long id : inState.getLongArray(CYCLE_IDS))
            addCycle(id);
      }
      else
      {
         Intent intent = getIntent();
         long id = intent.getLongExtra(ID, -1);
         if(id >= 0)
            addCycle(id);
      }

      refreshList();
   }

   /**
    * Refreshes and updates the list of cycles.
    */
   protected void refreshList()
   {
      if(data == null)
         return;

      refreshNavBar();

      Cursor cursor = data.listCycles();
      adapter.changeCursor(cursor);

      needsRefresh = false;
   }

   /**
    * Returns the color of a cycle or -1 if not plotted.
    */
   public int cycleColor(long id)
   {
      return chart.getColor(id);
   }


   /**
    * Loads a cycle and adds it to the chart.
    */
   protected void addCycle(long id)
   {
      long start = data.loadCycleStart(id);
      long end = data.loadCycleEnd(start);
      addCycle(id, start, end);
   }

   /**
    * Loads the samples of a cycle from its start and end dates.
    */
   protected ArrayList<CycleChartView.TestSample> loadSamples(long start, long end)
   {
      Cursor cursor = data.listData(start, end);
      ArrayList<CycleChartView.TestSample> samples = new ArrayList<>();
      while(cursor.moveToNext())
      {
         CycleChartView.TestSample sample = new CycleChartView.TestSample();
         sample.date = cursor.getLong(cursor.getColumnIndexOrThrow(TestsData.TESTS_DATE));
         sample.pigment = cursor.getInt(cursor.getColumnIndexOrThrow(TestsData.TESTS_PIGMENTATION));
         samples.add(sample);
      }
      return samples;
   }

   /**
    * Loads a cycle and adds it to the chart.
    * @param end final date or -1 if last cycle.
    */
   protected void addCycle(long id, long start, long end)
   {
      chart.addCycle(id, start, end, loadSamples(start, end));
   }

   /**
    * Reloads the data of the chart.
    */
   protected void reload()
   {
      ArrayList<CycleChartView.CycleData> cycles = chart.getCycles();
      if(cycles.isEmpty())
         return;

      for(CycleChartView.CycleData cycle : cycles)
      {
         cycle.start = data.loadCycleStart(cycle.id);
         cycle.end = data.loadCycleEnd(cycle.start);
         cycle.samples = loadSamples(cycle.start, cycle.end);
      }

      chart.updateData();
   }

   /**
    * Cycle selected event handler.
    * @param end final date or -1 if last cycle.
    */
   public void onCycleSelected(long id, long start, long end)
   {
      if(chart.hasCycle(id))
      {
         chart.removeCycle(id);
      }
      else
      {
         addCycle(id, start, end);
      }

      adapter.notifyDataSetChanged();
   }
}
