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

import android.content.Context;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Adaptor to populate the list of cycles in ChartsActivity.
 */
public class CyclesAdapter extends CursorAdapter
{
   /**
    * Cycle item holder for quick access.
    */
   protected static class CycleHolder
   {
      /**
       * Cycle database ID.
       */
      public long id;

      /**
       * Add to/remove from chart button.
       */
      public ImageView addButton;

      /**
       * Creation date text box.
       */
      public TextView date;

      /**
       * Cycle duration text box.
       */
      public TextView duration;

      /**
       * Default (or unselected) color.
       */
      public int defaultColor;

      /**
       * Cycle edit button.
       */
      public ImageButton editButton;

      /**
       * Cycle delete button.
       */
      public ImageButton deleteButton;

      /**
       * Start date of the cycle.
       */
      public long start;

      /**
       * End date of the cycle or 0.
       */
      public long end;
   }


   /**
    * Main activity.
    */
   protected ChartsActivity activity;

   /**
    * Layout inflater to load new layouts.
    */
   protected LayoutInflater inflater;

   /**
    * Date formatter helper.
    */
   protected SimpleDateFormat dateFormatter;


   /**
    * Creates the adaptor.
    */
   public CyclesAdapter(ChartsActivity activity, Cursor cursor)
   {
      super(activity, cursor, 0);

      this.activity = activity;
      inflater = LayoutInflater.from(activity);
      dateFormatter = new SimpleDateFormat(Config.getDateFormat(activity));
   }


   /**
    * Makes a new view to hold the data pointed to by cursor.
    *
    * @param context Interface to application's global information
    * @param cursor  The cursor from which to get the data. The cursor is already
    *                moved to the correct position.
    * @param parent  The parent to which the new view is attached to
    * @return the newly created view.
    */
   @Override
   public View newView(Context context, Cursor cursor, ViewGroup parent)
   {
      View view = inflater.inflate(R.layout.item_cycle_edit, parent, false);

      final CycleHolder holder = new CycleHolder();
      holder.addButton = (ImageView) view.findViewById(R.id.btn_add);
      holder.date = (TextView) view.findViewById(R.id.cycle_date);
      holder.duration = (TextView) view.findViewById(R.id.cycle_duration);
      holder.defaultColor = holder.date.getCurrentTextColor();
      view.setTag(holder);

      view.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            activity.onCycleSelected(holder.id, holder.start, holder.end);
         }
      });

      return view;
   }

   /**
    * Bind an existing view to the data pointed to by cursor
    *
    * @param view    Existing view, returned earlier by newView
    * @param context Interface to application's global information
    * @param cursor  The cursor from which to get the data. The cursor is already
    */
   @Override
   public void bindView(View view, Context context, Cursor cursor)
   {
      CycleHolder holder = (CycleHolder) view.getTag();
      holder.id = cursor.getLong(cursor.getColumnIndexOrThrow(TestsData.CYCLES_ID));
      holder.start = cursor.getLong(cursor.getColumnIndexOrThrow(TestsData.CYCLES_START_DATE));
      holder.date.setText(dateFormatter.format(new Date(holder.start)));

      long days;
      if(cursor.moveToPrevious() && !cursor.isBeforeFirst())
      {
         holder.end = cursor.getLong(cursor.getColumnIndexOrThrow(TestsData.CYCLES_START_DATE));
         days = (holder.end - holder.start) / (1000 * 60 * 60 * 24);

      }
      else
      {
         holder.end = -1;
         days = (TestsData.clearDate(System.currentTimeMillis()) - holder.start) / (1000 * 60 * 60 * 24);
      }
      holder.duration.setText(String.format(activity.getString(R.string.cycle_days), (int) days));

      int color = activity.cycleColor(holder.id);
      if(color != 0)
      {
         holder.addButton.setImageResource(R.drawable.ico_minus);

         if(android.os.Build.VERSION.SDK_INT >= 21)
            holder.addButton.setBackgroundTintList(ColorStateList.valueOf(color));
         holder.date.setTextColor(color);
         holder.duration.setTextColor(color);
      }
      else
      {
         holder.addButton.setImageResource(R.drawable.ico_plus);

         if(android.os.Build.VERSION.SDK_INT >= 21)
            holder.addButton.setBackgroundTintList(ColorStateList.valueOf(holder.defaultColor));
         holder.date.setTextColor(holder.defaultColor);
         holder.duration.setTextColor(holder.defaultColor);
      }
   }
}
