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

import android.content.Intent;
import android.support.design.widget.NavigationView;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * Class base for activities with navigation menu.
 */
public class NavigationActivity extends ServicesActivity implements NavigationView.OnNavigationItemSelectedListener
{
   /**
    * Installs the navigation menu controls.
    * Requires the layout to have the controls: toolbar, drawer_layout and nav_view.
    */
   @Override
   protected void init()
   {
      Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);

      DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
      assert drawer != null;
      ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_open, R.string.navigation_close);
      drawer.setDrawerListener(toggle);
      toggle.syncState();

      NavigationView nav = (NavigationView) findViewById(R.id.nav_view);
      assert nav != null;
      nav.setNavigationItemSelectedListener(this);

      super.init();
   }

   @Override
   protected void onDestroy()
   {
      super.onDestroy();
   }

   /**
    * Event invoked when the data is ready.
    */
   @Override
   protected void onServicesReady()
   {
      super.onServicesReady();

      refreshNavBar();
   }

   /**
    * Refreshes the statistics in the navigation bar.
    */
   protected void refreshNavBar()
   {
      if(data == null)
         return;

      NavigationView nav = (NavigationView) findViewById(R.id.nav_view);
      assert nav != null;
      Menu menu = nav.getMenu();
      /*
      MenuItem item = menu.findItem(R.id.nav_my_cycles);
      TextView cycles = (TextView) item.getActionView();
      cycles.setText(Integer.toString(data.countCycles()));
      */

      MenuItem item = menu.findItem(R.id.nav_my_tests);
      TextView tests = (TextView) item.getActionView();
      tests.setText(Integer.toString(data.countCycles()));
   }

   /**
    * Called when the activity has detected the user's press of the back key.
    */
   @Override
   public void onBackPressed()
   {
      DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
      assert drawer != null;
      if(drawer.isDrawerOpen(GravityCompat.START))
      {
         drawer.closeDrawer(GravityCompat.START);
      }
      else
      {
         onFinishRequest();
      }
   }

   /**
    * Called when the activity is actually leaving after a back button press.
    */
   public void onFinishRequest()
   {
      super.onBackPressed();
   }

   /**
    * Called when an item in the navigation menu is selected.
    *
    * @param item The selected item
    * @return true to display the item as the selected item
    */
   @Override
   public boolean onNavigationItemSelected(MenuItem item)
   {
      switch(item.getItemId())
      {
         case R.id.nav_home:
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            break;

         case R.id.nav_new_test:
            TaskStackBuilder.create(this).addParentStack(NewTestActivity.class).addNextIntent(new Intent(this, NewTestActivity.class)).startActivities();
            break;

         case R.id.nav_my_tests:
            TaskStackBuilder.create(this).addParentStack(MyCyclesActivity.class).addNextIntent(new Intent(this, MyCyclesActivity.class)).startActivities();
            break;

         case R.id.nav_my_cycles:
            TaskStackBuilder.create(this).addParentStack(ChartsActivity.class).addNextIntent(new Intent(this, ChartsActivity.class)).startActivities();
            break;

         case R.id.nav_information:
            TaskStackBuilder.create(this).addParentStack(InformationActivity.class).addNextIntent(new Intent(this, InformationActivity.class)).startActivities();
            break;

         case R.id.nav_settings:
            TaskStackBuilder.create(this).addParentStack(SettingsActivity.class).addNextIntent(new Intent(this, SettingsActivity.class)).startActivities();
            break;

         case R.id.nav_help:
            TaskStackBuilder.create(this).addParentStack(HelpActivity.class).addNextIntent(new Intent(this, HelpActivity.class)).startActivities();
            break;

         case R.id.nav_about:
            intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(HomeActivity.DATA_MESSAGE, getString(R.string.copyright));
            startActivity(intent);
            break;
      }

      DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
      assert drawer != null;
      drawer.closeDrawer(GravityCompat.START);
      return true;
   }
}
