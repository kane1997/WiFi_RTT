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
import android.view.Menu;
import android.view.MenuItem;

import android.content.pm.PackageManager;
import android.content.IntentFilter;

import android.net.wifi.rtt.WifiRttManager;
import android.net.wifi.rtt.RangingRequest;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WifiRttManager RttManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get instance of WifiRTTManager
        RttManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
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

}