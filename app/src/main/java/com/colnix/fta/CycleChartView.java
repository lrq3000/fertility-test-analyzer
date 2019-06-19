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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Ovulation cycle(s) chart view.
 */
public class CycleChartView extends HorizontalScrollView
{
   /**
    * Chart colors.
    */
   public static final int COLOR_BACKGROUND = 0xFFFFFFFF;
   public static final int COLOR_GRID = 0xFFBDBDBD;
   public static final int COLOR_TEXT = 0xFF757575;
   public static final int COLOR_INTER = 0xFFFF4970;
   public static final int COLOR_INTER_AREA = 0x80FFE4EA;

   public static final int COLOR_GRAPH_1 = 0xFF3CBCF2;
   public static final int COLOR_GRAPH_2 = 0xFFF23C3C;
   public static final int COLOR_GRAPH_3 = 0xFFB43CF2;
   public static final int COLOR_GRAPH_4 = 0xFF3CF26B;

   /**
    * Default dimensions.
    */
   public static final int THRESHOLD_DP = 500;
   public static final int HEIGHT_SMALL_DP = 320;
   public static final int HEIGHT_BIG_DP = 440;


   public static class TestSample
   {
      /**
       * Date in which the test was taken.
       */
      public long date;

      /**
       * Pigmentation percent.
       */
      public int pigment;
   }

   /**
    * Cycle entry data.
    */
   public static class CycleData
   {
      /**
       * Cycle database id.
       */
      public long id;

      /**
       * Cycle starting date.
       */
      public long start;

      /**
       * Cycle starting date.
       */
      public long end;
      /**
       * Assigned color.
       */
      public int color;

      /**
       * List of test samples of the cycle.
       */
      public List<TestSample> samples;


      /**
       * Fast constructor.
       */
      public CycleData(long id, long s, long e, int c, List<TestSample> sam)
      {
         this.id = id;
         start = s;
         end = e;
         color = c;
         samples = sam;
      }


      /**
       * Returns the number of days of the cycle.
       */
      public int getDays()
      {
         if(end > 0)
         {
            long days = (end - start) / (1000 * 60 * 60 * 24);
            return (int) days;
         }
         else if(samples.isEmpty())
         {
            return 1;
         }
         else
         {
            long last = samples.get(samples.size() - 1).date;

            long days = (last - start - 1) / (1000 * 60 * 60 * 24) + 2;
            return (int) days;
         }
      }

      /**
       * Returns the maximum pigmentation level reached.
       */
      public int maxPigment()
      {
         // Should we cached this operation?
         int max = 0;
         for(TestSample sample : samples)
         {
            if(sample.pigment > max)
               max = sample.pigment;
         }
         return ((max - 1) / 10 + 1) * 10;
      }
   }

   /**
    * Internal View used to set the dimensions of the scroll area.
    */
   class ScrollDimensions extends View
   {
      /**
       * Desired dimensions in pixels.
       */
      int desiredWidth;
      int desiredHeight;


      /**
       * View simplest construction.
       */
      public ScrollDimensions(Context context)
      {
         super(context);

         float density = getResources().getDisplayMetrics().density;
         desiredWidth = (int) (THRESHOLD_DP * density);
         desiredHeight = (int) (HEIGHT_SMALL_DP * density);
      }


      /**
       * Sets (or resets) the scrolling dimensions in pixels.
       */
      public void setSize(float width, float height)
      {
         desiredWidth = (int) width;
         desiredHeight = (int) height;

         requestLayout();
         invalidate();
      }

      /**
       * Measure the view and its content to determine the measured width and the measured height.
       * This method is invoked by measure(int, int) and should be overridden by subclasses to provide
       * accurate and efficient measurement of their contents.
       */
      @Override
      protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
      {
         int widthMode = MeasureSpec.getMode(widthMeasureSpec);
         int widthSize = MeasureSpec.getSize(widthMeasureSpec);
         int width;

         if(widthMode == MeasureSpec.EXACTLY)
         {
            width = widthSize;
         }
         else if(widthMode == MeasureSpec.AT_MOST)
         {
            width = Math.min(desiredWidth, widthSize);
         }
         else
         {
            width = desiredWidth;
         }

         int heightMode = MeasureSpec.getMode(heightMeasureSpec);
         int heightSize = MeasureSpec.getSize(heightMeasureSpec);
         int height;

         if(heightMode == MeasureSpec.EXACTLY)
         {
            height = heightSize;
         }
         else if(heightMode == MeasureSpec.AT_MOST)
         {
            height = Math.min(desiredHeight, heightSize);
         }
         else
         {
            height = desiredHeight;
         }

         setMeasuredDimension(width, height);
      }
   }


   /**
    * List of colors to assign.
    */
   ArrayList<Integer> colors;

   /**
    * List of cycles data.
    */
   ArrayList<CycleData> cycles;

   /**
    * Graph desired offset or -1 if none.
    */
   int desiredOffset;

   /**
    * Type of test, namely TYPES_TYPE_OVULATION or TYPES_TYPE_PREGNANCY.
    */
   int testType;

   /**
    * Chart paints.
    */
   Paint paintGrid;
   Paint paintText;
   Paint paintTitle;
   Paint paintInter;
   Paint paintInterArea;
   Paint paintData;

   /**
    * Radius of the grid dots.
    */
   float gridRadius;

   /**
    * Radius of the sample dots.
    */
   float sampleRadius;

   /**
    * Canvas dimensions.
    */
   int width;
   int height;

   /**
    * X and Y axis frames.
    */
   RectF xFrame;
   RectF yFrame;

   /**
    * Number of ticks (or grid density) in both axis.
    */
   int xTicks;
   int yTicks;

   /**
    * Maximum value of each axis.
    */
   int xMax;
   int yMax;

   /**
    * Actual tick step in the Y axis (pigment).
    */
   int yStep;

   /**
    * Graph pre-rendered bitmap.
    */
   Bitmap graph;

   /**
    * Indicatior arrows
    */
   Path leftArrow;
   Path rightArrow;

   /**
    * Date formatter helper.
    */
   SimpleDateFormat dateFormatter;

   /**
    * Scrolling dimensions helper.
    */
   ScrollDimensions dimensions;


   /**
    * View simplest construction.
    */
   public CycleChartView(Context context)
   {
      this(context, null);
   }

   /**
    * View construction with parameters for XML laying out.
    */
   public CycleChartView(Context context, AttributeSet attrs)
   {
      this(context, attrs, 0);
   }

   /**
    * View construction with parameters for XML laying out.
    */
   public CycleChartView(Context context, AttributeSet attrs, int defStyle)
   {
      super(context, attrs, defStyle);

      clear();
      testType = TestsData.TYPES_TYPE_OVULATION;

      paintGrid = new Paint();
      paintGrid.setColor(COLOR_GRID);
      paintGrid.setStyle(Paint.Style.STROKE);
      paintGrid.setStrokeWidth(2f);
      paintGrid.setAntiAlias(true);

      paintText = new Paint();
      paintText.setColor(COLOR_TEXT);
      paintText.setAntiAlias(true);

      paintTitle = new Paint();
      paintTitle.setColor(COLOR_TEXT);
      paintTitle.setFakeBoldText(true);
      paintTitle.setAntiAlias(true);

      paintInter = new Paint();
      paintInter.setColor(COLOR_INTER);
      paintInter.setStyle(Paint.Style.FILL);
      paintInter.setStrokeWidth(2f);
      paintInter.setPathEffect(new DashPathEffect(new float[] {10, 10}, 0));
      paintInter.setAntiAlias(true);

      paintInterArea = new Paint();
      paintInterArea.setColor(COLOR_INTER_AREA);
      paintInterArea.setStyle(Paint.Style.FILL);
      paintInterArea.setStrokeWidth(2f);
      paintInterArea.setAntiAlias(true);

      paintData = new Paint();
      paintData.setColor(COLOR_TEXT);
      paintData.setStyle(Paint.Style.FILL);
      paintData.setStrokeWidth(2f);
      paintData.setAntiAlias(true);

      dateFormatter = new SimpleDateFormat("dd/MM/yyyy");

      dimensions = new ScrollDimensions(context);
      addView(dimensions);
      setFillViewport(true);
   }


   /**
    * Clears all the chart data.
    */
   public void clear()
   {
      colors = new ArrayList<>();
      colors.add(COLOR_GRAPH_1);
      colors.add(COLOR_GRAPH_2);
      colors.add(COLOR_GRAPH_3);
      colors.add(COLOR_GRAPH_4);

      cycles = new ArrayList<>();
      desiredOffset = -1;

      updateData();
   }

   /**
    * Checks if the chart has one cycle.
    */
   public boolean hasCycle(long id)
   {
      for(CycleData current : cycles)
      {
         if(current.id == id)
            return true;
      }
      return false;
   }

   /**
    * Returns the color of a cycle or 0 if not in the chart.
    */
   public int getColor(long id)
   {
      for(CycleData current : cycles)
      {
         if(current.id == id)
            return current.color;
      }
      return 0;
   }

   /**
    * Removes a new cycle from the chart.
    */
   public void removeCycle(long id)
   {
      for(CycleData current : cycles)
      {
         if(current.id == id)
         {
            colors.add(0, current.color);
            cycles.remove(current);

            updateData();
            return;
         }
      }
   }

   /**
    * Adds a new cycle to the chart.
    */
   public void addCycle(long id, long start, long end, List<TestSample> samples)
   {
      // If exists, remove and reinsert at the front
      for(CycleData current : cycles)
      {
         if(current.id == id)
         {
            cycles.remove(current);

            current.start = start;
            current.end = end;
            current.samples = samples;
            cycles.add(current);

            updateData();
            return;
         }
      }

      // Select a new color
      if(colors.isEmpty())
      {
         assert !cycles.isEmpty();

         // Remove older cycle
         colors.add(cycles.get(0).color);
         cycles.remove(0);
      }
      int color = colors.get(0);
      colors.remove(0);

      // Insert and update
      CycleData data = new CycleData(id, start, end, color, samples);
      cycles.add(data);

      updateData();
   }

   /**
    * Returns the list of cycles data.
    * Don't forget to call updateData() if you modify it!
    */
   public ArrayList<CycleData> getCycles()
   {
      return cycles;
   }


   /**
    * Changes the test type.
    */
   public void setType(int type)
   {
      testType = type;

      updateData();
   }


   /**
    * Returns the title of the chart.
    */
   String getTitle()
   {
      if(cycles.isEmpty())
      {
         return "";
      }
      else if(cycles.size() > 1)
      {
         return String.format(getResources().getString(R.string.chart_cycles), cycles.size());
      }
      else
      {
         return String.format(getResources().getString(R.string.chart_cycle),
               dateFormatter.format(new Date(cycles.get(0).start)));
      }
   }

   /**
    * This is called during layout when the size of this view has changed. If you were just added
    * to the view hierarchy, you're called with the old values of 0.
    */
   @Override
   protected void onSizeChanged(int w, int h, int oldw, int oldh)
   {
      super.onSizeChanged(w, h, oldw, oldh);

      updateSize(w, h);
   }

   /**
    * Updates the layout to fit the data if necessary (and possible).
    */
   public void updateData()
   {
      yMax = 0;
      xMax = 0;
      for(CycleData data : cycles)
      {
         int pigment = data.maxPigment();
         if(pigment > yMax)
            yMax = pigment;

         int days = data.getDays();
         if(days > xMax)
            xMax = days;
      }

      if(width > 0 && height > 0)
      {
         desiredOffset = update();

         invalidate();
      }
   }

   /**
    * Updates the layout to the given size if necessary (and possible).
    */
   void updateSize(int w, int h)
   {
      if(width != w || height != h)
      {
         width = w;
         height = h;

         if(width > 0 && height > 0)
            update();
      }
   }

   /**
    * Calculates the offset of the first cycle without drawing anything.
    * @return  the offset of the first cycle detected in the last data update or -1 if none.
    */
   int calculateFirstCycleOffset()
   {
      // Interpretation
      int inter_thres;
      if(testType == TestsData.TYPES_TYPE_OVULATION)
         inter_thres = TestsData.POSITIVE_OVULATION;
      else
         inter_thres = TestsData.POSITIVE_PREGNANCY;

      float x_step = xFrame.width() / xTicks;

      if(testType == TestsData.TYPES_TYPE_OVULATION && cycles.size() == 1)
      {
         CycleData data = cycles.get(0);
         float start_day = -1;
         float end_day = -1;
         boolean previous = false;
         for(TestSample sample : data.samples)
         {
            float sample_day = (float)(sample.date - data.start) / (1000 * 60 * 60 * 24);
            if(end_day > 0 && sample_day > end_day)
            {
               float start_x = (start_day + 0.5f) * x_step;
               return (int)start_x;
            }

            if(sample.pigment >= inter_thres)
            {
               if(!previous)
               {
                  if(start_day < 0)
                     start_day = sample_day;
                  end_day = sample_day + 3;
               }
               previous = true;
            }
            else
            {
               previous = false;
            }
         }

         if(end_day > 0)
         {
            float start_x = (start_day + 0.5f) * x_step;
            return (int)start_x;
         }
      }

      return -1;
   }

   /**
    * Draws the graph or part of it.
    * @return  the offset of the first cycle detected in the last data update or -1 if none.
    */
   int drawGraph(Canvas canvas, int width, int height)
   {
      // Background
      canvas.drawColor(COLOR_BACKGROUND);

      // Grid
      float x_step = xFrame.width() / xTicks;
      float y_step = yFrame.height() / yTicks;
      paintGrid.setStyle(Paint.Style.FILL);
      for(int i = 0; i < width / x_step; i++)
      {
         for(int j = 0; j < yTicks; j++)
         {
            canvas.drawCircle((i + 0.5f) * x_step, (j + 0.5f) * y_step, gridRadius, paintGrid);
         }
      }

      // Interpretation
      int inter_thres;
      if(testType == TestsData.TYPES_TYPE_OVULATION)
         inter_thres = TestsData.POSITIVE_OVULATION;
      else
         inter_thres = TestsData.POSITIVE_PREGNANCY;

      int firstCycleOffset = -1;
      if(testType == TestsData.TYPES_TYPE_OVULATION && cycles.size() == 1)
      {
         CycleData data = cycles.get(0);
         float start_day = -1;
         float end_day = -1;
         boolean previous = false;
         for(TestSample sample : data.samples)
         {
            float sample_day = (float)(sample.date - data.start) / (1000 * 60 * 60 * 24);
            if(end_day > 0 && sample_day > end_day)
            {
               float start_x = (start_day + 0.5f) * x_step;
               float end_x = (end_day + 0.5f) * x_step;
               canvas.drawRect(start_x, 0, end_x, height, paintInterArea);

               if(firstCycleOffset == -1)
                  firstCycleOffset = (int)start_x;

               start_day = -1;
               end_day = -1;
            }

            if(sample.pigment >= inter_thres)
            {
               if(!previous)
               {
                  if(start_day < 0)
                     start_day = sample_day;
                  end_day = sample_day + 3;
               }
               previous = true;
            }
            else
            {
               previous = false;
            }
         }

         if(end_day > 0)
         {
            float start_x = (start_day + 0.5f) * x_step;
            float end_x = (end_day + 0.5f) * x_step;
            canvas.drawRect(start_x, 0, end_x, height, paintInterArea);

            if(firstCycleOffset == -1)
               firstCycleOffset = (int)start_x;
         }
      }

      float inter_y = height - ((float)inter_thres / yStep + 0.5f) * y_step;
      canvas.drawLine(0, inter_y, width, inter_y, paintInter);

      // Graphs
      for(CycleData data : cycles)
      {
         paintData.setColor(data.color);

         float previous_x = 0f;
         float previous_y = 0f;
         for(TestSample sample : data.samples)
         {
            float days = (float)(sample.date - data.start) / (1000 * 60 * 60 * 24);
            float x = (days + 0.5f) * x_step;
            float y = height - ((float)sample.pigment / yStep + 0.5f) * y_step;

            canvas.drawCircle(x, y, sampleRadius, paintData);

            if(previous_x > 0f && previous_y > 0f)
            {
               canvas.drawLine(previous_x, previous_y, x, y, paintData);
            }

            previous_x = x;
            previous_y = y;
         }
      }

      return firstCycleOffset;
   }

   /**
    * Updates the chart layout.
    * @return  the offset of the first cycle detected in the last data update or -1 if none.
    */
   int update()
   {
      // Calculate layout
      float margin = 10f;
      float label_margin = 30f;
      float text_size = 12f;
      gridRadius = 0.8f;
      sampleRadius = 3f;
      float desiredHeight = HEIGHT_SMALL_DP;

      DisplayMetrics dm = getResources().getDisplayMetrics();
      float density = dm.density;
      int smallest = Math.min(dm.widthPixels, dm.heightPixels);
      if(smallest / density > THRESHOLD_DP)
      {
         margin = 20f;
         label_margin = 40f;
         text_size = 16f;
         gridRadius = 1.6f;
         sampleRadius = 4f;
         desiredHeight = HEIGHT_BIG_DP;
      }

      margin *= density;
      label_margin *= density;
      text_size *= density;
      gridRadius *= density;
      sampleRadius *= density;
      desiredHeight *= density;
      float text_margin = text_size * 1.7f;
      float grid = text_margin;

      paintText.setTextSize(text_size);
      paintTitle.setTextSize(text_size * 1.2f);

      yFrame = new RectF(margin, margin + text_margin * 2, width - margin, height - margin - text_margin * 2);
      xFrame = new RectF(margin + label_margin, margin + text_margin * 2, width - margin, height - margin - text_margin);

      xTicks = (int)(xFrame.width() / grid);
      yTicks = (int)(yFrame.height() / grid);

      yStep = (((yMax / (yTicks - 1) - 1) / 10 + 1) * 10);
      if(yStep == 0)
         yStep = 10;

      // Pre-render graph:
      int firstCycleOffset;
      int graph_width = (int) xFrame.width();
      if(xMax > xTicks)
         graph_width = (int) (xMax * xFrame.width() / xTicks);

      if(graph != null)
         graph.recycle();
      graph = null;
      try
      {
         graph = Bitmap.createBitmap(graph_width, (int)yFrame.height(), Bitmap.Config.ARGB_8888);
         Canvas canvas = new Canvas(graph);
         firstCycleOffset = drawGraph(canvas, graph_width, (int)yFrame.height());
      }
      catch(Exception | OutOfMemoryError e)
      {
         if(graph != null)
            graph.recycle();
         graph = null;

         firstCycleOffset = calculateFirstCycleOffset();

         Toast.makeText(this.getContext(), R.string.chart_long_cycles, Toast.LENGTH_SHORT).show();
      }

      // Arrows
      leftArrow = new Path();
      float arrow_x = xFrame.left + text_size/2;
      float arrow_y = xFrame.top + yFrame.height() / 2;
      leftArrow.moveTo(arrow_x, arrow_y);
      leftArrow.lineTo(arrow_x + text_size, arrow_y - text_size);
      leftArrow.lineTo(arrow_x + text_size, arrow_y + text_size);
      leftArrow.close();

      rightArrow = new Path();
      arrow_x = xFrame.right - text_size/2;
      rightArrow.moveTo(arrow_x, arrow_y);
      rightArrow.lineTo(arrow_x - text_size, arrow_y - text_size);
      rightArrow.lineTo(arrow_x - text_size, arrow_y + text_size);
      rightArrow.close();

      // Set new dimensions
      dimensions.setSize(width - xFrame.width() + graph_width, desiredHeight);

      return firstCycleOffset;
   }

   /**
    * Implement this to do your drawing.
    * @param canvas the canvas on which the background will be drawn.
    */
   @Override
   protected void onDraw(Canvas canvas)
   {
      // Background
      canvas.drawColor(COLOR_BACKGROUND);

      // Update if not done already
      updateSize(canvas.getWidth(), canvas.getHeight());

      // Calculate offset
      if(desiredOffset != -1)
      {
         desiredOffset -= xFrame.width() / 3;
         if(desiredOffset > 0)
            scrollTo(desiredOffset, 0);
         desiredOffset = -1;
      }
      
      Rect bounds = canvas.getClipBounds();
      canvas.save();
      canvas.translate(bounds.left, 0);

      float x_step = xFrame.width() / xTicks;
      float y_step = yFrame.height() / yTicks;

      int xOffset = (int) (bounds.left / x_step + 0.5f);
      int max = xMax - xTicks;
      if(xOffset < 0)
         xOffset = 0;
      else if(max <= 0)
         xOffset = 0;
      else if(max > 0 && xOffset > max)
         xOffset = max;

      float offset = xOffset * x_step;

      // Chart title
      paintTitle.setTextAlign(Paint.Align.CENTER);
      String ti = getResources().getString(R.string.chart_title);
      canvas.drawText(ti, (yFrame.right/2)-paintTitle.getTextScaleX(), yFrame.top * 0.55f , paintTitle);

      // Graph
      int graph_width = (int) xFrame.width();
      if(xMax > xTicks)
         graph_width = (int) (xMax * xFrame.width() / xTicks);

      if(graph != null)
      {
         Bitmap graphClip = Bitmap.createBitmap(graph, (int)offset, 0, (int)xFrame.width(), (int)yFrame.height());
         canvas.drawBitmap(graphClip, (int)xFrame.left, (int)xFrame.top, null);
      }
      else
      {
         canvas.save();
         canvas.clipRect((int)xFrame.left, (int)xFrame.top, (int)xFrame.right, (int)yFrame.bottom);
         canvas.translate((int)xFrame.left - (int)offset, (int)xFrame.top);

         drawGraph(canvas, graph_width, (int)yFrame.height());

         canvas.restore();
      }

      // Frames
      paintGrid.setStyle(Paint.Style.STROKE);
      canvas.drawRect(xFrame, paintGrid);
      canvas.drawRect(yFrame, paintGrid);

      // Arrows
      if(offset > 0)
         canvas.drawPath(leftArrow, paintInterArea);
      if(offset + xFrame.width() < graph_width)
         canvas.drawPath(rightArrow, paintInterArea);

      // Axis
      paintText.setTextAlign(Paint.Align.LEFT);
      String str = getResources().getString(R.string.chart_pigmentation);
      canvas.drawText(str, yFrame.left, yFrame.top - paintText.getTextSize() / 2, paintText);
      paintText.setTextAlign(Paint.Align.RIGHT);
      canvas.drawText(getTitle(), yFrame.right, yFrame.top - paintText.getTextSize() / 2, paintText);
      str = getResources().getString(R.string.chart_days);
      canvas.drawText(str, xFrame.right, xFrame.bottom + paintText.getTextSize() * 1.1f, paintText);

      paintText.setTextAlign(Paint.Align.CENTER);
      for(int i = 0; i < xTicks; i++)
      {
         String day = Integer.toString(xOffset + i + 1);
         canvas.drawText(day, xFrame.left + (i + 0.5f) * x_step, xFrame.bottom - paintText.getTextSize() / 2, paintText);
      }

      paintText.setTextAlign(Paint.Align.RIGHT);
      for(int j = 0; j < yTicks; j++)
      {
         String percent = Integer.toString((yTicks - j - 1) * yStep);
         canvas.drawText(percent, xFrame.left - paintText.getTextSize() / 2,
               yFrame.top + (j + 0.5f) * y_step + paintText.getTextSize() / 2, paintText);
      }

      canvas.restore();
   }
}
