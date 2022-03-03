package com.example.rtttest1;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LocalizationActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "LocalizationActivity";

    //For RTT service
    private WifiRttManager myWifiRTTManager;
    private WifiManager myWifiManager;
    private RTTRangingResultCallback myRTTRangingResultCallback;
    private WifiScanReceiver myWifiScanReceiver;

    List<ScanResult> RTT_APs = new ArrayList<>();
    List<RangingResult> Ranging_Results = new ArrayList<>();
    List<String> APs_MacAddress = new ArrayList<>();

    final Handler RangingRequestDelayHandler = new Handler();

    //For IMU service
    private SensorManager sensorManager;
    private final HashMap<String, Sensor> sensors = new HashMap<>();

    private float accx, accy, accz, gyrox, gyroy, gyroz, magx, magy, magz;
    private long IMU_timestamp;

    //flag for leaving the activity
    Boolean Running = true;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate() LocalizationActivity");

        //receive RTT_APs from main activity
        Intent intent = getIntent();
        RTT_APs = intent.getParcelableArrayListExtra("SCAN_RESULT");

        if (RTT_APs == null || RTT_APs.isEmpty()) {
            Log.d(TAG, "RTT_APs null");
            Toast.makeText(getApplicationContext(),
                    "Please scan for available APs first",
                    Toast.LENGTH_SHORT).show();
            finish();
        } else {
            setContentView(R.layout.activity_localization_test);

            //RTT Initiation
            myWifiRTTManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
            myWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            myRTTRangingResultCallback = new RTTRangingResultCallback();
            myWifiScanReceiver = new WifiScanReceiver();

            registerReceiver(myWifiScanReceiver,
                    new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

            for (ScanResult AP:RTT_APs){
                APs_MacAddress.add(AP.BSSID);
            }

            //IMU Initiation
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            sensors.put("Accelerometer", sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
            sensors.put("Gyroscope", sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
            sensors.put("Magnetic", sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));

            //Localization initiation
            ImageView floor_plan = findViewById(R.id.imageViewFloorplan);
            int[] location = new int[2];
            floor_plan.getLocationOnScreen(location);

            Log.d(TAG,"X: "+location[0]+"Y: "+location[1]);
            //floor_plan.getImageMatrix();
            //TODO

            registerSensors();
            startRangingRequest();
            startLoggingData();
            startScanInBackground();
            Log.d(TAG,"Start localization");
        }
    }

    private void set_AP_pins(){

    }

    private void update_location_pin(){

    }

    @SuppressLint("MissingPermission")
    private void startRangingRequest() {
        RangingRequest rangingRequest =
                new RangingRequest.Builder().addAccessPoints(RTT_APs).build();

        myWifiRTTManager.startRanging(
                rangingRequest, getApplication().getMainExecutor(), myRTTRangingResultCallback);
    }

    private void startLoggingData(){
        Log.d(TAG,"StartLoggingData() LocalizationActivity");

        String url = "http://192.168.86.47:5000/server";
        final OkHttpClient client = new OkHttpClient();

        Handler LogRTT_Handler = new Handler();
        Runnable LogRTT_Runnable = new Runnable() {
            @Override
            public void run() {
                if (Running){
                    LogRTT_Handler.postDelayed(this,200);

                    List<String> RangingInfo = new ArrayList<>();
                    for (RangingResult result:Ranging_Results){
                        RangingInfo.add(String.valueOf(result.getMacAddress()));
                        RangingInfo.add(String.valueOf(result.getDistanceMm()));
                        RangingInfo.add(String.valueOf(result.getDistanceStdDevMm()));
                        RangingInfo.add(String.valueOf(result.getRssi()));
                    }

                    RequestBody RTT_body = new FormBody.Builder()
                            .add("Flag","RTT")
                            .add("Timestamp", String.valueOf(SystemClock.elapsedRealtime()))
                            .add("RTT_Result", String.valueOf(RangingInfo))
                            .build();

                    Request RTT_request = new Request.Builder()
                            .url(url)
                            .post(RTT_body)
                            .build();

                    final Call call = client.newCall(RTT_request);
                    call.enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            Log.i("onFailure",e.getMessage());
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response)
                                throws IOException{
                            String result = Objects.requireNonNull(response.body()).string();
                            //String result = response.body().string();
                            response.close();
                            Log.i("result",result);
                        }
                    });
                } else {
                    LogRTT_Handler.removeCallbacks(this);
                }
            }
        };

        Handler LogIMU_Handler = new Handler();
        Runnable LogIMU_Runnable = new Runnable() {
            @Override
            public void run() {
                if (Running) {
                    LogIMU_Handler.postDelayed(this,50);
                    RequestBody IMU_Body = new FormBody.Builder()
                            .add("Flag","IMU")
                            .add("Timestamp",String.valueOf(IMU_timestamp))
                            .add("accx", String.valueOf(accx))
                            .add("accy", String.valueOf(accy))
                            .add("accz", String.valueOf(accz))
                            .add("gyrox", String.valueOf(gyrox))
                            .add("gyroy", String.valueOf(gyroy))
                            .add("gyroz", String.valueOf(gyroz))
                            .add("magx", String.valueOf(magx))
                            .add("magy", String.valueOf(magy))
                            .add("magz", String.valueOf(magz))
                            .build();

                    Request IMU_Request = new Request.Builder()
                            .url(url)
                            .post(IMU_Body)
                            .build();

                    final Call call = client.newCall(IMU_Request);
                    call.enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            Log.i("onFailure",e.getMessage());
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response)
                                throws IOException{
                            String result = Objects.requireNonNull(response.body()).string();
                            response.close();
                            Log.i("result",result);
                        }
                    });
                } else {
                    LogIMU_Handler.removeCallbacks(this);
                }
            }
        };
        //wait x ms (only once) before running
        LogIMU_Handler.postDelayed(LogIMU_Runnable,1000);
        LogRTT_Handler.postDelayed(LogRTT_Runnable,1000);
    }

    private void startScanInBackground(){
        Handler BackgroundScan_Handler = new Handler();
        Runnable BackgroundScan_Runnable = new Runnable() {
            @Override
            public void run() {
                if (Running) {
                    BackgroundScan_Handler.postDelayed(this,3000);
                    myWifiManager.startScan();
                } else {
                    BackgroundScan_Handler.removeCallbacks(this);
                }
            }
        };
        BackgroundScan_Handler.postDelayed(BackgroundScan_Runnable,1000);
    }

    private void registerSensors(){
        for (Sensor eachSensor:sensors.values()){
            sensorManager.registerListener(this,
                    eachSensor,SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    public void unregisterSensors(){
        for (Sensor eachSensor:sensors.values()){
            sensorManager.unregisterListener(this,eachSensor);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        IMU_timestamp = SystemClock.elapsedRealtime();
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                accx = sensorEvent.values[0];
                accy = sensorEvent.values[1];
                accz = sensorEvent.values[2];
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magx = sensorEvent.values[0];
                magy = sensorEvent.values[1];
                magz = sensorEvent.values[2];
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyrox = sensorEvent.values[0];
                gyroy = sensorEvent.values[1];
                gyroz = sensorEvent.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private class WifiScanReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            for (ScanResult scanResult:myWifiManager.getScanResults()){
                if (scanResult.is80211mcResponder()) {
                    if (!APs_MacAddress.contains(scanResult.BSSID)) {
                        RTT_APs.add(scanResult);
                    }
                }
            }
            //TODO Handle getmaxpeers
        }
    }

    private class RTTRangingResultCallback extends RangingResultCallback {

        //Start next request
        private void queueNextRangingRequest() {
            RangingRequestDelayHandler.postDelayed(
                    LocalizationActivity.this::startRangingRequest, 100);
        }

        @Override
        public void onRangingFailure(int i) {
            queueNextRangingRequest();
        }

        @SuppressLint("WrongConstant")
        @Override
        public void onRangingResults(@NonNull List<RangingResult> list) {
            Ranging_Results.clear();
            for (RangingResult result:list) {
                if (result.getStatus() == 0){
                    Ranging_Results.add(result);
                }
            }
            queueNextRangingRequest();
        }
    }



    @Override
    protected void onStop() {
        Log.d(TAG, "onStop() LocalizationActivity");
        super.onStop();
        unregisterSensors();
        unregisterReceiver(myWifiScanReceiver);
        Running = false;
    }

    protected void onResume() {
        Log.d(TAG,"onResume() LocalizationActivity");
        super.onResume();
        Running = true;
    }
}
