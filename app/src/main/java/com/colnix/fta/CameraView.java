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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Camera preview widget which can take frames for processing or saving.
 * <p>
 * Uses the android.hardware.Camera class deprecated since API 21 to interface with the camera.
 */
@SuppressWarnings("deprecation")
public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback
{
   static final String TAG = "CameraView";

   /**
    * Frame rate in milliseconds.
    */
   static final int FRAME_RATE_MS = 500;

   /**
    * Zoom upper limit to keep it usable.
    */
   static final int MAXIMUM_ZOOM = 25;

   /**
    * Auto focus box size.
    */
   static final int AUTOFOCUS_SIZE = 100;


   /**
    * Drawing surface accessing holder.
    */
   SurfaceHolder holder;

   /**
    * Camera interface.
    */
   Camera camera;

   /**
    * Maximum zoom level supported by the camera.
    */
   int maxZoomLevel;

   /**
    * Current zoom level in the camera.
    */
   int zoomLevel;

   /**
    * Activity that listens to the camera frames or null.
    */
   NewTestActivity activity;

   /**
    * Manual focus area indicator or null.
    */
   ManualFocusView focusIndicator;

   /**
    * Time of the last frame processed.
    */
   long lastFrame;

   /**
    * Take picture on next frame flag.
    */
   boolean takePicture;

   /**
    * If the torch must be on.
    */
   boolean torch;


   /**
    * View simplest construction.
    */
   public CameraView(Context context)
   {
      this(context, null);
   }

   /**
    * View construction with parameters for XML laying out.
    */
   public CameraView(Context context, AttributeSet attrs)
   {
      this(context, attrs, 0);
   }

   /**
    * View construction with parameters for XML laying out.
    */
   public CameraView(Context context, AttributeSet attrs, int defStyle)
   {
      super(context, attrs, defStyle);

      holder = getHolder();
      holder.addCallback(this);
      takePicture = false;
      torch = false;
   }


   /**
    * Sets up the best camera configuration for the view.
    */
   protected void setupCamera(int width, int height)
   {
      float viewRatio = width / (float) height;
      Log.d(TAG, "setupCamera: " + width + "x" + height + " (" + viewRatio + ")");

      Camera.Parameters params = camera.getParameters();

      // Resolution

      if(viewRatio < 1.0f)
      {
         viewRatio = 1 / viewRatio;
         int temp = width;
         width = height;
         height = temp;
      }
      Camera.Size bestSize = null;
      float bestRatio = Float.MAX_VALUE;
      for(Camera.Size size : params.getSupportedPreviewSizes())
      {
         float ratio = size.width / (float) size.height;
         //Log.d(TAG, "   Detected camera size: " + size.width + "x" + size.height + " (" + ratio + ")");

         if(width == size.width && height == size.height)
         {
            // Exact match, stop search
            bestRatio = ratio;
            bestSize = size;
            break;
         }
         else if(bestSize == null)
         {
            // First available
            bestRatio = ratio;
            bestSize = size;
         }
         else if(size.width < width || size.height < height)
         {
            // Ignore small resolutions
         }
         else if(ratio == bestRatio)
         {
            // Same image ratio
            if(width <= bestSize.width && height <= bestSize.height)
            {
               // Image bigger than current best resolution
               if(width <= size.width && height <= size.height)
               {
                  // This resolution fits too, so choose the smaller
                  if(size.width < bestSize.width)
                  {
                     bestRatio = ratio;
                     bestSize = size;
                  }
               }
               else
               {
                  // This resolution does not fit, so stick to the other
               }
            }
            else
            {
               // Image is smaller than current best resolution, so choose the bigger of both
               if(size.width > bestSize.width)
               {
                  bestRatio = ratio;
                  bestSize = size;
               }
            }
         }
         else if(Math.abs(viewRatio - ratio) < Math.abs(viewRatio - bestRatio))
         {
            // Choose the ratio closer to the view
            bestRatio = ratio;
            bestSize = size;
         }
      }
      if(bestSize == null)
         throw new RuntimeException("No valid preview size found.");

      Log.i(TAG, "Selected camera image size: " + bestSize.width + "x" + bestSize.height + " (" + bestRatio + ")");
      params.setPreviewSize(bestSize.width, bestSize.height);


      // Format
      if(params.getPreviewFormat() != ImageFormat.NV21 && params.getPreviewFormat() != ImageFormat.YUY2)
      {
         List<Integer> formats = params.getSupportedPreviewFormats();
         if(formats.contains(ImageFormat.NV21))
         {
            // Documentation of getSupportedPreviewFormats() states that NV21 should be always supported.
            params.setPreviewFormat(ImageFormat.NV21);
         }
         else if(formats.contains(ImageFormat.JPEG))
         {
            params.setPreviewFormat(ImageFormat.JPEG);
         }
         else
         {
            String error = "No preview format supported found (";
            for(int fmt : formats)
               error += fmt + ",";
            error += ")";
            throw new RuntimeException(error);
         }
      }

      // Auto focus
      List<String> support = params.getSupportedFocusModes();
      if(support == null)
      {
         // No autofocus
      }
      else if(support.contains(Camera.Parameters.FOCUS_MODE_MACRO))
      {
         params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
      }
      else if(support.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
      {
         params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
      }
      else if(support.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
      {
         params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
      }

      // Auto flash
      support = params.getSupportedFlashModes();
      SharedPreferences prefs = Config.getPrefs(getContext());
      if(support == null)
      {
         // No flash
      }
      else if(torch && support.contains(Camera.Parameters.FLASH_MODE_TORCH))
      {
         params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
      }
      else if(prefs.getBoolean(Config.PREF_FLASH_ON_FOCUS, true) && support.contains(Camera.Parameters.FLASH_MODE_AUTO))
      {
         params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
      }
      else if(support.contains(Camera.Parameters.FLASH_MODE_OFF))
      {
         params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
      }

      // Zoom
      maxZoomLevel = 0;
      zoomLevel = 0;
      if(params.isZoomSupported())
      {
         maxZoomLevel = params.getMaxZoom();
         if(maxZoomLevel > MAXIMUM_ZOOM)
            maxZoomLevel = MAXIMUM_ZOOM;
         params.setZoom(zoomLevel);
      }

      camera.setParameters(params);
   }

   /**
    * Opens and configures the camera.
    */
   void openCamera(int format, int width, int height)
   {
      try
      {
         camera = Camera.open();
      }
      catch(Exception e)
      {
         camera = null;
      }

      if(camera == null)
      {
         // No camera facing back, try the default camera
         camera = Camera.open(0);
      }
      if(camera == null)
         throw new NullPointerException();

      setupCamera(width, height);
   }

   /**
    * Closes the camera if needed.
    */
   void closeCamera()
   {
      if(camera != null)
      {
         try
         {
            camera.setPreviewCallback(null);

            if(focusIndicator != null && focusIndicator.isVisible())
            {
               camera.cancelAutoFocus();
            }
         }
         catch(Exception e)
         {
         }

         try
         {
            camera.stopPreview();
            camera.release();
         }
         catch(Exception e)
         {
         }
         camera = null;
      }
   }

   /**
    * Sets the activity that listens to the camera frames.
    */
   void setActivity(NewTestActivity act, ManualFocusView focus)
   {
      activity = act;
      focusIndicator = focus;
   }


   /**
    * This is called immediately after the surface is first created.
    * Implementations of this should start up whatever rendering code
    * they desire.
    *
    * @param holder The SurfaceHolder whose surface is being created.
    */
   @Override
   public void surfaceCreated(SurfaceHolder holder)
   {
      // Creation delayed to surfaceChanged() as it is guaranteed to be called with the correct
      // viewport parameters.
   }

   /**
    * This is called immediately after any structural changes (format or
    * size) have been made to the surface. You should at this point update
    * the imagery in the surface. This method is always called at least
    * once, after {@link #surfaceCreated}.
    *
    * @param holder The SurfaceHolder whose surface has changed.
    * @param format The new PixelFormat of the surface.
    * @param width  The new width of the surface.
    * @param height The new height of the surface.
    */
   @Override
   public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
   {
      if(isInEditMode())
         return;

      closeCamera();

      if(!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
      {
         Toast.makeText(getContext(), R.string.camera_not_found, Toast.LENGTH_LONG).show();
         return;
      }

      try
      {
         openCamera(format, width, height);

         camera.setPreviewDisplay(holder);
         camera.setPreviewCallback(this);
         camera.startPreview();

         if(activity != null)
            activity.onCameraReady();
      }
      catch(Exception e)
      {
         closeCamera();

         Log.e(TAG, "Error creating the camera.", e);

         Toast.makeText(getContext(), R.string.camera_access_error, Toast.LENGTH_LONG).show();
      }
   }

   /**
    * This is called immediately before a surface is being destroyed. After
    * returning from this call, you should no longer try to access this
    * surface.
    *
    * @param holder The SurfaceHolder whose surface is being destroyed.
    */
   @Override
   public void surfaceDestroyed(SurfaceHolder holder)
   {
      closeCamera();
   }


   /**
    * Takes a picture on the next frame.
    */
   void takePicture()
   {
      if(camera == null || activity == null)
         return;

      takePicture = true;
      camera.setOneShotPreviewCallback(this);
   }

   /**
    * Checks for torch support.
    *
    * @return true if supported.
    */
   boolean supportsTorch()
   {
      return getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
   }

   /**
    * Enables or disables the torch mode which turns the flash light on permanently.
    */
   void setTorch(boolean enabled)
   {
      torch = enabled;

      if(camera != null)
      {
         try
         {
            Camera.Parameters params = camera.getParameters();
            List<String> modes = params.getSupportedFlashModes();
            if(modes == null)
               return;

            if(torch)
            {
               if(!modes.contains(Camera.Parameters.FLASH_MODE_TORCH))
                  return;

               params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
               camera.setParameters(params);
            }
            else
            {
               if(!modes.contains(Camera.Parameters.FLASH_MODE_OFF))
                  return;

               params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
               camera.setParameters(params);

               SharedPreferences prefs = Config.getPrefs(getContext());
               if(modes.contains(Camera.Parameters.FLASH_MODE_AUTO) && prefs.getBoolean(Config.PREF_FLASH_ON_FOCUS, true))
               {
                  params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                  camera.setParameters(params);
               }
            }
         }
         catch(Exception e)
         {
            Log.e(TAG, "Error setting the torch.", e);

            Toast.makeText(getContext(), R.string.camera_access_error, Toast.LENGTH_LONG).show();
         }
      }
   }


   /**
    * Returns the maximum zoom level supported by the camera.
    */
   int getMaxZoom()
   {
      return maxZoomLevel;
   }

   /**
    * Returns the current zoom level of the camera.
    */
   int getZoom()
   {
      return zoomLevel;
   }

   /**
    * Sets the camera zoom level.
    */
   void setZoom(int value)
   {
      if(camera == null || maxZoomLevel == 0)
         return;

      zoomLevel = value;
      if(zoomLevel < 0)
         zoomLevel = 0;
      else if(zoomLevel > maxZoomLevel)
         zoomLevel = maxZoomLevel;

      try
      {
         Camera.Parameters params = camera.getParameters();
         params.setZoom(zoomLevel);
         camera.setParameters(params);
      }
      catch(Exception e)
      {
         Log.e(TAG, "Error setting the zoom.", e);
      }
   }


   /**
    * On touch down, focus the camera around the touched area.
    */
   @Override
   public boolean onTouchEvent(MotionEvent event)
   {
      if(event.getAction() != MotionEvent.ACTION_DOWN || camera == null || focusIndicator == null)
         return false;

      if(focusIndicator.isVisible())
      {
         try
         {
            camera.cancelAutoFocus();
         }
         catch(Exception e)
         {
         }
      }

      int w = getWidth();
      int h = getHeight();
      int x = (int) event.getX() - AUTOFOCUS_SIZE / 2;
      int y = (int) event.getY() - AUTOFOCUS_SIZE / 2;
      if(x < 0)
      {
         x = 0;
      }
      else if(x > w - AUTOFOCUS_SIZE)
      {
         x = w - AUTOFOCUS_SIZE;
      }
      if(y < 0)
      {
         y = 0;
      }
      else if(y > h - AUTOFOCUS_SIZE)
      {
         y = h - AUTOFOCUS_SIZE;
      }
      Rect touch = new Rect(x, y, x + AUTOFOCUS_SIZE, y + AUTOFOCUS_SIZE);

      int weight = 1000;
      Rect focus_area = new Rect((touch.left * 2 * weight) / w - weight, (touch.top * 2 * weight) / h - weight, (touch.right * 2 * weight) / w - weight, (touch.bottom * 2 * weight) / h - weight);

      List<Camera.Area> focusList = new ArrayList<>();
      focusList.add(new Camera.Area(focus_area, weight));

      try
      {
         Camera.Parameters params = camera.getParameters();
         if(params.getMaxNumFocusAreas() < 1)
            return true;
         params.setFocusAreas(focusList);
         if(params.getMaxNumMeteringAreas() > 0)
            params.setMeteringAreas(focusList);
         List<String> modes = params.getSupportedFocusModes();
         if(modes == null || !modes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
            return true;
         params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

         camera.setParameters(params);
      }
      catch(Exception e)
      {
         return true;
      }
      focusIndicator.setFocusArea(touch);

      try
      {
         camera.autoFocus(new Camera.AutoFocusCallback()
         {
            @Override
            public void onAutoFocus(boolean success, Camera camera)
            {
               if(focusIndicator != null)
                  focusIndicator.setFocusArea(null);

               if(camera == null)
                  return;

               try
               {
                  camera.cancelAutoFocus();

                  Camera.Parameters params = camera.getParameters();
                  params.setFocusAreas(null);
                  if(params.getMaxNumMeteringAreas() > 0)
                     params.setMeteringAreas(null);

                  List<String> support = params.getSupportedFocusModes();
                  if(support.contains(Camera.Parameters.FOCUS_MODE_FIXED))
                     params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                  else if(support.contains(Camera.Parameters.FOCUS_MODE_MACRO))
                     params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                  else if(support.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
                     params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                  else if(support.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                     params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

                  camera.setParameters(params);
               }
               catch(Exception e)
               {
               }
            }
         });
      }
      catch(Exception e)
      {
         return true;
      }

      return true;
   }

   /**
    * Called as preview frames are displayed.  This callback is invoked
    * on the event thread open(int) was called from.
    *
    * <p>If using the {@link ImageFormat#YV12} format,
    * refer to the equations in {@link Camera.Parameters#setPreviewFormat}
    * for the arrangement of the pixel data in the preview callback
    * buffers.
    *
    * @param data   the contents of the preview frame in the format defined
    *               by {@link ImageFormat}, which can be queried
    *               with {@link Camera.Parameters#getPreviewFormat()}.
    *               If {@link Camera.Parameters#setPreviewFormat(int)}
    *               is never called, the default will be the YCbCr_420_SP
    *               (NV21) format.
    * @param camera the Camera service object.
    */
   @Override
   public void onPreviewFrame(byte[] data, Camera camera)
   {
      if(activity == null)
         return;

      long now = System.currentTimeMillis();
      if(!takePicture && lastFrame != 0 && now < lastFrame + FRAME_RATE_MS)
         return;
      lastFrame = now;

      int width, height, format;
      try
      {
         Camera.Parameters params = camera.getParameters();
         width = params.getPreviewSize().width;
         height = params.getPreviewSize().height;
         format = params.getPreviewFormat();
      }
      catch(Exception e)
      {
         return;
      }
      byte[] jpeg_data;

      if(format == ImageFormat.JPEG)
      {
         jpeg_data = data;
      }
      else
      {
         YuvImage yuvImage = new YuvImage(data, format, width, height, null);
         ByteArrayOutputStream os = new ByteArrayOutputStream();
         yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, os);
         jpeg_data = os.toByteArray();
      }

      Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg_data, 0, jpeg_data.length);

      if(Config.PreviewInverted())
      {
         Matrix matrix = new Matrix();
         matrix.preScale(-1.0f, -1.0f);
         bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

         ByteArrayOutputStream os = new ByteArrayOutputStream();
         bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
         jpeg_data = os.toByteArray();
      }

      if(takePicture)
      {
         if(activity.onPictureTaken(bitmap, jpeg_data))
         {
            camera.setPreviewCallback(this);
            camera.startPreview();
         }
         takePicture = false;
      }
      else
      {
         activity.onPreviewFrame(bitmap, jpeg_data);
      }
   }
}
