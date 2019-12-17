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
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

/**
 * This view prints a strip placeholder in the screen so that the user can place the strip correctly
 * in the camera view.
 * <p>
 * TODO: Serious misalignment reported in some smartphones. Related to soft keys?
 * (Model: GOOGLE PIXEL XL)
 */
public class StripPlaceholderView extends View
{
   /**
    * Scales a percentage rectangle to the window dimensions.
    *
    * @param r Percentage rectangle, all dimensions in [0, 1].
    * @param w Window width.
    * @param h Window height.
    * @return The rectangle scaled to the window dimensions.
    */
   public static Rect scaleRect(RectF r, int w, int h)
   {
      return new Rect((int) (r.left * w), (int) (r.top * h), (int) (r.right * w), (int) (r.bottom * h));
   }

   /**
    * Extracts the percentage rectangle of a real rectangle and the window dimensions.
    *
    * @param r Rectangle in window dimensions.
    * @param w Window width.
    * @param h Window height.
    * @return Percentage rectangle, all dimensions in [0, 1].
    */
   public static RectF unscaleRect(Rect r, int w, int h)
   {
      return new RectF((float) r.left / w, (float) r.top / h, (float) r.right / w, (float) r.bottom / h);
   }


   /**
    * Placeholder paint.
    */
   Paint paint;

   /**
    * Area of the view in which the strip should be.
    */
   RectF strip;

   /**
    * Position of the control line in the view.
    */
   RectF controlLine;

   /**
    * Area of the view in which the test line should be.
    */
   RectF testLine;

   /**
    * Area of the view in which the base of the strip (white) should be.
    */
   RectF stripBase;

   /**
    * Area of the view in which the handle of the strip should be.
    */
   RectF stripHandle;

   /**
    * Placeholder paint.
    */
   Paint debugPaint;

   /**
    * Rectangle with the guess of the test base area or null if none.
    */
   RectF debugBase;

   /**
    * Rectangle with the guess of the line or null if none.
    */
   RectF debugLine;

   /**
    * Rectangle with the guess of the control line or null if none.
    */
   RectF debugControlLine;


   /**
    * View simplest construction.
    */
   public StripPlaceholderView(Context context)
   {
      this(context, null);
   }

   /**
    * View construction with parameters for XML laying out.
    */
   public StripPlaceholderView(Context context, AttributeSet attrs)
   {
      this(context, attrs, 0);
   }

   /**
    * View construction with parameters for XML laying out.
    */
   public StripPlaceholderView(Context context, AttributeSet attrs, int defStyle)
   {
      super(context, attrs, defStyle);

      paint = new Paint();
      paint.setColor(ContextCompat.getColor(context, R.color.colorPrimary));
      paint.setStrokeWidth(4f);

      debugPaint = new Paint();
      debugPaint.setColor(ContextCompat.getColor(context, R.color.colorAccent));
      debugPaint.setStrokeWidth(4f);
      debugPaint.setStyle(Paint.Style.STROKE);

      setType(Config.PREF_PLACEHOLDER_TYPE_DEFAULT);
   }


   /**
    * Returns the area of the view in which the strip should be.
    */
   Rect getStrip(Bitmap bm)
   {
      return scaleRect(strip, bm.getWidth(), bm.getHeight());
   }

   /**
    * Returns the position of the control line in the view.
    */
   Rect getControlLine(Bitmap bm)
   {
      return scaleRect(controlLine, bm.getWidth(), bm.getHeight());
   }

   /**
    * Returns the area of the view in which the test line should be.
    */
   Rect getTestLine(Bitmap bm)
   {
      return scaleRect(testLine, bm.getWidth(), bm.getHeight());
   }

   /**
    * Returns the area of the view in which the base of the strip (white) should be.
    */
   Rect getStripBase(Bitmap bm)
   {
      return scaleRect(stripBase, bm.getWidth(), bm.getHeight());
   }

   /**
    * Returns the area of the view in which the handle of the strip should be.
    */
   Rect getStripHandle(Bitmap bm)
   {
      return scaleRect(stripHandle, bm.getWidth(), bm.getHeight());
   }


   /**
    * Sets the rectangle with the guess of the test base area.
    */
   void setDebugBase(Rect r, Bitmap bm)
   {
      debugBase = unscaleRect(r, bm.getWidth(), bm.getHeight());
      invalidate();
   }

   /**
    * Sets the rectangle with the guess of the line.
    */
   void setDebugLine(Rect r, Bitmap bm)
   {
      debugLine = unscaleRect(r, bm.getWidth(), bm.getHeight());
      invalidate();
   }

   /**
    * Sets the rectangle with the guess of the control line.
    */
   void setDebugControlLine(Rect r, Bitmap bm)
   {
      debugControlLine = unscaleRect(r, bm.getWidth(), bm.getHeight());
      invalidate();
   }

   /**
    * Sets the placeholder type and updates the view.
    *
    * @param type number 1 to 3.
    */
   void setType(int type)
   {
      float w;
      float l;
      switch(type)
      {
         case 1:
            w = 0.06f;
            l = 0.50f;
            break;
         case 2:
            w = 0.08f;
            l = 0.60f;
            break;
         case 3:
         default:
            w = 0.10f;
            l = 0.70f;
            break;
      }

      float pad = (0.90f - l) / 2;
      strip = new RectF(pad, 0.50f - w / 2, pad + l, 0.50f + w / 2);
      controlLine = new RectF(pad + l * 0.54f, strip.top, pad + l * 0.57f, strip.bottom);
      testLine = new RectF(pad + l * 0.37f, strip.top, pad + l * 0.50f, strip.bottom);
      stripBase = new RectF(testLine.right, strip.top, controlLine.left, strip.bottom);
      stripHandle = new RectF(pad + l * 0.67f, strip.top, strip.right, strip.bottom);

      invalidate();
   }

   /**
    * Draw the view in the screen.
    */
   @Override
   protected void onDraw(Canvas canvas)
   {
      int w = canvas.getWidth();
      int h = canvas.getHeight();

      paint.setStyle(Paint.Style.STROKE);
      canvas.drawRect(scaleRect(strip, w, h), paint);

      canvas.drawRect(scaleRect(controlLine, w, h), paint);

      Rect r = scaleRect(testLine, w, h);
      int sep = (r.bottom - r.top) / 3;
      int bracket = (r.bottom - r.top) / 4;
      int corner = (int) (paint.getStrokeWidth() / 2);
      canvas.drawLine(r.left, r.top - sep - corner, r.left, r.bottom + sep + corner, paint);
      canvas.drawLine(r.left - corner, r.bottom + sep, r.left + bracket, r.bottom + sep, paint);
      canvas.drawLine(r.left - corner, r.top - sep, r.left + bracket, r.top - sep, paint);
      canvas.drawLine(r.right, r.top - sep - corner, r.right, r.bottom + sep + corner, paint);
      canvas.drawLine(r.right + corner, r.bottom + sep, r.right - bracket, r.bottom + sep, paint);
      canvas.drawLine(r.right + corner, r.top - sep, r.right - bracket, r.top - sep, paint);

      paint.setStyle(Paint.Style.FILL);
      canvas.drawRect(scaleRect(stripHandle, w, h), paint);

      // Debug information
      if(debugBase != null)
         canvas.drawRect(scaleRect(debugBase, w, h), debugPaint);
      if(debugLine != null)
         canvas.drawRect(scaleRect(debugLine, w, h), debugPaint);
      if(debugControlLine != null)
         canvas.drawRect(scaleRect(debugControlLine, w, h), debugPaint);
   }
}
