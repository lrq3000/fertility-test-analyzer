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

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

public class HomeActivity extends NavigationActivity
{
   /**
    * Interface to handle notification results.
    */
   public interface NotifyListener
   {
      /**
       * Executed at the end of the notification.
       * @param positive true if the user pressed the possitive button (yes, ok...)
       */
      void closed(boolean positive);
   }


   // Intent data (all optional)
   /**
    * Notification message. Required to trigger a notification.
    */
   static final String DATA_MESSAGE = "message";

   /**
    * Notification title. Default R.string.notify_title
    */
   static final String DATA_TITLE = "title";

   /**
    * Notification positive button text.
    * Default R.string.notify_ok for message notifications and R.string.notify_yes for buy and feedback.
    */
   static final String DATA_POSITIVE = "positive";

   /**
    * Notification negative button text.
    * Default no button for message notifications or R.string.notify_no for buy and feedback.
    */
   static final String DATA_NEGATIVE = "negative";

   /**
    * Notification URI to redirect the user on positive response of message notifications.
    */
   static final String DATA_URI = "uri";


   /* State parameter */
   static final String NOTIFICATION_CLOSED = "notification_closed";


   /**
    * Flag saying that the notification has been seen and closed by the user.
    */
   boolean notificationClosed;


   /**
    * Called when the activity is starting. This is where most initialization should go: calling
    * setContentView(int) to inflate the activity's UI, using findViewById(int) to programmatically
    * interact with widgets in the UI
    */
   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_home);

      Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
      toolbar.setContentInsetsAbsolute(0, 0);
      setSupportActionBar(toolbar);
      ActionBar ab = getSupportActionBar();
      if(ab != null)
      {
         ab.setDisplayShowTitleEnabled(false);
         ab.setDisplayShowHomeEnabled(false);
      }

      // Start the service for all the application life, otherwise it gets recreated on every
      // activity.
      startService(new Intent(this, TestsService.class));
      init();

      // Called when the user clicks on New Test Button
      ImageButton btn = (ImageButton) findViewById(R.id.home_btn_newTest);
      btn.setOnClickListener(new View.OnClickListener()
            {
               @Override
               public void onClick(View v)
               {
                  SharedPreferences prefs = Config.getPrefs(HomeActivity.this);
                  prefs.edit().putBoolean(Config.PREF_FIRST_NEW_TEST, false).apply();

                  Intent intent = new Intent(HomeActivity.this, NewTestActivity.class);
                  startActivity(intent);
               }
            });
      SharedPreferences prefs = Config.getPrefs(HomeActivity.this);
      if(prefs.getBoolean(Config.PREF_FIRST_NEW_TEST, true))
      {
         Animation anim = AnimationUtils.loadAnimation(this, R.anim.button_highlight);
         btn.startAnimation(anim);
      }

      // Called when the user clicks on My Tests Button
      btn = (ImageButton) findViewById(R.id.home_btn_myTests);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            Intent intent = new Intent(HomeActivity.this, MyCyclesActivity.class);
            startActivity(intent);
         }
      });

      // Called when the user clicks on My Cycles Button
      btn = (ImageButton) findViewById(R.id.home_btn_myCycles);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            Intent intent = new Intent(HomeActivity.this, ChartsActivity.class);
            startActivity(intent);
         }
      });

      // Called when the user clicks on Information Button
      btn = (ImageButton) findViewById(R.id.home_btn_information);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            Intent intent = new Intent(HomeActivity.this, InformationActivity.class);
            startActivity(intent);
         }
      });

      // Called when the user clicks on Help Button
      btn = (ImageButton) findViewById(R.id.home_btn_help);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            Intent intent = new Intent(HomeActivity.this, HelpActivity.class);
            startActivity(intent);
         }
      });

      // Called when the user clicks on Settings Button
      btn = (ImageButton) findViewById(R.id.home_btn_settings);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            startActivity(intent);
         }
      });

      btn = (ImageButton) findViewById(R.id.btn_share);
      btn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View view)
         {
            Config.shareApp(HomeActivity.this);
         }
      });

      notificationClosed = false;
      if(savedInstanceState != null)
         notificationClosed = savedInstanceState.getBoolean(NOTIFICATION_CLOSED, false);
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

      outState.putBoolean(NOTIFICATION_CLOSED, notificationClosed);
   }

   /**
    * Called just before the activity starts interacting with the user. At this point the activity
    * is at the top of the activity stack, with user input going to it.
    */
   @Override
   protected void onResume()
   {
      super.onResume();

      onServicesReady();
   }

   /**
    * Event invoked when the data is ready.
    */
   @Override
   protected void onServicesReady()
   {
      super.onServicesReady();

      ActionBar ab = getSupportActionBar();
      if(ab != null)
      {
         ab.setDisplayShowTitleEnabled(false);
         ab.setDisplayShowHomeEnabled(false);
      }

      SharedPreferences prefs = Config.getPrefs(this);
      if(prefs.getBoolean(Config.PREF_FIRST_OPEN, true))
      {
         showNotification(getText(R.string.home_msg_title), getText(R.string.home_msg_content),
               null, null, new NotifyListener()
         {
            @Override
            public void closed(boolean positive)
            {
               Config.getPrefs(HomeActivity.this)
                     .edit()
                     .putBoolean(Config.PREF_FIRST_OPEN, false)
                     .apply();
            }
         });
      }
      else if(getIntent().getStringExtra(DATA_MESSAGE) != null && !notificationClosed)
      {
         showNotificationDialog();
      }
   }

   /**
    * Shows the notification message dialog.
    */
   void showNotificationDialog()
   {
      Intent intent = getIntent();
      CharSequence msg = intent.getStringExtra(DATA_MESSAGE);
      CharSequence title = intent.getStringExtra(DATA_TITLE);
      CharSequence pos = intent.getStringExtra(DATA_POSITIVE);
      CharSequence neg = intent.getStringExtra(DATA_NEGATIVE);
      final String uri = intent.getStringExtra(DATA_URI);

      if(msg == null)
         return;

      if(title == null)
         title = getString(R.string.notify_title);

      if(pos == null)
         pos = getText(R.string.notify_ok);

      showNotification(title, msg, neg, pos, new NotifyListener()
      {
         @Override
         public void closed(boolean positive)
         {
            notificationClosed = true;
            if(!positive)
               return;

            if(uri != null)
            {
               Config.goToUri(HomeActivity.this, uri);
            }
         }
      });
   }

   /**
    * Shows a generic notification to the user.
    */
   void showNotification(CharSequence title, CharSequence msg, CharSequence neg, CharSequence pos, final NotifyListener listener)
   {
      AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FtaAlertDialogStyle);
      builder.setTitle(title);
      builder.setMessage(msg);
      builder.setIcon(R.mipmap.logo);
      if(neg != null)
      {
         builder.setNegativeButton(neg, new DialogInterface.OnClickListener()
         {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
               if(listener != null)
                  listener.closed(false);
            }
         });
      }
      builder.setPositiveButton(pos, new DialogInterface.OnClickListener()
      {
         @Override
         public void onClick(DialogInterface dialogInterface, int i)
         {
            if(listener != null)
               listener.closed(true);
         }
      });
      AlertDialog dlg = builder.create();
      dlg.show();

      TextView text = (TextView)dlg.findViewById(android.R.id.message);
      if(text != null)
         text.setMovementMethod(LinkMovementMethod.getInstance());
   }
}
