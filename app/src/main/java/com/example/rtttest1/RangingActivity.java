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
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
//TODO better layout(linear?) and display?

public class RangingActivity extends AppCompatActivity implements SensorEventListener{
    private static final String TAG = "RangingActivity";

    //RTT
    private WifiManager myWifiManager;
    private WifiRttManager myWifiRTTManager;
    private WifiScanReceiver myWifiReceiver;
    private RTTRangingResultCallback myRTTResultCallback;
    private RangingActivityAdapter rangingActivityAdapter;

    List<ScanResult> RTT_APs = new ArrayList<>();
    List<RangingResult> Ranging_Result = new ArrayList<>();
    List<RangingResult> temp = new ArrayList<>();
    List<String> APs_MacAddress = new ArrayList<>();

    final Handler RangingRequestDelayHandler = new Handler();

    //IMU
    private SensorManager sensorManager;
    private final HashMap<String,Sensor> sensors = new HashMap<>();

    //flag for leaving the activity
    Boolean Running = true;

    private EditText RangingDelayEditText;
    private static final int RangingDelayDefault = 100;
    private int RangingDelay;

    private final float[] LastAccReading = new float[3];
    private final float[] LastGyroReading = new float[3];
    private final float[] LastMagReading = new float[3];

    private final float[] AccForOrientation = new float[3];
    private final float[] MagForOrientation = new float[3];
    private final float[] rotationMatrix = new float[9];
    private final float[] inclinationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];

    int IMU_num = 0;
    int RTT_num = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        RTT_APs = intent.getParcelableArrayListExtra("SCAN_RESULT");
        if (RTT_APs == null || RTT_APs.isEmpty()) {
            Log.d(TAG,"No RTT_APs");
            Toast.makeText(getApplicationContext(), "Please scan for available APs first",
                    Toast.LENGTH_SHORT).show();
            finish();
            //TODO edit toast
        } else {
            setContentView(R.layout.activity_ranging);
            Log.d(TAG, "RTT_APs passed to RangingActivity.java \n" + RTT_APs);

            RecyclerView myRecyclerView = findViewById(R.id.recyclerViewResults);
            myRecyclerView.setHasFixedSize(true);

            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
            myRecyclerView.setLayoutManager((layoutManager));

            RangingDelayEditText = findViewById(R.id.delayValue);
            RangingDelayEditText.setText(String.format(
                    Locale.getDefault(),"%d", RangingDelayDefault));

            //RTT
            myWifiRTTManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
            myRTTResultCallback = new RTTRangingResultCallback();
            myWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            myWifiReceiver = new WifiScanReceiver();
            registerReceiver(myWifiReceiver,
                    new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

            rangingActivityAdapter = new RangingActivityAdapter(Ranging_Result);
            myRecyclerView.setAdapter(rangingActivityAdapter);

            for (ScanResult AP:RTT_APs){
                APs_MacAddress.add(AP.BSSID);
            }

            //IMU
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

            sensors.put("Accelerometer",sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
            sensors.put("Gyroscope",sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
            sensors.put("Magnetic_field",sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));

            registerSensors();
            startRangingRequest();
        }
    }

    public void registerSensors(){
        for (Sensor eachSensor:sensors.values()){
            sensorManager.registerListener(this, eachSensor,SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    public void unregisterSensors(){
        for (Sensor eachSensor:sensors.values()){
            sensorManager.unregisterListener(this,eachSensor);
        }
    }

    @SuppressLint("MissingPermission")
    private void startRangingRequest() {
        RangingRequest rangingRequest =
                new RangingRequest.Builder().addAccessPoints(RTT_APs).build();

        myWifiRTTManager.startRanging(
                rangingRequest,getApplication().getMainExecutor(),myRTTResultCallback);

        String delay = RangingDelayEditText.getText().toString();
        if (!delay.equals("")){
            RangingDelay = Integer.parseInt(RangingDelayEditText.getText().toString());
        }else{
            Snackbar.make(findViewById(R.id.textViewDelayBeforeNextRequest),
                    "Please enter a valid number",Snackbar.LENGTH_SHORT).show();
            //TODO edit
        }
    }

    public void onClickBackgroundScan(View view){
        //TODO the button can only be clicked once
        Log.d(TAG,"Start scan in background");
        Snackbar.make(view,"Start scanning in background",Snackbar.LENGTH_SHORT).show();

        Thread scan_thread = new Thread(() -> {
            while (Running && (APs_MacAddress.size()<6)){
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                myWifiManager.startScan();
                Log.d(TAG,"Scanning in background");
            }
        });
        scan_thread.start();
    }

    public void onClickLogData(View view){
        Log.d(TAG,"onClickLogData()");
        Snackbar.make(view,"Start logging data",Snackbar.LENGTH_SHORT).show();

        EditText url_text = findViewById(R.id.editTextURL);
        //TODO optimise editText
        String url_bit = url_text.getText().toString();

        //IP address of Nest Router
        String url = "http://192.168.86." + url_bit + ":5000/server";

        final OkHttpClient client = new OkHttpClient();

        Thread RTT_thread = new Thread(() -> {
            while (Running) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                RTT_num++;
                List<String> RangingInfo = new ArrayList<>();

                for (RangingResult rangingResult: temp){
                    RangingInfo.add(String.valueOf(rangingResult.getMacAddress()));
                    RangingInfo.add(String.valueOf(rangingResult.getDistanceMm()));
                    RangingInfo.add(String.valueOf(rangingResult.getDistanceStdDevMm()));
                    RangingInfo.add(String.valueOf(rangingResult.getRssi()));
                }

                RequestBody RTT_Body = new FormBody.Builder()
                        .add("Flag","RTT")
                        .add("Timestamp", String.valueOf(SystemClock.elapsedRealtime()))
                        .add("num", String.valueOf(RTT_num))
                        .add("RTT_Result", String.valueOf(RangingInfo))
                        .build();

                Request RTT_Request = new Request.Builder()
                        .url(url)
                        .post(RTT_Body)
                        .build();

                final Call call = client.newCall(RTT_Request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.i("onFailure",e.getMessage());
                        Snackbar.make(view,"Failed to send data",Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response)
                            throws IOException {

                        String result = Objects.requireNonNull(response.body()).string();
                        response.close();
                        Log.i("result",result);
                    }
                });
            }
        });

        Thread IMU_thread = new Thread(() -> {
            while (Running) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                IMU_num++;
                Log.d(TAG, String.valueOf(IMU_num));
                RequestBody IMU_Body = new FormBody.Builder()
                        .add("Flag","IMU")
                        .add("Timestamp", String.valueOf(SystemClock.elapsedRealtimeNanos()))
                        .add("num",String.valueOf(IMU_num))
                        .add("Accx", String.valueOf(LastAccReading[0]))
                        .add("Accy", String.valueOf(LastAccReading[1]))
                        .add("Accz", String.valueOf(LastAccReading[2]))
                        .add("Gyrox", String.valueOf(LastGyroReading[0]))
                        .add("Gyroy", String.valueOf(LastGyroReading[1]))
                        .add("Gyroz", String.valueOf(LastGyroReading[2]))
                        .add("Magx", String.valueOf(LastMagReading[0]))
                        .add("Magy",String.valueOf(LastMagReading[1]))
                        .add("Magz",String.valueOf(LastMagReading[2]))
                        .add("Azimuth",String.valueOf(orientationAngles[0]))
                        .add("Pitch",String.valueOf(orientationAngles[1]))
                        .add("Roll",String.valueOf(orientationAngles[2]))
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
                        Snackbar.make(view,"Failed to send data",Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response)
                            throws IOException {
                        response.close();

                        //String result = Objects.requireNonNull(response.body()).string();
                        //Log.i("result",result);

                    }
                });
            }
        });
        IMU_thread.start();
        RTT_thread.start();
    }

    /*
    public void onClickLogData(View view){
        Snackbar.make(view,"Start sending data",Snackbar.LENGTH_SHORT).show();

        EditText url_text = findViewById(R.id.editTextURL);
        String url_bit = url_text.getText().toString();
        String url = "http://192.168.86." + url_bit + ":5000/server";
        //TODO editText

        final OkHttpClient client = new OkHttpClient();
        Handler LogRTT_Handler = new Handler();
        Runnable LogRTT_Runnable = new Runnable() {
            @Override
            public void run() {
                if (!Running){
                    LogRTT_Handler.removeCallbacks(this);
                } else{
                    //rate of RTT packet sending(optimal is 200)
                    LogRTT_Handler.postDelayed(this,200);
                    RTT_num++;
                    List<String> RangingInfo = new ArrayList<>();
                    for (RangingResult rangingResult: Ranging_Result){
                        RangingInfo.add(String.valueOf(rangingResult.getMacAddress()));
                        RangingInfo.add(String.valueOf(rangingResult.getDistanceMm()));
                        RangingInfo.add(String.valueOf(rangingResult.getDistanceStdDevMm()));
                        RangingInfo.add(String.valueOf(rangingResult.getRssi()));
                    }

                    RequestBody RTT_body = new FormBody.Builder()
                            .add("Flag","RTT")
                            .add("Timestamp", String.valueOf(SystemClock.elapsedRealtime()))
                            .add("num", String.valueOf(RTT_num))
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
                                throws IOException {
                            String result = Objects.requireNonNull(response.body()).string();
                            response.close();
                            Log.i("result",result);
                        }
                    });
                }
            }
        };

        Handler LogIMU_Handler = new Handler();
        Runnable LogIMU_Runnable = new Runnable() {
            @Override
            public void run() {
                if (!Running){
                    LogIMU_Handler.removeCallbacks(this);
                } else {
                    LogIMU_Handler.postDelayed(this,50);
                    IMU_num++;
                    Log.d(TAG, String.valueOf(IMU_num));
                    RequestBody IMU_Body = new FormBody.Builder()
                            .add("Flag","IMU")
                            .add("Timestamp", String.valueOf(SystemClock.elapsedRealtimeNanos()))
                            .add("num",String.valueOf(IMU_num))
                            .add("Accx", String.valueOf(LastAccReading[0]))
                            .add("Accy", String.valueOf(LastAccReading[1]))
                            .add("Accz", String.valueOf(LastAccReading[2]))
                            .add("Gyrox", String.valueOf(LastGyroReading[0]))
                            .add("Gyroy", String.valueOf(LastGyroReading[1]))
                            .add("Gyroz", String.valueOf(LastGyroReading[2]))
                            .add("Magx", String.valueOf(LastMagReading[0]))
                            .add("Magy",String.valueOf(LastMagReading[1]))
                            .add("Magz",String.valueOf(LastMagReading[2]))
                            .add("Azimuth",String.valueOf(orientationAngles[0]))
                            .add("Pitch",String.valueOf(orientationAngles[1]))
                            .add("Roll",String.valueOf(orientationAngles[2]))
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
                            //Log.i("result",String.valueOf(response.body()));
                        }
                    });
                }
            }
        };
        //wait x ms (only once) before running
        LogIMU_Handler.postDelayed(LogIMU_Runnable,1000);
        LogRTT_Handler.postDelayed(LogRTT_Runnable,1000);
    }

     */

    /*
    public void onClickStopLog(View view){
        Log.d(TAG,"onClickStopLog");
        Running = false;
        //TODO Stopping Logging
    }
     */

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        final float alpha = 0.97f;
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                //Log.d(TAG,"Acc: "+sensorEvent.timestamp);
                LastAccReading[0] = sensorEvent.values[0];
                LastAccReading[1] = sensorEvent.values[1];
                LastAccReading[2] = sensorEvent.values[2];
                AccForOrientation[0] = alpha * LastAccReading[0] + (1-alpha) * sensorEvent.values[0];
                AccForOrientation[1] = alpha * LastAccReading[1] + (1-alpha) * sensorEvent.values[1];
                AccForOrientation[2] = alpha * LastAccReading[2] + (1-alpha) * sensorEvent.values[2];
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                //Log.d(TAG,"Mag: "+sensorEvent.timestamp);
                LastMagReading[0] = sensorEvent.values[0];
                LastMagReading[1] = sensorEvent.values[1];
                LastMagReading[2] = sensorEvent.values[2];
                MagForOrientation[0] = alpha * LastMagReading[0] + (1-alpha) * sensorEvent.values[0];
                MagForOrientation[1] = alpha * LastMagReading[1] + (1-alpha) * sensorEvent.values[1];
                MagForOrientation[2] = alpha * LastMagReading[2] + (1-alpha) * sensorEvent.values[2];
                break;

            case Sensor.TYPE_GYROSCOPE:
                //Log.d(TAG,"Gyro: "+sensorEvent.timestamp);
                LastGyroReading[0] = sensorEvent.values[0];
                LastGyroReading[1] = sensorEvent.values[1];
                LastGyroReading[2] = sensorEvent.values[2];
        }

        // Rotation matrix based on current readings from accelerometer and magnetometer.
        SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix,
                AccForOrientation, MagForOrientation);

        // Express the updated rotation matrix as three orientation angles.
        SensorManager.getOrientation(rotationMatrix, orientationAngles);

        //Log.i("OrientationTestActivity",String.format("Orientation: %f, %f, %f", orientationAngles[0],orientationAngles[1],orientationAngles[2]));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        switch (i) {
            case -1:
                Log.d(TAG, "No Contact");
                break;
            case 0:
                Log.d(TAG, "Unreliable");
                break;
            case 1:
                Log.d(TAG, "Low Accuracy");
                break;
            case 2:
                Log.d(TAG, "Medium Accuracy");
                break;
            case 3:
                Log.d(TAG, "High Accuracy");
        }
    }

    private class WifiScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            for (ScanResult scanResult:myWifiManager.getScanResults()){
                if (scanResult.is80211mcResponder() && !APs_MacAddress.contains(scanResult.BSSID)){
                    APs_MacAddress.add(scanResult.BSSID);
                    RTT_APs.add(scanResult);
                }
            }
            //Log.d(TAG,"APs_MacAddress"+"("+APs_MacAddress.size()+")"+": "+APs_MacAddress);
            Log.d(TAG, "RTT_APs"+"("+RTT_APs.size()+")"+": "+RTT_APs);
        }
    }

    private class RTTRangingResultCallback extends RangingResultCallback {
        private void queueNextRangingRequest(){
            RangingRequestDelayHandler.postDelayed(
                    RangingActivity.this::startRangingRequest,RangingDelay);
        }

        @Override
        public void onRangingFailure(int i) {
            Log.d(TAG,"Ranging failed！");
            if (Running) {
                queueNextRangingRequest();
            }
        }


        @SuppressLint("WrongConstant")
        @Override
        public void onRangingResults(@NonNull List<RangingResult> list) {
            //Log.d(TAG, list.toString());
            List<RangingResult> status0_list = new ArrayList<>();
            for (RangingResult r:list){
                if (r.getStatus() == 0){
                    status0_list.add(r);
                }
            }
            temp = status0_list;

            if (Running){
                if (!status0_list.isEmpty()){
                    rangingActivityAdapter.swapData(status0_list);
                }
                queueNextRangingRequest();
            }
        }
    }

    //TODO onResume()

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop RangingActivity");
        super.onStop();
        unregisterSensors();
        unregisterReceiver(myWifiReceiver);
        Running = false;
    }
}