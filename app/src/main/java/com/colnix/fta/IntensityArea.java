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

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;

/**
 * This class hold the map of intensities of an area.
 */
public class IntensityArea
{
   /**
    * Area width.
    */
   protected int width;

   /**
    * Area height.
    */
   protected int height;

   /**
    * Area intensities.
    */
   protected float[] intensity;

   /**
    * Rectangle coordinates on the original bitmatp.
    */
   protected Rect rect;

   /**
    * The average intensity of the area from 0.0 (white) to 1.0 (black).
    */
   protected float average;

   /**
    * The delta or variation of the intensity in the area form -1.0 to +1.0.
    */
   protected float delta;

   /**
    * The point of maximum intensity in local coordinates.
    */
   protected Point maximum;

   /**
    * The point of minimum intensity in local coordinates.
    */
   protected Point minimum;


   /**
    * Extracts the intensity information from a bitmap rectangle.
    */
   public IntensityArea(Bitmap bitmap, Rect rect)
   {
      width = rect.width();
      height = rect.height();
      this.rect = new Rect(rect);
      if(rect.left < 0 || width <= 0 || rect.top < 0 || height <= 0 || width > bitmap.getWidth() || height > bitmap.getHeight())
         throw new ArrayIndexOutOfBoundsException();

      intensity = new float[width * height];
      for(int y = 0; y < height; y++)
      {
         int index = y * width;
         for(int x = 0; x < width; x++)
         {
            int pixel = bitmap.getPixel(rect.left + x, rect.top + y);
            int color = Color.red(pixel) + Color.green(pixel) + Color.blue(pixel);
            float intens = (float) color / (3 * 256);
            intensity[index + x] = 1.0f - intens;
         }
      }

      maximum = new Point();
      minimum = new Point();

      process();
   }


   /**
    * Returns the area width.
    */
   public int getWidth()
   {
      return width;
   }

   /**
    * Returns the area height.
    */
   public int getHeight()
   {
      return height;
   }

   /**
    * Returns the intensity of a point of the area.
    */
   public float get(int x, int y)
   {
      if(x < 0 || x >= width || y < 0 || y >= height)
         throw new ArrayIndexOutOfBoundsException();

      return intensity[y * width + x];
   }

   /**
    * Returns the intensity of a point of the area.
    */
   public float get(Point point)
   {
      return get(point.x, point.y);
   }

   /**
    * Returns the rectangle coordinates on the original bitmap.
    */
   public Rect getRect()
   {
      return rect;
   }


   /**
    * Removes margins from the area.
    *
    * @returns false if the margins cannot be applied.
    */
   public boolean margin(int left, int top, int right, int bottom)
   {
      return margin(new Rect(left, top, right, bottom));
   }

   /**
    * Removes margins from the area.
    *
    * @returns false if the margins cannot be applied.
    */
   public boolean margin(Rect r)
   {
      int width2 = width - r.left - r.right;
      int height2 = height - r.top - r.bottom;
      if(r.left < 0 || r.top < 0 || r.right < 0 || r.bottom < 0 || width2 <= 0 || height2 <= 0)
         return false;

      if(r.left > 0 || r.top > 0 || r.right > 0)
      {
         for(int y = 0; y < height2; y++)
         {
            int index2 = y * width2;
            int index = (r.top + y) * width;
            for(int x = 0; x < width2; x++)
            {
               intensity[index2 + x] = intensity[index + (r.left + x)];
            }
         }
      }

      width = width2;
      height = height2;
      rect.set(rect.left + r.left, rect.top + r.top, rect.right - r.right, rect.bottom - r.bottom);

      process();

      return true;
   }


   /**
    * Calculates the measurements of the area.
    */
   protected void process()
   {
      float max = Float.MIN_VALUE;
      float min = Float.MAX_VALUE;
      float accum = 0.0f;
      int count = 0;

      for(int y = 0; y < height; y++)
      {
         int index = y * width;
         for(int x = 0; x < width; x++)
         {
            float intens = intensity[index + x];

            if(intens > max)
            {
               max = intens;
               maximum.set(x, y);
            }

            if(intens < min)
            {
               min = intens;
               minimum.set(x, y);
            }

            accum += intens;
            count++;
         }
      }

      delta = max - min;
      average = accum / count;
   }

   /**
    * Returns the average intensity of the area from 0.0 (white) to 1.0 (black).
    */
   public float getAverage()
   {
      return average;
   }

   /**
    * Returns the delta or variation of the intensity in the area form -1.0 to +1.0.
    */
   public float getDelta()
   {
      return delta;
   }

   /**
    * Returns the point of maximum intensity in local coordinates.
    */
   public Point getMaximum()
   {
      return new Point(maximum);
   }

   /**
    * Returns the maximum intensity of the area.
    */
   public float getMaximumIntensity()
   {
      return get(maximum);
   }

   /**
    * Returns the point of minimum intensity in local coordinates.
    */
   public Point getMinimum()
   {
      return new Point(minimum);
   }

   /**
    * Returns the minimumintensity of the area.
    */
   public float getMinimumIntensity()
   {
      return get(minimum);
   }

   /**
    * Returns the raw buffer of the area intensities.
    * Should be accessed as: intensity[y * width + x];
    */
   public float[] getIntensityBuffer()
   {
      return intensity;
   }

   /**
    * Returns the area in pixels square,
    */
   public int getArea()
   {
      return width * height;
   }
}
