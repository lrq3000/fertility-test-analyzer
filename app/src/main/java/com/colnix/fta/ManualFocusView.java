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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

/**
 * Manual focus working indicator.
 */
public class ManualFocusView extends View
{
   /**
    * Indicator brackets length in per one.
    */
   public static final float BRACKET_PERCENT = 0.20f;


   /**
    * Indicator paint.
    */
   Paint paint;

   /**
    * Focus area to highlight or null if none.
    */
   Rect focusArea;


   /**
    * View simplest construction.
    */
   public ManualFocusView(Context context)
   {
      this(context, null);
   }

   /**
    * View construction with parameters for XML laying out.
    */
   public ManualFocusView(Context context, AttributeSet attrs)
   {
      this(context, attrs, 0);
   }

   /**
    * View construction with parameters for XML laying out.
    */
   public ManualFocusView(Context context, AttributeSet attrs, int defStyle)
   {
      super(context, attrs, defStyle);

      paint = new Paint();
      paint.setColor(ContextCompat.getColor(context, R.color.colorAccent));
      paint.setStrokeWidth(4f);
      paint.setStyle(Paint.Style.STROKE);

      focusArea = null;
   }


   /**
    * Sets the focus area to highlight or null to hide.
    */
   public void setFocusArea(Rect area)
   {
      focusArea = area;
      invalidate();
   }

   /**
    * Returns true if the focus ares is set and visible.
    */
   public boolean isVisible()
   {
      return (focusArea == null);
   }


   /**
    * Draw the view in the screen.
    */
   @Override
   protected void onDraw(Canvas canvas)
   {
      if(focusArea == null)
         return;

      //canvas.drawRect(focusArea, paint);

      int vert_len = (int) (focusArea.height() * BRACKET_PERCENT);
      int hor_len = (int) (focusArea.width() * BRACKET_PERCENT);
      int corner = (int) (paint.getStrokeWidth() / 2);

      canvas.drawLine(focusArea.left, focusArea.top + vert_len, focusArea.left, focusArea.top - corner, paint);
      canvas.drawLine(focusArea.left - corner, focusArea.top, focusArea.left + hor_len, focusArea.top, paint);

      canvas.drawLine(focusArea.left, focusArea.bottom - vert_len, focusArea.left, focusArea.bottom + corner, paint);
      canvas.drawLine(focusArea.left - corner, focusArea.bottom, focusArea.left + hor_len, focusArea.bottom, paint);

      canvas.drawLine(focusArea.right - hor_len, focusArea.bottom, focusArea.right + corner, focusArea.bottom, paint);
      canvas.drawLine(focusArea.right, focusArea.bottom + corner, focusArea.right, focusArea.bottom - vert_len, paint);

      canvas.drawLine(focusArea.right, focusArea.top + vert_len, focusArea.right, focusArea.top - corner, paint);
      canvas.drawLine(focusArea.right + corner, focusArea.top, focusArea.right - hor_len, focusArea.top, paint);
   }
}
