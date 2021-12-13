package com.example.rtttest1;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Bundle;

import com.google.android.material.snackbar.BaseTransientBottomBar;
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

    List<ScanResult> APsSupportingRTT;

    private WifiManager myWifiManager;
    private WifiScanReceiver myWifiReceiver;
    private WifiRttManager myWifiRTTManager;
    private TextView AP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        myWifiReceiver = new WifiScanReceiver();
        myWifiRTTManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);

        APsSupportingRTT = new ArrayList<>();

        AP = findViewById(R.id.textView1);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();

        // each time resume back in onResume state, check location permission
        LocationPermission = ActivityCompat.checkSelfPermission(
                this, permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        Log.d(TAG, String.valueOf(LocationPermission));

        //register a Broadcast receiver to run in the main activity thread
        registerReceiver(
                myWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        unregisterReceiver(myWifiReceiver);
    }


    //Scan surrounding WiFi points
    public void onClickScanAPs(View view) {
        Log.d(TAG, "onClickScanAPs");

        if (LocationPermission) {
            myWifiManager.startScan();

            Log.d(TAG, "Scanning");
            Snackbar.make(view, "Scanning...", BaseTransientBottomBar.LENGTH_LONG).show();

        } else {
            Intent IntentRequestPermission = new Intent(this,
                    LocationPermissionRequest.class);
            startActivity(IntentRequestPermission);
        }
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
            Log.d(TAG, "onReceive()");
            List<ScanResult> scanResults = myWifiManager.getScanResults();
            APsSupportingRTT = findRTTAPs(scanResults);
            AP.setText(String.valueOf(APsSupportingRTT));

            RangingRequest.Builder builder = new RangingRequest.Builder();
            builder.addAccessPoints(APsSupportingRTT);
            Log.d(TAG, String.valueOf(APsSupportingRTT));
            final RangingRequest request = new RangingRequest.Builder()
                    .addAccessPoints(APsSupportingRTT).build();

            final RangingResultCallback callback = new RangingResultCallback() {
                @Override
                public void onRangingFailure(int i) {
                    Log.d(TAG, "Ranging failed");
                }

                @Override
                public void onRangingResults(@NonNull List<RangingResult> list) {
                    Log.d(TAG, String.valueOf(list));
                }
            };
            myWifiRTTManager.startRanging(request,getApplication().getMainExecutor(),callback);
        }

                /*
            for (ScanResult scanResult : APsSupportingRTT) {
                RangingRequest.Builder builder = new RangingRequest.Builder();
                builder.addAccessPoint(scanResult);
                Log.d(TAG, String.valueOf(scanResult));
                final RangingRequest request = new RangingRequest.Builder()
                        .addAccessPoint(scanResult).build();
                final RangingResultCallback callback = new RangingResultCallback() {
                    @Override
                    public void onRangingFailure(int i) {
                        Log.d(TAG, "Ranging failed");
                    }

                    @Override
                    public void onRangingResults(@NonNull List<RangingResult> list) {
                        Log.d(TAG, String.valueOf(list));
                    }
                };
                myWifiRTTManager.startRanging(request, (Executor) callback, null);
            }

                 */
    }

    //Check RTT availability of the device
    public void onClickCheckRTTAvailability(View view){
        Log.d(TAG,"Checking RTT Availability");
        boolean RTT_availability = getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT);

        if (RTT_availability) {
            Snackbar.make(view, "RTT supported on this device :)",
                    BaseTransientBottomBar.LENGTH_LONG).show();
        } else {
            Snackbar.make(view, "RTT not supported on this device :(",
                    BaseTransientBottomBar.LENGTH_LONG).show();
        }
    }
}