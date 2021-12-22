package com.example.rtttest1;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.View;

import android.content.pm.PackageManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private boolean LocationPermission = false;

    ArrayList<ScanResult> RTT_APs;

    private WifiManager myWifiManager;
    private WifiScanReceiver myWifiReceiver;

    private TextView ScanResultDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        myWifiReceiver = new WifiScanReceiver();

        ScanResultDisplay = findViewById(R.id.ScanResult);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume MainActivity");
        super.onResume();

        // each time resume back in onResume state, check location permission
        LocationPermission = ActivityCompat.checkSelfPermission(
                this, permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        Log.d(TAG, "Location permission:" + LocationPermission);

        //register a Broadcast receiver to run in the main activity thread
        registerReceiver(
                myWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop MainActivity");
        super.onStop();
        unregisterReceiver(myWifiReceiver);
    }

    //Scan surrounding WiFi points
    public void onClickScanAPs(View view) {
        Log.d(TAG,"onClickScanAPs()");

        if (LocationPermission) {
            Log.d(TAG, "Scanning...");
            myWifiManager.startScan();

            Snackbar.make(view, "Scanning...", Snackbar.LENGTH_LONG).show();

        } else {
            // request permission
            Intent IntentRequestPermission = new Intent(this,
                    LocationPermissionRequest.class);
            startActivity(IntentRequestPermission);
        }
    }

    //Start ranging in a new screen
    public void onClickRangingAPs(View view) {
        Log.d(TAG,"onClickRangingAPs()");

        Intent IntentRanging = new Intent(getApplicationContext(), RangingActivity.class);
        //Pass RTT_APs to next activity
        IntentRanging.putParcelableArrayListExtra("SCAN_RESULT",RTT_APs);
        startActivity(IntentRanging);
    }

    private class WifiScanReceiver extends BroadcastReceiver {

        //Only keep RTT supported APs from the original scan list
        private List<ScanResult> findRTTAPs(@NonNull List<ScanResult> OriginalList) {
            List<ScanResult> RTT_APs = new ArrayList<>();

            for (ScanResult scanResult : OriginalList) {
                if (scanResult.is80211mcResponder()) {
                    RTT_APs.add(scanResult);
                }
            }
            return RTT_APs;
        }

        //Add to avoid permission check for each scan
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive MainActivity");

            List<ScanResult> scanResults = myWifiManager.getScanResults();
            RTT_APs = (ArrayList<ScanResult>) findRTTAPs(scanResults);
            Log.d(TAG, "All WiFi points\n" + scanResults);
            Log.d(TAG, "RTT APs\n" + RTT_APs);

            if (!RTT_APs.isEmpty()){
                //TODO better display
                ScanResultDisplay.setText(String.valueOf(RTT_APs));

            } else{
                String NO_AP = "No RTT APs available";
                ScanResultDisplay.setText(NO_AP);
                Log.d(TAG,NO_AP);
            }
        }
    }

    //Check RTT availability of the device
    public void onClickCheckRTTAvailability(View view){
        Log.d(TAG,"Checking RTT Availability");
        boolean RTT_availability = getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT);

        if (RTT_availability) {
            Snackbar.make(view, "RTT supported on this device :)",
                    Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(view, "RTT not supported on this device :(",
                    Snackbar.LENGTH_LONG).show();
        }
    }

}