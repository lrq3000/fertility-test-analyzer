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

import android.content.SharedPreferences;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Adaptor to populate the list of tests in MyCyclesActivity.
 */
public class TestsAdapter extends BaseAdapter
{
   /**
    * Types of item.
    */
   public static final int ITEM_CYCLE = 0;
   public static final int ITEM_TEST = 1;
   public static final int ITEM_TOTAL = 2;

   /**
    * Item information.
    */
   class Item
   {
      /**
       * Type of item.
       */
      public int type;

      /**
       * Cursor index.
       */
      public int index;

      /**
       * Cursor pointing to the item row.
       */
      public Cursor cursor;

      /**
       * Says if the note is opened (only if type is ITEM_TEST).
       */
      public boolean noteVisible;

      /**
       * Cycle duration (only if type is ITEM_CYCLE).
       */
      public String duration;


      /**
       * Quick test constructor.
       */
      public Item(int i, Cursor c, boolean note)
      {
         type = ITEM_TEST;
         index = i;
         cursor = c;
         noteVisible = note;
      }

      /**
       * Quick cycle constructor.
       */
      public Item(int i, Cursor c, String dur)
      {
         type = ITEM_CYCLE;
         index = i;
         cursor = c;
         duration = dur;
      }
   }

   /**
    * Cycle item holder for quick access.
    */
   static class CycleHolder
   {
      /**
       * Cycle unique identifier.
       */
      public long id;

      /**
       * Creation date text box.
       */
      public TextView date;

      /**
       * Cycle duration text box.
       */
      public TextView duration;
   }

   /**
    * Test item holder for quick access.
    */
   static class TestHolder
   {
      /**
       * Test unique identifier.
       */
      public long id;

      /**
       * Current position in the list.
       */
      public int position;

      /**
       * Pigmentation text box.
       */
      public TextView pigment;

      /**
       * Creation date text box.
       */
      public TextView date;

      /**
       * Creation time text box.
       */
      public TextView time;

      /**
       * Button that shows the note.
       */
      public ImageButton noteButton;

      /**
       * Test note.
       */
      public TextView note;
   }

   /**
    * Activity reference.
    */
   MyCyclesActivity activity;

   /**
    * List of tests.
    */
   Cursor tests;

   /**
    * List of cycles.
    */
   Cursor cycles;

   /**
    * Preordered list of items.
    */
   ArrayList<Item> list;

   /**
    * Position of the selected test or -1 if none.
    */
   int selected;

   /**
    * Layout inflater to load new layouts.
    */
   LayoutInflater inflater;

   /**
    * Date formatter helper.
    */
   SimpleDateFormat dateFormatter;

   /**
    * Time formatter helper.
    */
   SimpleDateFormat timeFormatter;

   /**
    * Says if the review mode is enabled.
    */
   boolean reviewMode;


   /**
    * Creates the adaptor.
    */
   public TestsAdapter(MyCyclesActivity activity)
   {
      this.activity = activity;
      list = new ArrayList<>();
      selected = -1;
      inflater = LayoutInflater.from(activity);
      dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
      timeFormatter = new SimpleDateFormat("HH:mm");
      reviewMode = false;
   }


   /**
    * Load a list of preordered items. This speeds up the item retrieval.
    */
   protected void loadList()
   {
      list.clear();

      if(tests == null || cycles == null)
         return;

      SharedPreferences prefs = Config.getPrefs(activity);
      boolean noteVisible = prefs.getBoolean(Config.PREF_SHOW_NOTES, false);

      int test_index = 0;
      long last_cycle_date = 0;
      for(int cycle_index = 0; cycle_index < cycles.getCount(); cycle_index++)
      {
         cycles.moveToPosition(cycle_index);
         long cycle_date = cycles.getLong(cycles.getColumnIndexOrThrow(TestsData.CYCLES_START_DATE));
         long days;
         if(last_cycle_date != 0)
            days = (last_cycle_date - cycle_date) / (1000 * 60 * 60 * 24);
         else
            days = (TestsData.clearDate(System.currentTimeMillis()) - cycle_date) / (1000 * 60 * 60 * 24);
         Item it = new Item(cycle_index, cycles, String.format(activity.getString(R.string.cycle_days), (int) days));
         last_cycle_date = cycle_date;
         list.add(it);

         while(test_index < tests.getCount())
         {
            tests.moveToPosition(test_index);
            long test_date = tests.getLong(tests.getColumnIndexOrThrow(TestsData.TESTS_DATE));
            if(test_date < cycle_date)
               break;
            list.add(new Item(test_index, tests, noteVisible));
            test_index++;
         }
      }
   }

   /**
    * Change the underlying cursor to a new cursor. If there is an existing cursor it will be closed.
    */
   public void changeCursors(Cursor tests, Cursor cycles)
   {
      if(this.tests != null)
         this.tests.close();
      this.tests = tests;

      if(this.cycles != null)
         this.cycles.close();
      this.cycles = cycles;

      loadList();

      if(selected >= 0)
         setSelectedFirst();

      if(tests == null || cycles == null)
         notifyDataSetInvalidated();
      else
         notifyDataSetChanged();
   }

   /**
    * Enables or disables the review mode.
    */
   public void setReviewMode(boolean enable)
   {
      reviewMode = enable;
   }

   /**
    * How many items are in the data set represented by this Adapter.
    * @return Count of items.
    */
   @Override
   public int getCount()
   {
      if(tests == null || cycles == null)
         return 0;

      return list.size();
   }

   /**
    * Returns the number of types of Views that will be created by getView(int, View, ViewGroup).
    * Each type represents a set of views that can be converted in getView(int, View, ViewGroup).
    * If the adapter always returns the same type of View for all items, this method should return 1.
    */
   @Override
   public int getViewTypeCount()
   {
      return ITEM_TOTAL;
   }

   /**
    * Get the type of View that will be created by getView(int, View, ViewGroup) for the specified item.
    * @param position
    * @return An integer representing the type of View. Two views should share the same type if one
    *    can be converted to the other in getView(int, View, ViewGroup). Note: Integers must be in
    *    the range 0 to getViewTypeCount() - 1.
    */
   @Override
   public int getItemViewType(int position)
   {
      return getItem(position).type;
   }

   /**
    * Get the data item associated with the specified position in the data set.
    * @param position Position of the item whose data we want within the adapter's
    * data set.
    * @return The data at the specified position.
    */
   @Override
   public Item getItem(int position)
   {
      if(position < 0 || position > list.size())
         return null;

      Item it = list.get(position);
      if(it.cursor != null)
         it.cursor.moveToPosition(it.index);
      return it;
   }

   /**
    * Indicates whether the item ids are stable across changes to the underlying data.
    */
   @Override
   public boolean hasStableIds()
   {
      return true;
   }

   /**
    * Get the row id associated with the specified position in the list.
    * @param position The position of the item within the adapter's data set whose row id we want.
    * @return The id of the item at the specified position.
    */
   @Override
   public long getItemId(int position)
   {
      Item it = getItem(position);
      if(it == null)
         return 0;

      long id;
      if(it.type == ITEM_TEST)
      {
         id = it.cursor.getLong(it.cursor.getColumnIndexOrThrow(TestsData.TESTS_ID));
         id = id & 0x0FFFFFFFFFFFFFFFL;
      }
      else if(it.type == ITEM_CYCLE)
      {
         id = it.cursor.getLong(it.cursor.getColumnIndexOrThrow(TestsData.CYCLES_ID));
         id = id & 0x0FFFFFFFFFFFFFFFL | 0x1000000000000000L;
      }
      else // if(it.type == ITEM_AD)
      {
         id = position;
         id = id & 0x0FFFFFFFFFFFFFFFL | 0x2000000000000000L;
      }
      return id;
   }

   /**
    * Sets the selected element.
    */
   public void setSelected(int position)
   {
      selected = position;
   }

   /**
    * Sets the first test element selected.
    * @return the test id or -1 if none.
    */
   public void setSelectedFirst()
   {
      for(int pos = 0; pos < list.size(); pos++)
      {
         Item it = list.get(pos);
         if(it.type == ITEM_TEST)
         {
            setSelected(pos);
            return;
         }
      }

      setSelected(-1);
   }

   /**
    * Returns the selected test id or -1 if none.
    */
   public long getSelectedId()
   {
      if(selected < 0)
         return -1;

      Item it = getItem(selected);
      long id = it.cursor.getLong(it.cursor.getColumnIndexOrThrow(TestsData.TESTS_ID));
      id = id & 0x0FFFFFFFFFFFFFFFL;
      return id;
   }

   /**
    * Creates an empty test item.
    */
   protected View createTestItem(ViewGroup parent)
   {
      final View view = inflater.inflate(R.layout.item_test_edit, parent, false);

      final TestHolder holder = new TestHolder();
      holder.pigment = (TextView) view.findViewById(R.id.test_pigment);
      holder.date = (TextView) view.findViewById(R.id.test_date);
      holder.time = (TextView) view.findViewById(R.id.test_time);
      holder.noteButton = (ImageButton) view.findViewById(R.id.btn_note);
      holder.note = (TextView) view.findViewById(R.id.test_note);
      view.setTag(holder);

      view.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v)
         {
            activity.onTestSelected(view, holder.id, holder.position);
         }
      });

      ImageButton btn = (ImageButton) view.findViewById(R.id.btn_remove);
      btn.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v)
         {
            activity.onTestRemoveClicked(holder.id);
         }
      });

      holder.noteButton.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v)
         {
            if(holder.note == null)
            {
               activity.onTestSelected(view, holder.id, holder.position);
            }
            else
            {
               Item it = getItem(holder.position);
               it.noteVisible = !it.noteVisible;
               if(it.noteVisible)
                  holder.note.setVisibility(View.VISIBLE);
               else
                  holder.note.setVisibility(View.GONE);
            }
         }
      });

      return view;
   }

   /**
    * Fills a test item with the data.
    */
   protected void bindTestItem(int position, View view, Item it)
   {
      TestHolder holder = (TestHolder) view.getTag();

      holder.id = it.cursor.getLong(it.cursor.getColumnIndexOrThrow(TestsData.TESTS_ID));
      holder.position = position;

      int pigment = it.cursor.getInt(it.cursor.getColumnIndexOrThrow(TestsData.TESTS_PIGMENTATION));
      holder.pigment.setText(String.format(activity.getString(R.string.pigment_percent), pigment));

      Date date = new Date(it.cursor.getLong(it.cursor.getColumnIndexOrThrow(TestsData.TESTS_DATE)));
      holder.date.setText(dateFormatter.format(date));
      holder.time.setText(timeFormatter.format(date));

      String note = it.cursor.getString(it.cursor.getColumnIndexOrThrow(TestsData.TESTS_NOTE));
      if(note == null)
         holder.noteButton.setVisibility(View.INVISIBLE);
      else
         holder.noteButton.setVisibility(View.VISIBLE);

      if(holder.note != null)
      {
         holder.note.setText(note);
         if(it.noteVisible)
            holder.note.setVisibility(View.VISIBLE);
         else
            holder.note.setVisibility(View.GONE);
      }

      if(selected >= 0 && selected == position)
      {
         view.setActivated(true);
         if(android.os.Build.VERSION.SDK_INT >= 21)
            view.setTranslationZ(3);
      }
      else
      {
         view.setActivated(false);
         if(android.os.Build.VERSION.SDK_INT >= 21)
            view.setTranslationZ(0);
      }
   }

   /**
    * Creates an empty cycle item.
    */
   protected View createCycleItem(ViewGroup parent)
   {
      final View view = inflater.inflate(R.layout.item_cycle_header, parent, false);

      final CycleHolder holder = new CycleHolder();
      holder.date = (TextView) view.findViewById(R.id.cycle_date);
      holder.duration = (TextView) view.findViewById(R.id.cycle_duration);
      view.setTag(holder);

      view.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v)
         {
            activity.onCycleSelected(view, holder.id);
         }
      });

      ImageButton btn = (ImageButton) view.findViewById(R.id.btn_cycle_see);
      btn.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v)
         {
            activity.onCycleDetailsClick(holder.id);
         }
      });

      btn = (ImageButton) view.findViewById(R.id.btn_cycle_remove);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View view)
         {
            activity.onCycleRemoveClick(holder.id);
         }
      });

      return view;
   }

   /**
    * Fills a cycle item with the data.
    */
   protected void bindCycleItem(View view, Item it)
   {
      CycleHolder holder = (CycleHolder) view.getTag();

      holder.id = it.cursor.getLong(it.cursor.getColumnIndexOrThrow(TestsData.CYCLES_ID));

      Date date = new Date(it.cursor.getLong(it.cursor.getColumnIndexOrThrow(TestsData.CYCLES_START_DATE)));
      holder.date.setText(String.format(activity.getString(R.string.cycle_header), dateFormatter.format(date)));

      holder.duration.setText(it.duration);
   }

   /**
    * Get a View that displays the data at the specified position in the data set. You can either
    * create a View manually or inflate it from an XML layout file. When the View is inflated, the
    * parent View (GridView, ListView...) will apply default layout parameters unless you use
    * {@link LayoutInflater#inflate(int, ViewGroup, boolean)}
    * to specify a root view and to prevent attachment to the root.
    * @param position The position of the item within the adapter's data set of the item whose view
    * we want.
    * @param convertView The old view to reuse, if possible. Note: You should check that this view
    * is non-null and of an appropriate type before using. If it is not possible to convert
    * this view to display the correct data, this method can create a new view.
    * Heterogeneous lists can specify their number of view types, so that this View is
    * always of the right type (see {@link #getViewTypeCount()} and
    * {@link #getItemViewType(int)}).
    * @param parent The parent that this view will eventually be attached to
    * @return A View corresponding to the data at the specified position.
    */
   @Override
   public View getView(int position, View convertView, ViewGroup parent)
   {
      Item it = getItem(position);
      if(it.type == ITEM_TEST)
      {
         if(convertView == null)
            convertView = createTestItem(parent);

         bindTestItem(position, convertView, it);

         return convertView;
      }
      else
      {
         if(convertView == null)
            convertView = createCycleItem(parent);

         bindCycleItem(convertView, it);

         return convertView;
      }
   }
}
