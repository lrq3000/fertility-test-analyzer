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
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.GregorianCalendar;

/**
 * This activity lets the user create a new cycle by the beginning date or change an existing one.
 * <p>
 * The date selected is returned but the database is not modified.
 */
public class CycleEditionActivity extends AppCompatActivity
{
   static final String ACTION_CYCLE = "com.colnix.fta.CYCLE_EDITION";

   static final String DATA_EXPLANATION = "explain";
   static final String DATA_ID = "id";       // If set is just returned untouched
   static final String DATA_DATE = "date";   // in/out
   static final String DATA_MIN_DATE = "min_date";
   static final String DATA_MAX_DATE = "max_date";


   /**
    * Cycle id sent in the intent of -1 or none.
    */
   long cycleId;

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
      setContentView(R.layout.activity_cycle_edition);

      Intent intent = getIntent();

      /*
      String explanation = intent.getStringExtra(DATA_EXPLANATION);
      if(explanation != null)
      {
         TextView explain = (TextView) findViewById(R.id.date_explain);
         explain.setText(explanation);
      }*/

      cycleId = intent.getLongExtra(DATA_ID, -1);

      DatePicker date_picker = (DatePicker) findViewById(R.id.date_picker);
      date_picker.setCalendarViewShown(true);
      date_picker.setSpinnersShown(false);
      minDate = intent.getLongExtra(DATA_MIN_DATE, 0);
      if(minDate <= 0)
         minDate = new GregorianCalendar(2000, 0, 1).getTimeInMillis();
      maxDate = intent.getLongExtra(DATA_MAX_DATE, 0);

      if(!Config.calendarBug())
      {
         date_picker.setMinDate(TestsData.clearDate(minDate));
         if(maxDate > 0)
            date_picker.setMaxDate(TestsData.clearDate(maxDate));
      }

      long init_date = intent.getLongExtra(DATA_DATE, 0);
      if(init_date <= 0)
         init_date = new Date().getTime();
      GregorianCalendar date = new GregorianCalendar();
      date.setTimeInMillis(init_date);
      date_picker.init(date.get(GregorianCalendar.YEAR), date.get(GregorianCalendar.MONTH), date.get(GregorianCalendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener()
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
   }

   /**
    * Accepts the date and returns the value.
    */
   void acceptDate()
   {
      GregorianCalendar calendar = new GregorianCalendar();
      DatePicker date_picker = (DatePicker) findViewById(R.id.date_picker);

      calendar.set(date_picker.getYear(), date_picker.getMonth(), date_picker.getDayOfMonth(), 0, 0, 0);
      long date = calendar.getTimeInMillis();
      if(date < minDate)
      {
         date = minDate;
         Toast.makeText(this, R.string.date_limited, Toast.LENGTH_SHORT).show();
      }
      else if(maxDate > 0 && date > maxDate)
      {
         date = maxDate;
         Toast.makeText(this, R.string.date_limited, Toast.LENGTH_SHORT).show();
      }

      Intent result = new Intent(ACTION_CYCLE);
      if(cycleId >= 0)
         result.putExtra(DATA_ID, cycleId);
      result.putExtra(DATA_DATE, date);
      setResult(RESULT_OK, result);
      finish();
   }
}
