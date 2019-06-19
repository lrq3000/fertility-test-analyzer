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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Activity that allows the user to select a date and time for a new test.
 * <p>
 * The date selected is returned but the database is not modified.
 */
public class DateSelectionActivity extends AppCompatActivity
{
   static final String ACTION_DATE = "com.colnix.fta.DATE_SELECTION";

   static final String DATA_DATE = "date";   // in/out

   static final String STATE_DATE_SELECTED = "date_sel";

   /**
    * True if the date has alredy been selected.
    */
   boolean dateSelected;

   /**
    * Minimum valid date.
    */
   long minDate;

   /**
    * Maximum valid date.
    */
   long maxDate;


   /**
    * Called when the activity is starting. This is where most initialization should go: calling
    * setContentView(int) to inflate the activity's UI, using findViewById(int) to programmatically
    * interact with widgets in the UI
    */
   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_date_selection);

      long init_date = getIntent().getLongExtra(DATA_DATE, 0);
      if(init_date <= 0)
         init_date = new Date().getTime();
      GregorianCalendar date = new GregorianCalendar();
      date.setTimeInMillis(init_date);

      dateSelected = false;
      if(savedInstanceState != null && savedInstanceState.getBoolean(STATE_DATE_SELECTED))
         dateSelected = true;

      // Date panel
      View panel = findViewById(R.id.date_layout);
      if(!dateSelected)
         panel.setVisibility(View.VISIBLE);
      else
         panel.setVisibility(View.GONE);

      DatePicker date_picker = (DatePicker) findViewById(R.id.date_picker);
      date_picker.setCalendarViewShown(true);
      date_picker.setSpinnersShown(false);
      minDate = new GregorianCalendar(2000, 0, 1).getTimeInMillis();
      maxDate = System.currentTimeMillis();

      if(!Config.calendarBug())
      {
         date_picker.setMinDate(minDate);
         date_picker.setMaxDate(maxDate);
      }

      // If rotating the state value is loaded after onCreate.
      date_picker.init(date.get(GregorianCalendar.YEAR), date.get(GregorianCalendar.MONTH),
            date.get(GregorianCalendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener()
            {
               @Override
               public void onDateChanged(DatePicker datePicker, int i, int i1, int i2)
               {
                  // It fires with the set of the year, but the user may want to select the month
                  // and day.
                  //acceptDate();
               }
            });

      Button btn = (Button) findViewById(R.id.date_cancel);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View view)
         {
            setResult(RESULT_CANCELED);
            finish();
         }
      });

      btn = (Button) findViewById(R.id.date_accept);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View view)
         {
            acceptDate();
         }
      });

      // Time panel
      panel = findViewById(R.id.time_layout);
      if(!dateSelected)
         panel.setVisibility(View.GONE);
      else
         panel.setVisibility(View.VISIBLE);

      TimePicker time_picker = (TimePicker) findViewById(R.id.time_picker);
      time_picker.setIs24HourView(true);
      time_picker.setCurrentHour(date.get(GregorianCalendar.HOUR));
      time_picker.setCurrentMinute(date.get(GregorianCalendar.MINUTE));
      /* It fires with the set of the hour, not waiting for the minute to be selected.
      time_picker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener()
      {
         @Override
         public void onTimeChanged(TimePicker timePicker, int i, int i1)
         {
            acceptTime();
         }
      });
      */

      btn = (Button) findViewById(R.id.time_cancel);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View view)
         {
            setResult(RESULT_CANCELED);
            finish();
         }
      });

      btn = (Button) findViewById(R.id.time_accept);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View view)
         {
            acceptTime();
         }
      });
   }

   @Override
   public void onSaveInstanceState(Bundle outState)
   {
      outState.putBoolean(STATE_DATE_SELECTED, dateSelected);

      super.onSaveInstanceState(outState);
   }

   /**
    * Accepts the date and move on to the time.
    */
   void acceptDate()
   {
      View panel = findViewById(R.id.date_layout);
      panel.setVisibility(View.GONE);
      panel = findViewById(R.id.time_layout);
      panel.setVisibility(View.VISIBLE);

      dateSelected = true;
   }

   /**
    * Accepts the time and returns the result.
    */
   void acceptTime()
   {
      GregorianCalendar calendar = new GregorianCalendar();
      DatePicker date_picker = (DatePicker) findViewById(R.id.date_picker);
      TimePicker time_picker = (TimePicker) findViewById(R.id.time_picker);

      calendar.set(date_picker.getYear(), date_picker.getMonth(), date_picker.getDayOfMonth(),
            time_picker.getCurrentHour(), time_picker.getCurrentMinute());

      long date = calendar.getTimeInMillis();
      if(date < minDate)
      {
         date = minDate;
         Toast.makeText(this, R.string.date_limited, Toast.LENGTH_SHORT).show();
      }
      else if(date > maxDate)
      {
         date = maxDate;
         Toast.makeText(this, R.string.date_limited, Toast.LENGTH_SHORT).show();
      }

      Intent result = new Intent(ACTION_DATE);
      result.putExtra(DATA_DATE, date);
      setResult(RESULT_OK, result);
      finish();
   }
}
