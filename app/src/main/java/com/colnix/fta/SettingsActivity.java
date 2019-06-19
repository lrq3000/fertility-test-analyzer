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
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity for the user to change their preferences and settings.
 */
public class SettingsActivity extends NavigationActivity
{
   /**
    * Called when the activity is starting. This is where most initialization should go: calling
    * setContentView(int) to inflate the activity's UI, using findViewById(int) to programmatically
    * interact with widgets in the UI
    */
   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_settings);

      init();

      final SharedPreferences prefs = Config.getPrefs(this);

      CheckBox check = (CheckBox) findViewById(R.id.check_save_pics);
      check.setChecked(prefs.getBoolean(Config.PREF_SAVE_PHOTO, true));
      check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
         @Override
         public void onCheckedChanged(CompoundButton compoundButton, boolean checked)
         {
            prefs.edit().putBoolean(Config.PREF_SAVE_PHOTO, checked).apply();
         }
      });

      check = (CheckBox) findViewById(R.id.check_flash_on_focus);
      check.setChecked(prefs.getBoolean(Config.PREF_FLASH_ON_FOCUS, true));
      check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
         @Override
         public void onCheckedChanged(CompoundButton compoundButton, boolean checked)
         {
            prefs.edit().putBoolean(Config.PREF_FLASH_ON_FOCUS, checked).apply();
         }
      });

      check = (CheckBox) findViewById(R.id.check_visible_notes);
      check.setChecked(prefs.getBoolean(Config.PREF_SHOW_NOTES, false));
      check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
         @Override
         public void onCheckedChanged(CompoundButton compoundButton, boolean checked)
         {
            prefs.edit().putBoolean(Config.PREF_SHOW_NOTES, checked).apply();
         }
      });

      check = (CheckBox) findViewById(R.id.check_camera_tip);
      check.setChecked(prefs.getBoolean(Config.PREF_SHOW_CAMERA_PREVIEW, true));
      check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
         @Override
         public void onCheckedChanged(CompoundButton compoundButton, boolean checked)
         {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean(Config.PREF_SHOW_CAMERA_PREVIEW, checked);
            if(checked)
            {
               edit.putInt(Config.PREF_CAMERA_WIZARD_COUNT, 0);
               edit.putInt(Config.PREF_TEST_ATTEMPTS, 0);
               edit.putBoolean(Config.PREF_FIRST_LOW_RES, true);
               edit.putBoolean(Config.PREF_FIRST_NEW_TEST, true);
            }
            edit.apply();
         }
      });

      EditText edit = (EditText) findViewById(R.id.edit_ovulation_brand);
      edit.setText(prefs.getString(Config.PREF_DEFAULT_OVULATION_BRAND, ""));
      edit.setOnEditorActionListener(new TextView.OnEditorActionListener()
      {
         @Override
         public boolean onEditorAction(TextView textView, int action, KeyEvent keyEvent)
         {
            if(action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_DONE ||
                  (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                        keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER))
            {
               prefs.edit().putString(Config.PREF_DEFAULT_OVULATION_BRAND,
                     textView.getText().toString()).apply();
            }
            return false;
         }
      });

      edit = (EditText) findViewById(R.id.edit_pregnancy_brand);
      edit.setText(prefs.getString(Config.PREF_DEFAULT_PREGNANCY_BRAND, ""));
      edit.setOnEditorActionListener(new TextView.OnEditorActionListener()
      {
         @Override
         public boolean onEditorAction(TextView textView, int action, KeyEvent keyEvent)
         {
            if(action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_DONE ||
                  (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                        keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER))
            {
               prefs.edit().putString(Config.PREF_DEFAULT_PREGNANCY_BRAND,
                     textView.getText().toString()).apply();
            }
            return false;
         }
      });

      TextView text = (TextView) findViewById(R.id.text_version);
      text.setText(BuildConfig.VERSION_NAME);

      if( !BuildConfig.DEBUG)
      {
         View view = findViewById(R.id.text_clear_title);
         view.setVisibility(View.GONE);
         view = findViewById(R.id.text_clear);
         view.setVisibility(View.GONE);
      }
      else
      {
         View view = findViewById(R.id.text_clear_title);
         view.setVisibility(View.VISIBLE);
         view = findViewById(R.id.text_clear);
         view.setVisibility(View.VISIBLE);
         view.setOnClickListener(new View.OnClickListener()
         {
            @Override
            public void onClick(View view)
            {
               prefs.edit().clear().apply();

               Toast.makeText(SettingsActivity.this, R.string.pref_reset, Toast.LENGTH_SHORT).show();
               finish();
            }
         });
      }
   }
}
