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

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;

/**
 * Class base for activities which require data and remote services access.
 */
public class ServicesActivity extends AppCompatActivity
{
   /**
    * Tests data access.
    */
   protected TestsData data;

   /**
    * Data service connection handler.
    */
   ServiceConnection dataConnection = new ServiceConnection()
   {
      @Override
      public void onServiceConnected(ComponentName componentName, IBinder service)
      {
         TestsService.ServiceBinder binder = (TestsService.ServiceBinder) service;
         data = binder.getService();

         onServicesReady();
      }

      @Override
      public void onServiceDisconnected(ComponentName componentName)
      {
         data = null;
      }
   };

   /**
    * Initialise the data service.
    */
   protected void init()
   {
      bindService(new Intent(this, TestsService.class), dataConnection, BIND_AUTO_CREATE);
   }

   /**
    * Event invoked when the data and remote services are ready.
    */
   protected void onServicesReady()
   {
   }

   /**
    * Perform any final cleanup before an activity is destroyed.
    */
   @Override
   protected void onDestroy()
   {
      super.onDestroy();

      if(data != null)
         unbindService(dataConnection);
   }
}
