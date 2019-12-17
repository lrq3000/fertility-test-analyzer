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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

/**
 * Strip test photo capture activity.
 * <p>
 * Lets the user take a picture of a strip test and extracts the pigmentation level from it.
 */
public class NewTestActivity extends AppCompatActivity
{
   static final String TAG = "NewTestActivity";

   static final int REQUEST_IMAGE = 1;
   static final int REQUEST_PERM = 2;

   static final String STATE_TORCH = "state_torch";
   static final String STATE_ZOOM = "state_zoom";


   /**
    * Camera control.
    */
   CameraView camera;

   /**
    * Strip placeholder control.
    */
   StripPlaceholderView placeholder;

   /**
    * Manual focus operation indicator.
    */
   ManualFocusView focusIndicator;

   /**
    * Instance state or null if none.
    */
   protected Bundle state;


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
      state = savedInstanceState;
      setContentView(R.layout.activity_new_test);

      camera = (CameraView) findViewById(R.id.camera);

      focusIndicator = (ManualFocusView) findViewById(R.id.focus_area);

      View messageLayout = findViewById(R.id.message_layout);
      messageLayout.setVisibility(View.INVISIBLE);

      CheckBox check = (CheckBox) findViewById(R.id.check_torch);
      check.setVisibility(View.INVISIBLE);
      check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
         @Override
         public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
         {
            if(isChecked)
               buttonView.setText(getString(R.string.cam_torch_on));
            else
               buttonView.setText(getString(R.string.cam_torch_off));
            camera.setTorch(isChecked);
         }
      });

      placeholder = (StripPlaceholderView) findViewById(R.id.placeholder);

      SharedPreferences prefs = Config.getPrefs(this);
      int ph_type = prefs.getInt(Config.PREF_PLACEHOLDER_TYPE, Config.PREF_PLACEHOLDER_TYPE_DEFAULT);
      RadioButton rb;
      switch(ph_type)
      {
         case 1:
            rb = (RadioButton) findViewById(R.id.mask_small);
            rb.setChecked(true);
            break;
         case 2:
            rb = (RadioButton) findViewById(R.id.mask_medium);
            rb.setChecked(true);
            break;
         case 3:
         default:
            rb = (RadioButton) findViewById(R.id.mask_large);
            rb.setChecked(true);
            break;
      }
      setPlaceholder(ph_type);

      RadioGroup rg = (RadioGroup) findViewById(R.id.mask_group);
      rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
      {
         @Override
         public void onCheckedChanged(RadioGroup radioGroup, int checkedId)
         {
            int type;
            switch(checkedId)
            {
               case R.id.mask_small:
                  type = 1;
                  break;
               case R.id.mask_medium:
                  type = 2;
                  break;
               case R.id.mask_large:
               default:
                  type = 3;
                  break;
            }
            setPlaceholder(type);
         }
      });

      final SeekBar zoomBar = (SeekBar) findViewById(R.id.zoom_bar);
      zoomBar.setVisibility(View.INVISIBLE);
      zoomBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
      {
         @Override
         public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
         {
            if(!fromUser)
               return;
            camera.setZoom(progress);
         }

         @Override
         public void onStartTrackingTouch(SeekBar seekBar)
         {

         }

         @Override
         public void onStopTrackingTouch(SeekBar seekBar)
         {

         }
      });

      View view = findViewById(R.id.zoom_in);
      view.setVisibility(View.INVISIBLE);
      view.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View view)
         {
            int zoom = zoomBar.getProgress() + 1;
            camera.setZoom(zoom);
            zoomBar.setProgress(camera.getZoom());
         }
      });

      view = findViewById(R.id.zoom_out);
      view.setVisibility(View.INVISIBLE);
      view.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View view)
         {
            int zoom = zoomBar.getProgress() - 1;
            camera.setZoom(zoom);
            zoomBar.setProgress(camera.getZoom());
         }
      });

      view = findViewById(R.id.zoom_label);
      view.setVisibility(View.INVISIBLE);

      ImageButton ibtn = (ImageButton) findViewById(R.id.btn_capture);
      ibtn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            camera.takePicture();
         }
      });

      Button btn = (Button) findViewById(R.id.btn_load);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, getString(R.string.ntest_select_image)), REQUEST_IMAGE);
         }
      });
      if(Config.isDevMode(this))
         btn.setVisibility(View.VISIBLE);
      else
         btn.setVisibility(View.GONE);

      btn = (Button) findViewById(R.id.window_tip_ok);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            View view = findViewById(R.id.window_tip);
            view.setVisibility(View.GONE);

            SharedPreferences prefs = Config.getPrefs(NewTestActivity.this);
            if(prefs.getBoolean(Config.PREF_ZOOM_AVAILABLE, true))
               view = findViewById(R.id.zoom_tip);
            else
               view = findViewById(R.id.mask_tip);
            view.setVisibility(View.VISIBLE);
         }
      });

      btn = (Button) findViewById(R.id.zoom_tip_ok);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            View view = findViewById(R.id.zoom_tip);
            view.setVisibility(View.GONE);

            view = findViewById(R.id.mask_tip);
            view.setVisibility(View.VISIBLE);
         }
      });

      btn = (Button) findViewById(R.id.mask_tip_ok);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            View view = findViewById(R.id.mask_tip);
            view.setVisibility(View.GONE);
         }
      });

      if(savedInstanceState == null)
      {
         // Reset attempts counter.
         prefs.edit().putInt(Config.PREF_TEST_ATTEMPTS, 0).apply();
      }

      // Android Marshmallow and newer require explicit permission request
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
      {
         if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
         {
            camera.setVisibility(View.INVISIBLE);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERM);
            return;
         }
      }

      if(prefs.getBoolean(Config.PREF_SHOW_CAMERA_PREVIEW, true))
      {
         Intent intent = new Intent(this, CameraPreviewActivity.class);
         startActivity(intent);
      }
   }

   /**
    * Sets the placeholder type preview.
    *
    * @param type Type number 1
    */
   void setPlaceholder(int type)
   {
      SharedPreferences prefs = Config.getPrefs(this);
      prefs.edit().putInt(Config.PREF_PLACEHOLDER_TYPE, type).apply();

      placeholder.setType(type);
   }

   /**
    * Called just before the activity starts interacting with the user. At this point the activity
    * is at the top of the activity stack, with user input going to it.
    */
   @Override
   protected void onResume()
   {
      focusIndicator.setFocusArea(null);
      camera.setActivity(this, focusIndicator);
      if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
      {
         camera.setVisibility(View.VISIBLE);
      }

      SharedPreferences prefs = Config.getPrefs(this);
      View view;
      if(prefs.getBoolean(Config.PREF_SHOW_CAMERA_WIZARD, false))
      {
         prefs.edit().putBoolean(Config.PREF_SHOW_CAMERA_WIZARD, false).apply();

         view = findViewById(R.id.window_tip);
         view.setVisibility(View.VISIBLE);
      }

      super.onResume();
   }

   /**
    * Called when the system is about to start resuming another activity. This method is typically
    * used to commit unsaved changes to persistent data, stop animations and other things that may
    * be consuming CPU, and so on. It should do whatever it does very quickly, because the next
    * activity will not be resumed until it returns.
    */
   @Override
   protected void onPause()
   {
      super.onPause();

      camera.setActivity(null, null);
      camera.setVisibility(View.INVISIBLE);
   }

   /**
    * Called to retrieve per-instance state from an activity before being killed so that the state
    * can be restored in onCreate(Bundle) or onRestoreInstanceState(Bundle) (the Bundle populated
    * by this method will be passed to both).
    */
   @Override
   protected void onSaveInstanceState(Bundle outState)
   {
      super.onSaveInstanceState(outState);

      CheckBox check = (CheckBox) findViewById(R.id.check_torch);
      outState.putBoolean(STATE_TORCH, check.isChecked());

      SeekBar zoomBar = (SeekBar) findViewById(R.id.zoom_bar);
      outState.putInt(STATE_ZOOM, zoomBar.getProgress());

      state = outState;
   }

   /**
    * Callback for the result from requesting permissions. This method is invoked for every call on requestPermissions.
    */
   @Override
   public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
   {
      switch(requestCode)
      {
         case REQUEST_PERM:
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
               // The activity need to be set here because this event comes before the onResume one.
               camera.setActivity(this, focusIndicator);
               camera.setVisibility(View.VISIBLE);

               SharedPreferences prefs = Config.getPrefs(this);
               if(prefs.getBoolean(Config.PREF_SHOW_CAMERA_PREVIEW, true))
               {
                  Intent intent = new Intent(this, CameraPreviewActivity.class);
                  startActivity(intent);
               }
            }
            else
            {
               Toast.makeText(this, getString(R.string.ntest_camera_perm), Toast.LENGTH_SHORT).show();
               finish();
            }
      }
   }

   /**
    * Called when an activity you launched exits, giving you the requestCode you started it with,
    * the resultCode it returned, and any additional data from it.
    * You will receive this call immediately before onResume() when your activity is re-starting.
    */
   @Override
   public void onActivityResult(int requestCode, int resultCode, Intent data)
   {
      super.onActivityResult(requestCode, resultCode, data);

      if(requestCode == REQUEST_IMAGE && resultCode == RESULT_OK)
      {
         Bitmap bitmap;
         try
         {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
         }
         catch(IOException e)
         {
            //Log.w(TAG, "Image read error: " + data.getData(), e);
            return;
         }

         onPictureTaken(bitmap, null);   // Loaded image should never be saved
      }
   }

   /**
    * Event received when the camera is set and running.
    */
   void onCameraReady()
   {
      CheckBox check = (CheckBox) findViewById(R.id.check_torch);
      if(camera.supportsTorch())
      {
         check.setVisibility(View.VISIBLE);
         if(state != null)
         {
            check.setChecked(state.getBoolean(STATE_TORCH, false));
         }
      }
      else
      {
         check.setVisibility(View.INVISIBLE);
      }

      SeekBar zoom_bar = (SeekBar) findViewById(R.id.zoom_bar);
      View zoom_in = findViewById(R.id.zoom_in);
      View zoom_out = findViewById(R.id.zoom_out);
      View zoom_label = findViewById(R.id.zoom_label);
      int max_zoom = camera.getMaxZoom();
      if(max_zoom > 0)
      {
         zoom_bar.setVisibility(View.VISIBLE);
         zoom_bar.setMax(max_zoom);
         if(state != null)
         {
            int zoom = state.getInt(STATE_ZOOM);
            camera.setZoom(zoom);
            zoom_bar.setProgress(zoom);
         }
         else
         {
            zoom_bar.setProgress(camera.getZoom());
         }
         zoom_in.setVisibility(View.VISIBLE);
         zoom_out.setVisibility(View.VISIBLE);
         zoom_label.setVisibility(View.VISIBLE);
      }
      else
      {
         zoom_bar.setVisibility(View.INVISIBLE);
         zoom_in.setVisibility(View.INVISIBLE);
         zoom_out.setVisibility(View.INVISIBLE);
         zoom_label.setVisibility(View.INVISIBLE);
      }

      SharedPreferences prefs = Config.getPrefs(this);
      prefs.edit().putBoolean(Config.PREF_ZOOM_AVAILABLE, max_zoom > 0).apply();
   }

   /**
    * Picture taken event. Sends the image to the verification activity.
    *
    * @param jpeg_data JPEG encoded image.
    * @param bitmap    raw pixels bitmap.
    * @return true to continue the preview or false to stop it.
    */
   boolean onPictureTaken(Bitmap bitmap, byte[] jpeg_data)
   {
      /* The bitmap information is too big to go in the intent, it makes the binder to fail,
       so the only way I found to pass this information is using a static method.
       Not ideal but works...
       */
      TestVerificationActivity.setImage(bitmap, jpeg_data);
      startActivity(new Intent(this, TestVerificationActivity.class));

      return false;
   }

   /**
    * Processes a preview frame and warn the user of possible defects.
    *
    * @param bitmap
    * @param jpeg_data
    */
   void onPreviewFrame(Bitmap bitmap, byte[] jpeg_data)
   {
      //Log.i(TAG, "onPreviewFrame " + bitmap.getWidth() + "x" + bitmap.getHeight());

      int warn = TestVerificationActivity.checkPreview(bitmap, placeholder);
      if(warn != -1)
      {
         View messageLayout = findViewById(R.id.message_layout);
         messageLayout.setVisibility(View.VISIBLE);
         TextView message = (TextView) findViewById(R.id.message);
         message.setText(warn);
      }
      else
      {
         View messageLayout = findViewById(R.id.message_layout);
         messageLayout.setVisibility(View.INVISIBLE);
      }
   }
}
