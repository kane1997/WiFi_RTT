package com.example.rtttest1;

import android.Manifest;
import android.Manifest.permission;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.view.View;

import android.content.pm.PackageManager;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private boolean LocationPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();

        // each time resume back in onResume state check location permission
        LocationPermission = ActivityCompat.checkSelfPermission(this,
                permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        Log.d(TAG, String.valueOf(LocationPermission));
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        //TODO save battery and memory
    }

    public void onClickScanAPs(View view){
        Log.d(TAG, "onClickScanAPs");

        Snackbar Scan_msg;
        if (LocationPermission) {
            //TODO scan
            Log.d(TAG,"Scanning");

            Scan_msg = Snackbar.make(view, "Scanning",
                    BaseTransientBottomBar.LENGTH_LONG);
            Scan_msg.show();

        } else {
            Intent IntentRequestPermission = new Intent(this,
                    LocationPermissionRequest.class);
            startActivity(IntentRequestPermission);
        }
    }

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
        Log.d(TAG,"Checking RTT Availability");

        boolean RTT_availability = getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT);
        Snackbar RTT_support;

        if (RTT_availability) {
            RTT_support = Snackbar.make(view, "RTT supported on this device :)",
                    BaseTransientBottomBar.LENGTH_LONG);
        } else {
            RTT_support = Snackbar.make(view, "RTT not supported on this device :(",
                    BaseTransientBottomBar.LENGTH_LONG);
        }
        RTT_support.show();
    }

}