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

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Strip test photo verification activity.
 *
 * Shows the picture taken of a strip and lets the user verify that it is correctly placed.
 */
public class TestVerificationActivity extends AppCompatActivity
{
   //static final String TAG = "TestVerification";

   static final int REQUEST_PERM = 1;
   static final int REQUEST_TESTEDIT = 2;

   /**
    * Tolerance of the base delta.
    */
   static final float MAX_BASE_DELTA = 0.10f;

   /**
    * Minimum allowed height for a valid base color as a percent (per one) of the base.
    */
   static final float MIN_BASE_HEIGHT_PER = 0.45f;

   /**
    * Minimum allowed height for a valid base color.
    */
   static final int MIN_BASE_HEIGHT = 16;

   /**
    * Minimum allowed width for a valid base color as a percent (per one) of the base.
    */
   static final float MIN_BASE_WIDTH_PER = 0.45f;

   /**
    * Minimum allowed width for a valid base color.
    */
   static final int MIN_BASE_WIDTH = 12;

   /**
    * Base area auto crop square half side in which average the center.
    * 0 means no average, 1 means a 3x3 pixel square, 2 means 5x5....
    */
   static final int MAX_BASE_CENTER = 1;

   /**
    * Step of base margin removal.
    */
   static final int STEP_BASE_MARGIN = 2;

   /**
    * Minimum allowed height for a valid control line color as a percent (per one) of the base.
    */
   static final float MIN_CONTROL_HEIGHT_PER = 0.50f;

   /**
    * Minimum allowed height for a valid control line color.
    */
   static final int MIN_CONTROL_HEIGHT = 16;

   /**
    * Minimum allowed width for a valid control line color as a percent (per one) of the base..
    */
   static final float MIN_CONTROL_WIDTH_PER = 0.50f;

   /**
    * Minimum allowed width for a valid control line color.
    */
   static final int MIN_CONTROL_WIDTH = 6;

   /**
    * Control line auto crop square half side in which average the center.
    * 0 means no average, 1 means a 3x3 pixel square, 2 means 5x5....
    */
   static final int MAX_CONTROL_CENTER = 1;

   /**
    * Control line auto crop intensity delta.
    */
   static final float MAX_CONTROL_DELTA = 0.10f;

   /**
    * Control line total allowed intensity delta.
    */
   static final float MAX_CONTROL_TOTAL_DELTA = 0.20f;

   /**
    * Minimum delta between base and control line.
    */
   static final float MIN_CONTROL_CONTRAST = 0.05f;

   /**
    * Minimum delta between base and handle line.
    */
   static final float MIN_HANDLE_CONTRAST = 0.10f;

   /**
    * Minimum delta between base and control line for preview.
    */
   static final float MIN_CONTROL_CONTRAST_PREVIEW = 0.03f;

   /**
    * Minimum delta between base and handle line for preview.
    */
   static final float MIN_HANDLE_CONTRAST_PREVIEW = 0.08f;

   /**
    * Minimum allowed height for a valid test line color as a percent (per one) of the base.
    */
   static final float MIN_LINE_HEIGHT_PER = 0.50f;

   /**
    * Minimum allowed height for a test control line color.
    */
   static final int MIN_LINE_HEIGHT = 16;

   /**
    * Percentage of the width to use as lateral margins in the auto crop.
    */
   static final float LINE_CROP_LATERAL_PER = 0.10f;

   /**
    * Test line auto crop square half side in which average the center.
    * 0 means no average, 1 means a 3x3 pixel square, 2 means 5x5....
    */
   static final int MAX_LINE_CENTER = 1;

   /**
    * Test line auto crop intensity delta.
    */
   static final float MAX_LINE_DELTA = 0.10f;

   /**
    * Test line minimum intensity delta for peak-shape filter.
    */
   static final float MIN_LINE_DELTA = 0.004f;


   /**
    * Image bitmap.
    */
   static Bitmap intentBitmap;

   /**
    * JPEG data or null if the image doesn't have to the saved.
    */
   static byte[] intentJpegData;

   /**
    * Stores the image data which cannot be passed into the intent because of its size.
    * USE WITH CARE!
    */
   static void setImage(Bitmap bitmap, byte[] jpeg)
   {
      intentBitmap = bitmap;
      intentJpegData = jpeg;
   }

   /**
    * Runs a quick validation on the image to assist the user with the photo capture.
    *
    * @return A warning message or -1 if everything looks ok.
    */
   static int checkPreview(Bitmap bitmap, StripPlaceholderView placeholder)
   {
      IntensityArea base = new IntensityArea(bitmap, placeholder.getStripBase(bitmap));

      IntensityArea control = new IntensityArea(bitmap, placeholder.getControlLine(bitmap));
      if(control.getAverage() < base.getAverage() + MIN_CONTROL_CONTRAST_PREVIEW)
         return R.string.tverific_warn_control_contrast;

      IntensityArea handle = new IntensityArea(bitmap, placeholder.getStripHandle(bitmap));
      if(handle.getAverage() < base.getAverage() + MIN_HANDLE_CONTRAST_PREVIEW)
         return R.string.tverific_warn_handle_contrast;

      return -1;
   }


   /**
    * Image preview control.
    */
   ImageView image;

   /**
    * Strip placeholder control.
    */
   StripPlaceholderView placeholder;

   /**
    * Results text box.
    */
   TextView msgOk;

   /**
    * Error or warning text box.
    */
   TextView msgError;

   /**
    * Error message layout.
    */
   View msgLayout;

   /**
    * Image bitmap.
    */
   Bitmap bitmap;

   /**
    * JPEG data or null if the image don't have to the saved.
    */
   byte[] jpegData;

   /**
    * Pigmentation level detected.
    * A negative value means that it was an error in the process.
    */
   int pigment;

   /**
    * If the calculation was done in low resolution mode.
    */
   boolean low_res;

   /**
    * Detection error code.
    */
   String errorCode;

   /**
    * True if the result is valid and false if the detection failed.
    */
   boolean result;


   /**
    * Called when the activity is starting. This is where most initialization should go: calling
    * setContentView(int) to inflate the activity's UI, using findViewById(int) to programmatically
    * interact with widgets in the UI
    *
    * @param savedInstanceState
    */
   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
      getWindow().setFormat(PixelFormat.TRANSLUCENT);
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_test_verification);

      image = (ImageView) findViewById(R.id.image);

      placeholder = (StripPlaceholderView) findViewById(R.id.placeholder);

      SharedPreferences prefs = Config.getPrefs(this);
      int ph_type = prefs.getInt(Config.PREF_PLACEHOLDER_TYPE, Config.PREF_PLACEHOLDER_TYPE_DEFAULT);
      placeholder.setType(ph_type);

      msgOk = (TextView) findViewById(R.id.msg_ok);
      msgOk.setVisibility(View.INVISIBLE);

      msgLayout = (View) findViewById(R.id.msg_layout);
      msgLayout.setVisibility(View.INVISIBLE);
      msgError = (TextView) findViewById(R.id.msg_error);

      bitmap = intentBitmap;
      jpegData = intentJpegData;
      if(bitmap == null)
      {
         finish();
         return;
      }
      image.setImageBitmap(bitmap);

      final CheckBox check_save = (CheckBox) findViewById(R.id.check_save);
      check_save.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
         @Override
         public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked)
         {
            if(isChecked)
               check_save.setText(R.string.cam_save_on);
            else
               check_save.setText(R.string.cam_save_off);
         }
      });

      check_save.setChecked(prefs.getBoolean(Config.PREF_SAVE_PHOTO, true));

      ImageButton btn = (ImageButton) findViewById(R.id.btn_cancel);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            onBackPressed();
         }
      });

      final ImageButton btn_ok = (ImageButton) findViewById(R.id.btn_ok);
      btn_ok.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            createTest();

            SharedPreferences prefs = Config.getPrefs(TestVerificationActivity.this);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt(Config.PREF_TEST_ATTEMPTS, 0);

            edit.apply();
         }
      });

      final ImageButton btn_low_res = (ImageButton) findViewById(R.id.btn_low_res);
      btn_low_res.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View view)
         {
            low_res = true;
            result = processLow();

            btn_low_res.setVisibility(View.GONE);
            btn_ok.setVisibility(View.VISIBLE);
            btn_ok.setEnabled(result);
         }
      });

      // Process the image
      low_res = false;
      result = process();

      if(result)
      {
         btn_ok.setVisibility(View.VISIBLE);
         btn_ok.setEnabled(true);
         btn_low_res.setVisibility(View.GONE);
      }
      else
      {
         btn_ok.setVisibility(View.GONE);
         btn_ok.setEnabled(false);
         btn_low_res.setVisibility(View.VISIBLE);

         if(prefs.getBoolean(Config.PREF_FIRST_LOW_RES, true))
         {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FtaAlertDialogStyle);
            builder.setTitle(R.string.cam_low_res_title);
            View body = getLayoutInflater().inflate(R.layout.dialog_low_res, null);
            builder.setView(body);

            final AlertDialog dlg = builder.create();
            ImageButton ib = (ImageButton)body.findViewById(R.id.btn_recalculate);
            ib.setEnabled(false);
            Button button = (Button)body.findViewById(R.id.btn_ok);
            final CheckBox cb = (CheckBox)body.findViewById(R.id.check_dont_repeat);
            button.setOnClickListener(new View.OnClickListener()
            {
               @Override
               public void onClick(View view)
               {
                  if(cb.isChecked())
                  {
                     SharedPreferences prefs = Config.getPrefs(TestVerificationActivity.this);
                     prefs.edit().putBoolean(Config.PREF_FIRST_LOW_RES, false).apply();
                  }

                  dlg.dismiss();
               }
            });

            dlg.show();
         }
      }
   }


   /**
    * Checks if a tip is necessary and sets it to be shown next time.
    */
   public void checkShowTips(SharedPreferences prefs, SharedPreferences.Editor edit, int attempts)
   {
      int count = prefs.getInt(Config.PREF_CAMERA_WIZARD_COUNT, 0);
      if(count < Config.CONST_ATTEMPTS_CAMERA_WIZARD.length)
      {
         int trigger = Config.CONST_ATTEMPTS_CAMERA_WIZARD[count];
         if(attempts == trigger)
         {
            edit.putBoolean(Config.PREF_SHOW_CAMERA_WIZARD, true);
            edit.putInt(Config.PREF_CAMERA_WIZARD_COUNT, count + 1);
            edit.putInt(Config.PREF_TEST_ATTEMPTS, 0);
            return;
         }
      }
   }

   /**
    * Called when the activity has detected the user's press of the back key.
    */
   @Override
   public void onBackPressed()
   {
      SharedPreferences prefs = Config.getPrefs(this);
      SharedPreferences.Editor edit = prefs.edit();

      // The user has not accepted the result, so the attempt is considered as failed.
      int attempts = prefs.getInt(Config.PREF_TEST_ATTEMPTS, 0);
      attempts++;
      edit.putInt(Config.PREF_TEST_ATTEMPTS, attempts);

      // Tips required?
      checkShowTips(prefs, edit, attempts);

      edit.apply();

      super.onBackPressed();
   }

   /**
    * Perform any final cleanup before an activity is destroyed.
    */
   @Override
   protected void onDestroy()
   {
      super.onDestroy();

      setImage(null, null);
   }

   /**
    * Callback for the result from requesting permissions. This method is invoked for every call on requestPermissions(android.app.Activity, String[], int).
    */
   @Override
   public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
   {
      switch(requestCode)
      {
         case REQUEST_PERM:
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
               createTest();
            }
            else
            {
               CheckBox check_save = (CheckBox) findViewById(R.id.check_save);
               check_save.setChecked(false);
               Toast.makeText(this, getString(R.string.tverific_permision_denied), Toast.LENGTH_SHORT).show();
            }
      }
   }

   /**
    * Saves a JPEG image in the SD card.
    *
    * @return the url of the file saved or null on error.
    */
   String saveJpeg()
   {
      if(jpegData == null)
         return null;

      // Request external storage permission if needed
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
      {
         if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
         {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            {
               AlertDialog.Builder builder = new AlertDialog.Builder(this);
               builder.setTitle(getString(R.string.tverific_permision_needed))
                     .setMessage(getString(R.string.tverific_permision_expl))
                     .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                     {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                           ActivityCompat.requestPermissions(TestVerificationActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERM);
                        }
                     });
               builder.create().show();
            }
            else
            {
               ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERM);
            }
            return null;
         }
      }

      // App media directory
      File mediaDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), getString(R.string.app_name));
      mediaDir.mkdirs();

      // Create a media file name
      String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
      File mediaFile = new File(mediaDir, timeStamp + ".jpg");

      // Save image
      try
      {
         FileOutputStream fos = new FileOutputStream(mediaFile);
         fos.write(jpegData);
         fos.close();
      }
      catch(FileNotFoundException e)
      {
         //Log.d(TAG, "File not found: " + e.getMessage());

         CheckBox check_save = (CheckBox) findViewById(R.id.check_save);
         check_save.setChecked(false);
         Toast.makeText(this, getString(R.string.tverific_permision_denied), Toast.LENGTH_SHORT).show();
         return null;
      }
      catch(IOException e)
      {
         //Log.d(TAG, "Error accessing file: " + e.getMessage());

         CheckBox check_save = (CheckBox) findViewById(R.id.check_save);
         check_save.setChecked(false);
         Toast.makeText(this, getString(R.string.tverific_permision_denied), Toast.LENGTH_SHORT).show();
         return null;
      }

      // Notify system gallery
      ContentValues values = new ContentValues();
      values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
      values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
      values.put(MediaStore.MediaColumns.DATA, mediaFile.getAbsolutePath());
      getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

      Toast.makeText(getApplicationContext(), getString(R.string.tverific_photo_saved), Toast.LENGTH_SHORT).show();

      return mediaFile.toString();
   }

   /**
    * Creates a test from the computed results.
    */
   void createTest()
   {
      if(!result || pigment < 0)
         return;

      String file = null;
      CheckBox check_save = (CheckBox) findViewById(R.id.check_save);
      if(jpegData != null && check_save.isChecked())
      {
         file = saveJpeg();
         if(file == null)
            return;  // Permission error
      }

      Intent intent = new Intent(this, TestEditionActivity.class);
      intent.putExtra(TestEditionActivity.PIGMENTATION, pigment);
      intent.putExtra(TestEditionActivity.FILE, file);
      if(low_res)
         intent.putExtra(TestEditionActivity.NOTE, getString(R.string.cam_low_res_note));
      startActivityForResult(intent, REQUEST_TESTEDIT);
   }

   /**
    * Called when an activity you launched exits, giving you the requestCode you started it with,
    * the resultCode it returned, and any additional data from it.
    * You will receive this call immediately before onResume() when your activity is re-starting.
    */
   @Override
   public void onActivityResult(int requestCode, int resultCode, Intent intent)
   {
      super.onActivityResult(requestCode, resultCode, intent);

      if(requestCode == REQUEST_TESTEDIT)
      {
         if(resultCode == RESULT_OK)
         {
            TaskStackBuilder.create(this)
                  .addParentStack(MyCyclesActivity.class)
                  .addNextIntent(new Intent(this, MyCyclesActivity.class))
                  .startActivities();
         }
         else
         {
            finish();
         }
      }
   }


   /**
    * Shows the user a success message.
    */
   boolean ok(String msg)
   {
      msgOk.setVisibility(View.VISIBLE);
      msgOk.setText(msg);

      return true;
   }

   /**
    * Shows the user an error message.
    */
   boolean error(String msg, String error)
   {
      //Log.w(TAG, msg);
      msgLayout.setVisibility(View.VISIBLE);
      msgError.setText(msg);
      //msgError.setText(error);

      errorCode = error;

      return false;
   }

   /**
    * Finds a clean area in the base.
    *
    * @return base area or null on error.
    */
   IntensityArea findBase()
   {
      IntensityArea base = new IntensityArea(bitmap, placeholder.getStripBase(bitmap));

      int min_width = (int) (base.getWidth() * MIN_BASE_WIDTH_PER);
      if(min_width < MIN_BASE_WIDTH)
         min_width = MIN_BASE_WIDTH;
      int min_height = (int) (base.getHeight() * MIN_BASE_HEIGHT_PER);
      if(min_height < MIN_BASE_HEIGHT)
         min_height = MIN_BASE_HEIGHT;

      // Start with a vertical auto crop:
      // Get the intensity in the center
      int x_center = base.getWidth() / 2;
      int y_center = base.getHeight() / 2;

      float center = 0f;
      int count = 0;
      for(int x = x_center - MAX_BASE_CENTER; x <= x_center + MAX_BASE_CENTER; x++)
      {
         for(int y = y_center - MAX_BASE_CENTER; y <= y_center + MAX_BASE_CENTER; y++)
         {
            center += base.get(x, y);
            count++;
         }
      }
      center /= count;

      // Search the margins
      int top, bottom;
      float intens;

      for(top = y_center - MAX_BASE_CENTER; top > 0; top--)
      {
         intens = 0f;
         count = 0;
         for(int x = x_center - MAX_BASE_CENTER; x <= x_center + MAX_BASE_CENTER; x++)
         {
            intens += base.get(x, top);
            count++;
         }
         intens /= count;

         if(Math.abs(intens - center) > MAX_BASE_DELTA)
            break;
      }
      top++;

      for(bottom = y_center - MAX_BASE_CENTER; bottom < base.getHeight(); bottom++)
      {
         intens = 0f;
         count = 0;
         for(int x = x_center - MAX_BASE_CENTER; x <= x_center + MAX_BASE_CENTER; x++)
         {
            intens += base.get(x, bottom);
            count++;
         }
         intens /= count;

         if(Math.abs(intens - center) > MAX_BASE_DELTA)
            break;
      }
      bottom = base.getHeight() - bottom + 1;

      // Apply margins
      if(!base.margin(0, top, 0, bottom) || base.getHeight() < min_height)
      {
         //placeholder.setDebugLine(base.getRect(), bitmap);
         error(getString(R.string.tverific_error_position_light), "base_margin_1");
         //error("Invalid base color (delta: " + base.getDelta() + ", area: " + base.getArea() + ").");
         return null;
      }

      // Refine removing point-specific noise
      while(base.getDelta() > MAX_BASE_DELTA)
      {
         // Get the farthest point
         Point point;
         float max = Math.abs(base.getAverage() - base.getMaximumIntensity());
         float min = Math.abs(base.getAverage() - base.getMinimumIntensity());
         if(max > min)
         {
            point = base.getMaximum();
         }
         else
         {
            point = base.getMinimum();
         }

         // Get the distance to the borders
         Rect distance = new Rect(point.x + STEP_BASE_MARGIN, point.y + STEP_BASE_MARGIN,
               base.getWidth() - point.x + STEP_BASE_MARGIN, base.getHeight() - point.y + STEP_BASE_MARGIN);

         // Make the smallest area part of the margin (priority for the sides)
         Rect margin = new Rect();
         if(distance.top < distance.bottom)
         {
            if(distance.left < distance.right)
            {
               if(distance.top <= distance.left)
               {
                  margin.top = distance.top;
               }
               else
               {
                  margin.left = distance.left;
               }
            }
            else
            {
               if(distance.top <= distance.right)
               {
                  margin.top = distance.top;
               }
               else
               {
                  margin.right = distance.right;
               }
            }
         }
         else
         {
            if(distance.left < distance.right)
            {
               if(distance.bottom <= distance.left)
               {
                  margin.bottom = distance.bottom;
               }
               else
               {
                  margin.left = distance.left;
               }
            }
            else
            {
               if(distance.bottom <= distance.right)
               {
                  margin.bottom = distance.bottom;
               }
               else
               {
                  margin.right = distance.right;
               }
            }
         }

         // Apply margin and retry
         if(!base.margin(margin) || base.getWidth() < min_width || base.getHeight() < min_height)
         {
            //placeholder.setDebugLine(base.getRect(), bitmap);
            //error("Invalid base color (delta: " + base.getDelta() + ")");
            error(getString(R.string.tverific_error_position_light), "base_margin_2");
            return null;
         }
      }

      return base;
   }

   /**
    * Finds a clean area in the control.
    *
    * @return control area or null on error.
    */
   IntensityArea findControl(float base_intens, float min_contrast)
   {
      IntensityArea control = new IntensityArea(bitmap, placeholder.getControlLine(bitmap));

      int min_width = (int) (control.getWidth() * MIN_CONTROL_WIDTH_PER);
      if(min_width < MIN_CONTROL_WIDTH)
         min_width = MIN_CONTROL_WIDTH;
      int min_height = (int) (control.getHeight() * MIN_CONTROL_HEIGHT_PER);
      if(min_height < MIN_CONTROL_HEIGHT)
         min_height = MIN_CONTROL_HEIGHT;

      if(control.getWidth() < 2 + 2 * MAX_CONTROL_CENTER + 1 || control.getHeight() < 2 + 2 * MAX_CONTROL_CENTER + 1)
      {
         // No room to auto crop.
         return control;
      }

      // Get the intensity in the center
      int x_center = control.getWidth() / 2;
      int y_center = control.getHeight() / 2;

      //float center = control.get(x_center, y_center);
      float center = 0f;
      int count = 0;
      for(int x = x_center - MAX_CONTROL_CENTER; x <= x_center + MAX_CONTROL_CENTER; x++)
      {
         for(int y = y_center - MAX_CONTROL_CENTER; y <= y_center + MAX_CONTROL_CENTER; y++)
         {
            center += control.get(x, y);
            count++;
         }
      }
      center /= count;

      if(center < base_intens + min_contrast)
      {
         /*
         Rect box = control.getRect();
         box.right = box.left + x_center + MAX_CONTROL_CENTER;
         box.left += x_center - MAX_CONTROL_CENTER;
         box.bottom = box.top + y_center + MAX_CONTROL_CENTER;
         box.top += y_center - MAX_CONTROL_CENTER;
         placeholder.setDebugLine(box, bitmap);
         */
         //error("Not enough contrast in control center (delta: " + (center - base_intens) + ").");
         error(getString(R.string.tverific_error_position_light), "control_contrast_1");
         return null;
      }

      // Search the margins
      int left, top, right, bottom;
      float intens;
      for(left = x_center - MAX_CONTROL_CENTER; left > 0; left--)
      {
         //intens = control.get(left, y_center);
         intens = 0f;
         count = 0;
         for(int y = y_center - MAX_CONTROL_CENTER; y <= y_center + MAX_CONTROL_CENTER; y++)
         {
            intens += control.get(left, y);
            count++;
         }
         intens /= count;

         if(Math.abs(intens - center) > MAX_CONTROL_DELTA)
            break;
      }
      left++;

      for(top = y_center - MAX_CONTROL_CENTER; top > 0; top--)
      {
         //intens = control.get(x_center, top);
         intens = 0f;
         count = 0;
         for(int x = x_center - MAX_CONTROL_CENTER; x <= x_center + MAX_CONTROL_CENTER; x++)
         {
            intens += control.get(x, top);
            count++;
         }
         intens /= count;

         if(Math.abs(intens - center) > MAX_CONTROL_DELTA)
            break;
      }
      top++;

      for(right = x_center - MAX_CONTROL_CENTER; right < control.getWidth(); right++)
      {
         //intens = control.get(right, y_center);
         intens = 0f;
         count = 0;
         for(int y = y_center - MAX_CONTROL_CENTER; y <= y_center + MAX_CONTROL_CENTER; y++)
         {
            intens += control.get(right, y);
            count++;
         }
         intens /= count;

         if(Math.abs(intens - center) > MAX_CONTROL_DELTA)
            break;
      }
      right = control.getWidth() - right + 1;

      for(bottom = y_center - MAX_CONTROL_CENTER; bottom < control.getHeight(); bottom++)
      {
         //intens = control.get(x_center, bottom);
         intens = 0f;
         count = 0;
         for(int x = x_center - MAX_CONTROL_CENTER; x <= x_center + MAX_CONTROL_CENTER; x++)
         {
            intens += control.get(x, bottom);
            count++;
         }
         intens /= count;

         if(Math.abs(intens - center) > MAX_CONTROL_DELTA)
            break;
      }
      bottom = control.getHeight() - bottom + 1;

      // Apply margins
      if(!control.margin(left, top, right, bottom) || control.getWidth() < min_width || control.getHeight() < min_height)
      {
         //placeholder.setDebugControlLine(control.getRect(), bitmap);
         //error("Control line not found (delta: " + control.getDelta() + ", area: " + control.getArea() + ").");
         error(getString(R.string.tverific_error_control_not_found), "control_margin");
         return null;
      }
/*
      // Refine removing point-specific noise
      while(control.getDelta() > MAX_CONTROL_TOTAL_DELTA)
      {
         // Get the farthest point
         Point point;
         float max = Math.abs(control.getAverage() - control.getMaximumIntensity());
         float min = Math.abs(control.getAverage() - control.getMinimumIntensity());
         if(max > min)
         {
            point = control.getMaximum();
         }
         else
         {
            point = control.getMinimum();
         }

         // Get the distance to the borders
         Rect distance = new Rect(point.x + STEP_BASE_MARGIN, point.y + STEP_BASE_MARGIN,
               control.getWidth() - point.x + STEP_BASE_MARGIN, control.getHeight() - point.y + STEP_BASE_MARGIN);

         // Make the smallest area part of the margin (priority for the sides)
         Rect margin = new Rect();
         if(distance.top < distance.bottom)
         {
            if(distance.left < distance.right)
            {
               if(distance.top <= distance.left)
               {
                  margin.top = distance.top;
               }
               else
               {
                  margin.left = distance.left;
               }
            }
            else
            {
               if(distance.top <= distance.right)
               {
                  margin.top = distance.top;
               }
               else
               {
                  margin.right = distance.right;
               }
            }
         }
         else
         {
            if(distance.left < distance.right)
            {
               if(distance.bottom <= distance.left)
               {
                  margin.bottom = distance.bottom;
               }
               else
               {
                  margin.left = distance.left;
               }
            }
            else
            {
               if(distance.bottom <= distance.right)
               {
                  margin.bottom = distance.bottom;
               }
               else
               {
                  margin.right = distance.right;
               }
            }
         }

         Log.w(TAG, "Control iteration: margin = " + margin);

         // Apply margin and retry
         if(!control.margin(margin) || control.getWidth() < min_width || control.getHeight() < min_height)
         {
            placeholder.setDebugLine(control.getRect());
            error("Invalid control color (delta: " + control.getDelta() + ")");
            return null;
         }
      }
*/
      if(control.getAverage() < base_intens + min_contrast)
      {
         //placeholder.setDebugControlLine(control.getRect(), bitmap);
         //error("Not enough contrast in control line (delta: " + (control.getAverage() - base_intens) + ").");
         error(getString(R.string.tverific_error_control_contrast), "control_contrast_2");
         return null;
      }

      return control;
   }

   /**
    * Finds a the area of the line.
    *
    * @return line area or null if not found.
    */
   IntensityArea findLine(IntensityArea control)
   {
      /*
      // Remove control line top and bottom margins
      Rect line_frame = new Rect(placeholder.getTestLine());
      line_frame.top = control.getRect().top;
      line_frame.bottom = control.getRect().bottom;
      IntensityArea line = new IntensityArea(bitmap, line_frame);
      */

      // Start with a double vertical auto crop (remove strip margins):
      IntensityArea line = new IntensityArea(bitmap, placeholder.getTestLine(bitmap));

      int min_height = (int) (line.getHeight() * MIN_LINE_HEIGHT_PER);
      if(min_height < MIN_LINE_HEIGHT)
         min_height = MIN_LINE_HEIGHT;

      // Get the intensity in the center
      int x1_center = (int) (line.getWidth() * LINE_CROP_LATERAL_PER);
      if(x1_center < MAX_LINE_CENTER)
         x1_center = MAX_LINE_CENTER;
      int x2_center = line.getWidth() - x1_center;
      int y_center = line.getHeight() / 2;

      int count = 0;
      float center1 = 0f;
      for(int x = x1_center - MAX_LINE_CENTER; x <= x1_center + MAX_LINE_CENTER; x++)
      {
         for(int y = y_center - MAX_LINE_CENTER; y <= y_center + MAX_LINE_CENTER; y++)
         {
            center1 += line.get(x, y);
            count++;
         }
      }
      center1 /= count;

      float center2 = 0f;
      count = 0;
      for(int x = x2_center - MAX_LINE_CENTER; x <= x2_center + MAX_LINE_CENTER; x++)
      {
         for(int y = y_center - MAX_LINE_CENTER; y <= y_center + MAX_LINE_CENTER; y++)
         {
            center2 += line.get(x, y);
            count++;
         }
      }
      center2 /= count;

      // Search the margins
      int top, bottom;
      float intens;

      for(top = y_center - MAX_LINE_CENTER; top > 0; top--)
      {
         intens = 0f;
         count = 0;
         for(int x = x1_center - MAX_LINE_CENTER; x <= x1_center + MAX_LINE_CENTER; x++)
         {
            intens += line.get(x, top);
            count++;
         }
         intens /= count;

         if(Math.abs(intens - center1) > MAX_LINE_DELTA)
            break;

         intens = 0f;
         count = 0;
         for(int x = x2_center - MAX_LINE_CENTER; x <= x2_center + MAX_LINE_CENTER; x++)
         {
            intens += line.get(x, top);
            count++;
         }
         intens /= count;

         if(Math.abs(intens - center2) > MAX_LINE_DELTA)
            break;
      }
      top++;

      for(bottom = y_center - MAX_LINE_CENTER; bottom < line.getHeight(); bottom++)
      {
         intens = 0f;
         count = 0;
         for(int x = x1_center - MAX_LINE_CENTER; x <= x1_center + MAX_LINE_CENTER; x++)
         {
            intens += line.get(x, bottom);
            count++;
         }
         intens /= count;

         if(Math.abs(intens - center1) > MAX_LINE_DELTA)
            break;

         intens = 0f;
         count = 0;
         for(int x = x2_center - MAX_LINE_CENTER; x <= x2_center + MAX_LINE_CENTER; x++)
         {
            intens += line.get(x, bottom);
            count++;
         }
         intens /= count;

         if(Math.abs(intens - center2) > MAX_LINE_DELTA)
            break;
      }
      bottom = line.getHeight() - bottom + 1;

      // Apply margins
      if(!line.margin(0, top, 0, bottom) || line.getHeight() < min_height)
      {
         return null;
      }

      // Get the column with the same width than the control line, which maximizes the intensity
      int col_width = control.getRect().width();
      int width = line.getWidth();
      int height = line.getHeight();
      float[] intensity = line.getIntensityBuffer();

      if(col_width >= width)
         return line;

      // Calculate the average intensity of each X line and find the maximum point
      float[] x_avg = new float[width];
      for(int x = 0; x < width; x++)
      {
         x_avg[x] = 0;
         for(int y = 0; y < height; y++)
         {
            x_avg[x] += intensity[y * width + x];
         }
         x_avg[x] /= height;
      }

      // Find the optimal position
      int best_x = 0;
      float best_intens = -1;
      for(int x = 0; x < width - col_width; x++)
      {
         intens = 0f;
         for(int i = 0; i < col_width; i++)
         {
            intens += x_avg[x + i];
         }
         // intens /= col_width;   // Skipped for performance

         if(intens > best_intens)
         {
            best_x = x;
            best_intens = intens;
         }
      }

      // Peak-like shape check
      int max_index = -1;
      float max_value = -1;
      for(int x = best_x; x < best_x + col_width; x++)
      {
         if(x_avg[x] > max_value)
         {
            max_index = x;
            max_value = x_avg[x];
         }
      }

      int min_index = max_index - col_width / 2;
      if(min_index < 0)
         return null;
      float min_value = x_avg[min_index];
      if(min_value > max_value - MIN_LINE_DELTA)
         return null;

      min_index = max_index + col_width / 2;
      if(min_index > width - 1)
         return null;
      min_value = x_avg[min_index];
      if(min_value > max_value - MIN_LINE_DELTA)
         return null;

      // Apply the new margin
      line.margin(best_x, 0, width - best_x - col_width, 0);

      return line;
   }

   /**
    * Processes the bitmap and displays the warnings.
    *
    * @return true on success
    */
   boolean process()
   {
      pigment = -1;

      // Get base area and intensity
      IntensityArea base = findBase();
      if(base == null)
         return false;

      //Log.w(TAG, "bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight());
      //Log.w(TAG, "control line: " + placeholder.getControlLine(bitmap));

      // Get control line area and intensity
      IntensityArea control = findControl(base.getAverage(), MIN_CONTROL_CONTRAST);
      if(control == null)
         return false;

      // Warn if poor handle contrast
      IntensityArea handle = new IntensityArea(bitmap, placeholder.getStripHandle(bitmap));
      if(handle.getAverage() < base.getAverage() + MIN_HANDLE_CONTRAST)
      {
         // Just a warning, so the process continues.
         //error("Not enough contrast in handle (delta: " + (handle.getAverage() - base.getAverage()) + ").");
         error(getString(R.string.tverific_warn_handle_contrast), "handle_contrast");
      }

      // Find test line area and intensity
      IntensityArea line = findLine(control);

      // Calculate pigmentation
      if(line == null)
      {
         pigment = 0;
         //error("Line not found.");
         error(getString(R.string.tverific_error_line_not_found), "line_not_found");
      }
      else
      {
         pigment = (int) ((line.getAverage() - base.getAverage()) * 100 / (control.getAverage() - base.getAverage()));
         if(pigment < 0)
            pigment = 0;
      }

      // Display debug information
      //placeholder.setDebugBase(base.getRect(), bitmap);
      placeholder.setDebugControlLine(control.getRect(), bitmap);
      if(line != null)
         placeholder.setDebugLine(line.getRect(), bitmap);

      return ok(getString(R.string.tverific_pigment) + " " + pigment + "%");
   }

   /**
    * Re-tries the pigment calculation in low resolution mode.
    *
    * @return true on success
    */
   boolean processLow()
   {
      error(getString(R.string.cam_low_res_title), "low_res");

      pigment = -1;

      // Get base area and intensity
      IntensityArea base = findBase();
      if(base == null)
         return error(getString(R.string.tverific_fatal), "fatal_" + errorCode);

      //Log.w(TAG, "bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight());
      //Log.w(TAG, "control line: " + placeholder.getControlLine(bitmap));

      // Get control line area and intensity
      IntensityArea control = findControl(base.getAverage(), MIN_CONTROL_CONTRAST_PREVIEW);
      if(control == null)
         return error(getString(R.string.tverific_fatal), "fatal_" + errorCode);

      // Find test line area and intensity
      IntensityArea line = findLine(control);

      // Calculate pigmentation
      if(line == null)
      {
         pigment = 0;
         //error("Line not found.");
         error(getString(R.string.tverific_error_line_not_found), "line_not_found");
      }
      else
      {
         pigment = (int) ((line.getAverage() - base.getAverage()) * 100 / (control.getAverage() - base.getAverage()));
         if(pigment < 0)
            pigment = 0;
      }

      // Display debug information
      //placeholder.setDebugBase(base.getRect(), bitmap);
      placeholder.setDebugControlLine(control.getRect(), bitmap);
      if(line != null)
         placeholder.setDebugLine(line.getRect(), bitmap);

      return ok(getString(R.string.tverific_pigment) + " " + pigment + "%");
   }
}
