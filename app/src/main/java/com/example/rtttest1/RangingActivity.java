package com.example.rtttest1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Send ranging requests and display distance and RSSI values
 */
public class RangingActivity extends AppCompatActivity {
    private static final String TAG = "RangingActivity";

    private WifiRttManager myWifiRTTManager;
    private RTTRangingResultCallback myRTTResultCallback;
    private static final int RangingDelayDefault = 100;

    //flag for leaving the activity
    Boolean Running = true;

    private EditText RangingDelayEditText;

    private int RangingDelay;
    private int i = 0;

    final Handler RangingRequestDelayHandler = new Handler();

    //For IMU
    /*
    private SensorManager sensorManager;
    private float AccX,AccY,AccZ,GyroX,GyroY,GyroZ,MagX,MagY,MagZ;
     */

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

            TextView mac_Text1 = findViewById(R.id.Mac_1);
            TextView mac_Text2 = findViewById(R.id.Mac_2);
            TextView mac_Text3 = findViewById(R.id.Mac_3);

            for (ScanResult s:RTT_APs){
                switch(i){
                    case 0:
                        mac_Text1.setText(s.BSSID);
                        break;
                    case 1:
                        mac_Text2.setText(s.BSSID);
                        break;
                    case 2:
                        mac_Text3.setText(s.BSSID);
                        break;
                }
                i++;
            }

            RangingDelayEditText = findViewById(R.id.delayValue);
            RangingDelayEditText.setText(String.format("%d", RangingDelayDefault));

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

        RangingDelay = Integer.parseInt(RangingDelayEditText.getText().toString());
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Stopping ranging activity...");
        super.onStop();
        Running = false;
    }

    public void onClickLogRTT(View view){
        Log.d(TAG,"onClickLogRTT()");

        Handler LogHandler = new Handler();
        Runnable Logrunnable = new Runnable() {
            @Override
            public void run() {
                if (!Running) {
                    LogHandler.removeCallbacks(this);
                }else{
                    // rate of packet sending
                    LogHandler.postDelayed(this,100);

                    //IP address of Nest Router
                    String url = "http://192.168.86.24:5000/server";

                    //IP address of personal hotspot (not working)
                    //String url = "http://172.20.10.2:5000/server";

                    //IP address of Yilun's laptop
                    //String url = "http://192.168.137.179:5000/server";

                    OkHttpClient client = new OkHttpClient.Builder().build();

                    RequestBody body = new FormBody.Builder()
                            .add("Timestamp", (String) ((TextView)
                                    findViewById(R.id.timestamp)).getText())
                            .add("Mac_1", (String) ((TextView)
                                    findViewById(R.id.Mac_1)).getText())
                            .add("Distance_1", (String) ((TextView)
                                    findViewById(R.id.Distance_1)).getText())
                            .add("RSSI_1", (String) ((TextView)
                                    findViewById(R.id.RSSI_1)).getText())
                            .add("Mac_2", (String) ((TextView)
                                    findViewById(R.id.Mac_2)).getText())
                            .add("Distance_2", (String) ((TextView)
                                    findViewById(R.id.Distance_2)).getText())
                            .add("RSSI_2", (String) ((TextView)
                                    findViewById(R.id.RSSI_2)).getText())
                            .add("Mac_3", (String) ((TextView)
                                    findViewById(R.id.Mac_3)).getText())
                            .add("Distance_3", (String) ((TextView)
                                    findViewById(R.id.Distance_3)).getText())
                            .add("RSSI_3", (String) ((TextView)
                                    findViewById(R.id.RSSI_3)).getText())
                            .build();

                    Request request = new Request.Builder()
                            .url(url)
                            .post(body)
                            .build();

                    final Call call = client.newCall(request);

                    call.enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Log.i("onFailure",e.getMessage());
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            String result = response.body().string();
                            Log.i("result", result);
                        }
                    });
                }
            }
        };
        // wait x ms (only once) before running
        LogHandler.postDelayed(Logrunnable,1000);
    }

    private class RTTRangingResultCallback extends RangingResultCallback {

        private void queueNextRangingRequest(){
            RangingRequestDelayHandler.postDelayed(
                    RangingActivity.this::startRangingRequest,
                    RangingDelay);
        }

        @Override
        public void onRangingFailure(int i) {
            Log.d(TAG,"Ranging failed！");
            queueNextRangingRequest();
        }

        @Override
        public void onRangingResults(@NonNull List<RangingResult> list) {
            Log.d(TAG,"Ranging successful");
            Log.d(TAG, list.toString());

            Integer status
                    = list.get(0).getStatus() + list.get(1).getStatus() + list.get(2).getStatus();
            if (Running && status == 0){
                //Log.d(TAG, String.valueOf(Running && status == 0));
                TextView distance_Text1 = findViewById(R.id.Distance_1);
                TextView distance_Text2 = findViewById(R.id.Distance_2);
                TextView distance_Text3 = findViewById(R.id.Distance_3);
                TextView RSSI_Text1 = findViewById(R.id.RSSI_1);
                TextView RSSI_Text2 = findViewById(R.id.RSSI_2);
                TextView RSSI_Text3 = findViewById(R.id.RSSI_3);
                TextView timestamp_Text = findViewById(R.id.timestamp);

                distance_Text1.setText(String.valueOf(list.get(0).getDistanceMm()));
                distance_Text2.setText(String.valueOf(list.get(1).getDistanceMm()));
                distance_Text3.setText(String.valueOf(list.get(2).getDistanceMm()));
                RSSI_Text1.setText(String.valueOf(list.get(0).getRssi()));
                RSSI_Text2.setText(String.valueOf(list.get(1).getRssi()));
                RSSI_Text3.setText(String.valueOf(list.get(2).getRssi()));
                timestamp_Text.setText(String.valueOf(list.get(0).getRangingTimestampMillis()));

                //TODO recycler view on results
            }
            if (Running){
                queueNextRangingRequest();
            }
        }
    }

}
