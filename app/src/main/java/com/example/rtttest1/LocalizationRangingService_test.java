package com.example.rtttest1;

import android.annotation.SuppressLint;
import android.app.Service;
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
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LocalizationRangingService_test extends Service implements SensorEventListener {

    private Looper serviceLooper;

    private static final String TAG = "LocalizationRangingService";

    //For communication with activity
    private final IBinder myBinder = new MyBinder();

    //For RTT service
    private WifiRttManager myWifiRTTManager;
    private WifiManager myWifiManager;
    private RangingActivityAdapter myRangingActivityAdapter;
    private RTTRangingResultCallback myRTTRangingResultCallback;
    private WifiScanReceiver myWifiScanReceiver;

    private int RangingDelay = 100;

    List<ScanResult> RTT_APs = new ArrayList<>();
    List<RangingResult> Ranging_Results = new ArrayList<>();

    final Handler RangingRequestDelayHandler = new Handler();

    //For IMU service
    private SensorManager sensorManager;
    private final HashMap<String, Sensor> sensors = new HashMap<>();

    private float accx, accy, accz, gyrox, gyroy, gyroz, magx, magy, magz;
    private long IMU_timestamp;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //When service starts and intent is passed
    protected void onHandleIntent(@Nullable Intent intent) {
        RTT_APs = intent.getParcelableArrayListExtra("SCAN_RESULT");
        Log.d(TAG, String.valueOf(RTT_APs));
        //TODO test.1
        //TODO Handle RTT_APs null/empty

        //RTT Initiation
        myWifiRTTManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        myWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        myRTTRangingResultCallback = new RTTRangingResultCallback();
        myWifiScanReceiver = new WifiScanReceiver();
        myRangingActivityAdapter = new RangingActivityAdapter(Ranging_Results);

        registerReceiver(myWifiScanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        //IMU Initiation
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensors.put("Accelerometer", sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        sensors.put("Gyroscope", sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
        sensors.put("Magnetic", sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));

        registerSensors();
        startRangingRequest();
        startLoggingData();
    }

    private class MyBinder extends Binder {
        LocalizationRangingService_test getService() {
            return LocalizationRangingService_test.this;
        }
    }

    @SuppressLint("MissingPermission")
    private void startRangingRequest() {
        RangingRequest rangingRequest =
                new RangingRequest.Builder().addAccessPoints(RTT_APs).build();

        myWifiRTTManager.startRanging(
                rangingRequest, getApplication().getMainExecutor(), myRTTRangingResultCallback);
    }

    private void startLoggingData(){
        //TODO snackbar
        String url = "http://192.168.86.34:5000/server";
        final OkHttpClient client = new OkHttpClient();

        Handler LogRTT_Handler = new Handler();
        Runnable LogRTT_Runnable = new Runnable() {
            @Override
            public void run() {
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
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        response.close();
                        Log.i("result",String.valueOf(response.body()));
                    }
                });
            }
        };

        Handler LogIMU_Handler = new Handler();
        Runnable LogIMU_Runnable = new Runnable() {
            @Override
            public void run() {
                LogIMU_Handler.postDelayed(this,20);
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
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        response.close();
                        Log.i("result",String.valueOf(response.body()));
                    }
                });
            }
        };
        //wait x ms (only once) before running
        LogIMU_Handler.postDelayed(LogIMU_Runnable,1000);
        LogRTT_Handler.postDelayed(LogRTT_Runnable,1000);

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

    private class RTTRangingResultCallback extends RangingResultCallback{

        //Start next request
        private void queueNextRangingRequest() {
            RangingRequestDelayHandler.postDelayed(
                    LocalizationRangingService_test.this::startRangingRequest,RangingDelay);
        }

        @Override
        public void onRangingFailure(int i) {
            queueNextRangingRequest();
        }

        @SuppressLint("WrongConstant")
        @Override
        public void onRangingResults(@NonNull List<RangingResult> list) {
            List<RangingResult> successful_requests = new ArrayList<>();
            for (RangingResult result:list) {
                if (result.getStatus() == 0){
                    successful_requests.add(result);
                }
            }

            queueNextRangingRequest();
        }
    }

    private class WifiScanReceiver extends BroadcastReceiver {
        //Only keep RTT APs
        private List<ScanResult> findRTTAPs(@NonNull List<ScanResult> OriginalList){
            List<ScanResult> new_list = new ArrayList<>();
            for (ScanResult scanResult:OriginalList){
                if (scanResult.is80211mcResponder()){
                    new_list.add(scanResult);
                }
            }
            return new_list;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            RTT_APs = findRTTAPs(myWifiManager.getScanResults());
        }
    }
}
