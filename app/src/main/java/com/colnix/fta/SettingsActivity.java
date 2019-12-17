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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Activity for the user to change their preferences and settings.
 */
public class SettingsActivity extends NavigationActivity
{
   static final int REQUEST_PERM = 1;
   static final int REQUEST_CSV = 2;

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

      Button btn = (Button) findViewById(R.id.btn_export);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View view)
         {
            exportData();
         }
      });

      btn = (Button) findViewById(R.id.btn_import);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View view)
         {
            importData(false);
         }
      });

      TextView text = (TextView) findViewById(R.id.text_photo_dir);
      text.setText(Config.getMediaDisplayDir(this));
      text.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View view)
         {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = FileProvider.getUriForFile(SettingsActivity.this, getApplicationContext().getPackageName() + ".provider", Config.getMediaDir(SettingsActivity.this));
            intent.setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getString(R.string.pref_open_dir)));
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

      Spinner spinner = (Spinner) findViewById(R.id.spinner_date_format);
      String[] date_formats = new String[]{getString(R.string.pref_date_format_dd_mm_yyyy), getString(R.string.pref_date_format_mm_dd_yyyy)};
      spinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, date_formats));
      spinner.setSelection(prefs.getString(Config.PREF_DATE_FORMAT, "dd/MM/yyyy").equals("dd/MM/yyyy") ? 0 : 1);
      spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
      {
         @Override
         public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
         {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(Config.PREF_DATE_FORMAT, i == 0 ? "dd/MM/yyyy" : "MM/dd/yyyy");
            edit.apply();
         }

         @Override
         public void onNothingSelected(AdapterView<?> adapterView)
         {

         }
      });

      EditText edit = (EditText) findViewById(R.id.edit_ovulation_brand);
      edit.setText(prefs.getString(Config.PREF_DEFAULT_OVULATION_BRAND, ""));
      edit.setOnEditorActionListener(new TextView.OnEditorActionListener()
      {
         @Override
         public boolean onEditorAction(TextView textView, int action, KeyEvent keyEvent)
         {
            if(action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_DONE || (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER))
            {
               prefs.edit().putString(Config.PREF_DEFAULT_OVULATION_BRAND, textView.getText().toString()).apply();
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
            if(action == EditorInfo.IME_ACTION_SEARCH || action == EditorInfo.IME_ACTION_DONE || (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER))
            {
               prefs.edit().putString(Config.PREF_DEFAULT_PREGNANCY_BRAND, textView.getText().toString()).apply();
            }
            return false;
         }
      });

      text = (TextView) findViewById(R.id.text_version);
      text.setText(BuildConfig.VERSION_NAME);

      check = (CheckBox) findViewById(R.id.check_dev_mode);
      check.setChecked(prefs.getBoolean(Config.PREF_DEV_MODE, false));
      check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
         @Override
         public void onCheckedChanged(CompoundButton compoundButton, boolean checked)
         {
            prefs.edit().putBoolean(Config.PREF_DEV_MODE, checked).apply();
            finish();
         }
      });

      if(!Config.isDevMode(this))
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

   @Override
   protected void onDestroy()
   {
      super.onDestroy();
   }

   void exportData()
   {
      if(data == null)
         return;

      // Request external storage permission if needed
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
      {
         if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
         {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            {
               AlertDialog.Builder builder = new AlertDialog.Builder(this);
               builder.setTitle(getString(R.string.pref_permision_needed)).setMessage(getString(R.string.pref_permision_expl)).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
               {
                  @Override
                  public void onClick(DialogInterface dialogInterface, int i)
                  {
                     ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERM);
                  }
               });
               builder.create().show();
            }
            else
            {
               ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERM);
            }
            return;
         }
      }

      // App media directory
      // It does not look like the best place to save the data, but in practice is handy when
      // you try to move your data.
      File media_dir = Config.getMediaDir(this);

      // Create a media file name
      String time_stamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
      File csv_file = new File(media_dir, time_stamp + ".csv");

      if(!data.exportData(csv_file))
      {
         Toast.makeText(getApplicationContext(), getString(R.string.pref_export_error), Toast.LENGTH_SHORT).show();
         return;
      }

      Intent intent = new Intent(Intent.ACTION_SEND);
      Uri uri = FileProvider.getUriForFile(SettingsActivity.this, getApplicationContext().getPackageName() + ".provider", csv_file);
      intent.putExtra(Intent.EXTRA_STREAM, uri);
      intent.setType("text/csv");
      intent.setDataAndType(uri, "text/csv");
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      startActivity(Intent.createChooser(intent, getString(R.string.pref_send_file)));
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
               exportData();
            }
            else
            {
               Toast.makeText(this, getString(R.string.pref_permision_denied), Toast.LENGTH_SHORT).show();
            }
      }
   }

   void importData(boolean confirmed)
   {
      if(data == null)
         return;

      if(!confirmed && data.countTests() > 0)
      {
         // Ask for confirmation
         AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FtaAlertDialogStyle);
         builder.setTitle(getString(R.string.pref_import_confirm_title));
         builder.setIcon(R.drawable.ico_warning_grey);
         builder.setMessage(getString(R.string.pref_import_confirm_message));
         builder.setNeutralButton(getString(R.string.delete_no), null);
         builder.setPositiveButton(getString(R.string.pref_continue), new DialogInterface.OnClickListener()
         {
            public void onClick(DialogInterface dialog, int i)
            {
               importData(true);
            }
         });
         builder.create().show();
         return;
      }

      // Ask for the file
      Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
      intent.setType("text/csv");
      startActivityForResult(Intent.createChooser(intent, getString(R.string.pref_select_csv)), REQUEST_CSV);
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

      if(requestCode == REQUEST_CSV && resultCode == RESULT_OK)
      {
         File csv_file = new File(data.getData().getPath());
         if(this.data.importData(Config.getMediaDir(this), csv_file))
         {
            Toast.makeText(this, getString(R.string.pref_import_ok), Toast.LENGTH_SHORT).show();
         }
         else
         {
            Toast.makeText(this, getString(R.string.pref_import_error), Toast.LENGTH_SHORT).show();
         }
      }
   }
}
