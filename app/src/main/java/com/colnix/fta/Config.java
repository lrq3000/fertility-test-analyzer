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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import static android.content.Context.MODE_PRIVATE;

/**
 * Config helper class.
 */
public class Config
{
   /**
    * Application preferences file.
    */
   static final String CONST_PREFERENCES_FILE = "FTA";

   /**
    * Google Play application web page.
    */
   static final String CONST_GOOGLE_PLAY_WEB = "https://play.google.com/store/apps/details?id=com.colnix.fta&referrer=utm_source%3Dshare";

   /**
    * Google Play application in the market app.
    */
   static final String CONST_GOOGLE_PLAY = "market://details?id=com.colnix.fta";

   /**
    * Number of failed attempts which make the camera wizard to show.
    */
   static final int[] CONST_ATTEMPTS_CAMERA_WIZARD = new int[] { 3, 6, 9 };


   //// Preferences keys ////
   /**
    * The user has opened the application for the very first time.
    */
   static final String PREF_FIRST_OPEN = "first_open";

   /**
    * The user has used the low resolution detection mode for the very first time.
    */
   static final String PREF_FIRST_LOW_RES = "first_low_resolution";

   /**
    * The user has never used the New Test activity.
    */
   static final String PREF_FIRST_NEW_TEST = "first_new_test";

   /**
    * Default type of placeholder.
    */
   static final int PREF_PLACEHOLDER_TYPE_DEFAULT = 2;

   /**
    * Type of placeholder preferred by the user.
    */
   static final String PREF_PLACEHOLDER_TYPE = "placeholder_type";

   /**
    * Number of consecutive failed attempts taking a picture of a test.
    */
   static final String PREF_TEST_ATTEMPTS = "test_attempts";

   /**
    * Flag saying if the soft zoom is available in this device.
    */
   static final String PREF_ZOOM_AVAILABLE = "zoom_available";

   /**
    * If the camera wizard must be shown.
    */
   static final String PREF_SHOW_CAMERA_WIZARD = "show_camera_wizard";

   /**
    * Number of times that the camera wizard has been shown.
    */
   static final String PREF_CAMERA_WIZARD_COUNT = "camera_wizard_count";

   /**
    * If the photo should be saved by default.
    */
   static final String PREF_SAVE_PHOTO = "save_photo";

   /**
    * If the flash can be used during the autofocus
    */
   static final String PREF_FLASH_ON_FOCUS = "flash_on_focus";

   /**
    * If the test notes should be visible by default.
    */
   static final String PREF_SHOW_NOTES = "show_notes";

   /**
    * If the initial camera preview tip must be shown.
    */
   static final String PREF_SHOW_CAMERA_PREVIEW = "show_camera_preview";

   /**
    * Name of the ovulation brand to use by default.
    */
   static final String PREF_DEFAULT_OVULATION_BRAND = "default_ovulation_brand";

   /**
    * Name of the pregnancy brand to use by default.
    */
   static final String PREF_DEFAULT_PREGNANCY_BRAND = "default_pregnancy_brand";


   /**
    * Returns the application preferences file reader/writer.
    */
   public static SharedPreferences getPrefs(Context ctx)
   {
      return ctx.getSharedPreferences(CONST_PREFERENCES_FILE, MODE_PRIVATE);
   }

   /**
    * Redirect the user to an activity showing an URI.
    */
   public static void goToUri(Context ctx, String uri)
   {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setData(Uri.parse(uri));

      try
      {
         ctx.startActivity(intent);
      }
      catch(ActivityNotFoundException e)
      {
         Toast.makeText(ctx, ctx.getResources().getString(R.string.share_error), Toast.LENGTH_SHORT).show();
      }
   }

   /**
    * Lets the user share the app with friends.
    */
   public static void shareApp(Context ctx)
   {
      Intent sendIntent = new Intent();
      sendIntent.setAction(Intent.ACTION_SEND);
      sendIntent.putExtra(Intent.EXTRA_TEXT,
            String.format(ctx.getResources().getString(R.string.share_text), CONST_GOOGLE_PLAY_WEB));
      sendIntent.setType("text/plain");
      ctx.startActivity(Intent.createChooser(sendIntent, ctx.getResources().getText(R.string.share_title)));
   }

   /**
    * Returns true if this model's calendar implementation is buggy.
    */
   public static boolean calendarBug()
   {
      /*
      if(Build.MANUFACTURER.equals("Acer") && Build.MODEL.equals("E39"))
         return true;
      */
      if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
         return true;

      return false;
   }

   /**
    * Returns true if this device produces an inverted preview image.
    */
   public static boolean PreviewInverted()
   {
      String model = deviceModel();
      if(model.equals("BQ AQUARIS U PLUS"))
         return true;
      return false;
   }

   /**
    * Returns a cleaned up device model string.
    */
   public static String deviceModel()
   {
      String manufacturer = Build.MANUFACTURER;
      String model = Build.MODEL;
      if(model.startsWith(manufacturer))
      {
         return model.toUpperCase();
      }
      return manufacturer.toUpperCase() + " " + model.toUpperCase();
   }
}
