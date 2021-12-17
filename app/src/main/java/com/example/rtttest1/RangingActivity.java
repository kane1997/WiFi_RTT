package com.example.rtttest1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * A ranging request returns the average distance of 7 readings from a burst
 */
public class RangingActivity extends AppCompatActivity {
    private static final String TAG = "RangingActivity";

    private WifiRttManager myWifiRTTManager;
    private RTTRangingResultCallback myRTTResultCallback;

    //flag for leaving the activity
    Boolean Running = true;

    private TextView RangingResultList;

    //Can be regarded as an approximation of sampling rate
    private static final int RangingDelay = 1000;

    final Handler RangingRequestDelayHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //receive RTT_APs from main activity
        Intent intent = getIntent();
        ArrayList<ScanResult> RTT_APs = intent.getParcelableArrayListExtra("SCAN_RESULT");

        if (RTT_APs == null || RTT_APs.isEmpty()) {
            Log.d(TAG,"RTT_APs null");
            Toast.makeText(getApplicationContext(),
                    "Please scan for available APs first",
                    Toast.LENGTH_SHORT).show();

            finish();
        } else {
            setContentView(R.layout.activity_ranging);
            Log.d(TAG, "RTT_APs passed to RangingActivity.java \n" + RTT_APs);

            RangingResultList = findViewById(R.id.RangingResultList);
            myWifiRTTManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
            myRTTResultCallback = new RTTRangingResultCallback();
            startRangingRequest();
        }
    }

    @SuppressLint("MissingPermission")
    private void startRangingRequest() {
        Log.d(TAG,"startingRangingRequest");

        Intent intent = getIntent();
        ArrayList<ScanResult> RTT_APs = intent.getParcelableArrayListExtra("SCAN_RESULT");

        RangingRequest rangingRequest =
                new RangingRequest.Builder().addAccessPoints(RTT_APs).build();

        myWifiRTTManager.startRanging(
                rangingRequest,getApplication().getMainExecutor(),myRTTResultCallback);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Stopping ranging activity...");
        super.onStop();
        Running = false;
    }

    private class RTTRangingResultCallback extends RangingResultCallback {

        private void queueNextRangingRequest(){
            RangingRequestDelayHandler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                        startRangingRequest(); }
                        },
                    RangingDelay);
        }

        @Override
        public void onRangingFailure(int i) {
            Log.d(TAG,"Ranging failed");
            queueNextRangingRequest();
        }

        @Override
        public void onRangingResults(@NonNull List<RangingResult> list) {
            Log.d(TAG,"Ranging successful");
            Log.d(TAG, String.valueOf(list));
            RangingResultList.setText(String.valueOf(list));

            if (Running){
                queueNextRangingRequest();
            }
        }
    }
}
