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

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/**
 * Service that manages a singleton TestsData object and shares it within the application.
 */
public class TestsService extends Service
{
   /**
    * Intermediate class to pass on a reference to the data.
    */
   public class ServiceBinder extends Binder
   {
      /**
       * Returns a reference to the TestsService active.
       */
      public TestsData getService()
      {
         return TestsService.this.data;
      }
   }


   /**
    * Actual data accessing object.
    */
   protected TestsData data;


   /**
    * Called by the system every time a client explicitly starts the service by calling
    * startService(Intent), providing the arguments it supplied and a unique integer token
    * representing the start request.
    */
   @Override
   public int onStartCommand(Intent intent, int flags, int startId)
   {
      return START_NOT_STICKY;
   }

   /**
    * Return the communication channel to the service.
    * Note that unlike other application components, calls on to the IBinder interface returned
    * here may not happen on the main thread of the process.
    * @param intent The Intent that was used to bind to this service, as given to Context.bindService.
    *    Note that any extras that were included with the Intent at that point will not be seen here.
    * @return Return an IBinder through which clients can call on to the service.
    */
   @Override
   public IBinder onBind(Intent intent)
   {
      //Log.i("TestsService", "onBind");
      if(data == null)
      {
         data = new TestsData(getApplicationContext());
      }

      return new ServiceBinder();
   }

   /**
    * Called when all clients have disconnected from a particular interface published by the service.
    * @return Return true if you would like to have the service's onRebind(Intent) method later
    *    called when new clients bind to it.
    */
   @Override
   public boolean onUnbind(Intent intent)
   {
      return false;
   }

   /**
    * Called by the system to notify a Service that it is no longer used and is being removed.
    */
   @Override
   public void onDestroy()
   {
      //Log.i("TestsService", "onDestroy");
      if(data != null)
      {
         data.close();
         data = null;
      }
   }
}
