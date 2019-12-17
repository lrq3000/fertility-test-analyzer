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
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;

/**
 * Helper activity to show a preview of the use of the camera.
 */
public class CameraPreviewActivity extends AppCompatActivity
{
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
      setContentView(R.layout.activity_camera_preview);

      Button btn = (Button) findViewById(R.id.btn_ok);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            CheckBox cb = (CheckBox) findViewById(R.id.check_dont_repeat);
            if(cb.isChecked())
            {
               // The camera wizard will not show automatically next time
               SharedPreferences prefs = Config.getPrefs(CameraPreviewActivity.this);
               prefs.edit().putBoolean(Config.PREF_SHOW_CAMERA_PREVIEW, false).apply();
            }

            finish();
         }
      });
   }
}
