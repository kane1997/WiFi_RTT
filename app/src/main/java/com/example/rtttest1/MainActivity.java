package com.example.rtttest1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Bundle;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.rtttest1.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;

import android.content.pm.PackageManager;
import android.content.IntentFilter;

import android.net.wifi.rtt.WifiRttManager;
import android.net.wifi.rtt.RangingRequest;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private WifiRttManager RttManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //get instance of WifiRTTManager
        RttManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }

        });

    }

    /*
    // in onResume state check location permission
    @Override
    public void onResume(){

    }
    */

    /*
    // receive WiFi RTT status change
    IntentFilter filter = new IntentFilter(WifiRttManager.ACTION_WIFI_RTT_STATE_CHANGED);
    BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiRttManager.isAvailable()) {
                //TODO something
            } else {
                //TODO something else
            }
        }
    };
    this.registerReceiver(myReceiver, filter);
    */

    //Check RTT availability of the device
    public void onClickCheckRTTAvailability(View view){
        boolean RTT_availability = getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT);
        Snackbar RTT_support;

        if (RTT_availability) {
            RTT_support = Snackbar.make(view, "RTT supported",
                    BaseTransientBottomBar.LENGTH_LONG);
        } else {
            RTT_support = Snackbar.make(view, "RTT not supported",
                    BaseTransientBottomBar.LENGTH_LONG);
        }
        RTT_support.show();
    }

    public void onClickScanAPs(View view){
        //TODO
    }

    /*
    //https://www.py4u.net/discuss/676888
    final RangingRequest rttRequest = new RangingRequest.Builder().
            addAccessPoint(scanResult).build();
    final RangingResultCallback callback = new RangingResultCallback() {
        @Override
        public void onRangingFailure(int i) {
            //TODO handle failure
        }

        @Override
        public void onRangingResults(@NonNull List<RangingResult> list) {
            //TODO handle result
        }
    }
    // start ranging and return results on main thread
    //RttManager.startRanging(request,callback,null);
    */


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }


}