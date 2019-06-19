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

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * This activity informs the user about the strip tests abd how this application can help her.
 */
public class HelpActivity extends NavigationActivity
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
      setContentView(R.layout.activity_help);

      init();
   }
}
