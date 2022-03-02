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
public class RangingActivity extends AppCompatActivity implements SensorEventListener{
    private static final String TAG = "RangingActivity";

    //RTT
    private WifiRttManager myWifiRTTManager;
    private RTTRangingResultCallback myRTTResultCallback;
    private RangingActivityAdapter rangingActivityAdapter;
    private WifiManager myWifiManager;

    List<ScanResult> RTT_APs = new ArrayList<>();
    List<RangingResult> Ranging_Result = new ArrayList<>();
    //List<String> RangingInfo = new ArrayList<>();

    //IMU
    private SensorManager sensorManager;
    private final HashMap<String,Sensor> sensors = new HashMap<>();

    //flag for leaving the activity
    Boolean Running = true;

    private EditText RangingDelayEditText;
    private static final int RangingDelayDefault = 100;
    private int RangingDelay;

    private TextView textAccx;
    private TextView textAccy;
    private TextView textAccz;
    private TextView textGrox;
    private TextView textGroy;
    private TextView textGroz;
    private TextView textMagx;
    private TextView textMagy;
    private TextView textMagz;

    public float accx,accy,accz,gyrox,gyroy,gyroz,magx,magy,magz;
    public long IMU_timestamp;

    final Handler RangingRequestDelayHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //receive RTT_APs from main activity
        Intent intent = getIntent();
        RTT_APs = intent.getParcelableArrayListExtra("SCAN_RESULT");

        if (RTT_APs == null || RTT_APs.isEmpty()) {
            Log.d(TAG,"RTT_APs null");
            Toast.makeText(getApplicationContext(),
                    "Please scan for available APs first",
                    Toast.LENGTH_SHORT).show();
            finish();
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
            WifiScanReceiver myWifiReceiver = new WifiScanReceiver();

            registerReceiver(myWifiReceiver,
                    new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

            rangingActivityAdapter = new RangingActivityAdapter(Ranging_Result);
            myRecyclerView.setAdapter(rangingActivityAdapter);

            //IMU
            textAccx = findViewById(R.id.textViewAccX);
            textAccy = findViewById(R.id.textViewAccY);
            textAccz = findViewById(R.id.textViewAccZ);
            textGrox = findViewById(R.id.textViewGroX);
            textGroy = findViewById(R.id.textViewGroY);
            textGroz = findViewById(R.id.textViewGroZ);
            textMagx = findViewById(R.id.textViewMagX);
            textMagy = findViewById(R.id.textViewMagY);
            textMagz = findViewById(R.id.textViewMagZ);

            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

            sensors.put("Accelerometer",sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
            sensors.put("Gyroscope",sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
            sensors.put("Magnetic",sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
            registerSensors();

            startRangingRequest();
        }
    }

    public void registerSensors(){
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
        Log.d(TAG,"BackgroundScan");
        Snackbar.make(view,"Start scanning in background",Snackbar.LENGTH_SHORT).show();
        Handler Update_Handler = new Handler();
        Runnable Update_Runnable = new Runnable() {
            @Override
            public void run() {
                if (!Running){
                    Update_Handler.removeCallbacks(this);
                } else{
                    //background scan rate
                    Update_Handler.postDelayed(this,3000);

                    myWifiManager.startScan();
                }
            }
        };
        Update_Handler.postDelayed(Update_Runnable,1000);
    }

    public void onClickLogData(View view){
        Log.d(TAG,"onClickLogData()");
        Snackbar.make(view,"Start sending data",Snackbar.LENGTH_SHORT).show();

        //IP address of Nest Router
        String url = "http://192.168.86.34:5000/server";

        final OkHttpClient client = new OkHttpClient();

        /*
        Thread RTT_thread = new Thread(() -> {
            while (Running) {
                RangingInfo.clear();
                //rate of RTT packet sending(optimal is 200)
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.d(TAG,"1");

                for (RangingResult rangingResult: Ranging_Result){
                    RangingInfo.add(String.valueOf(rangingResult.getMacAddress()));
                    RangingInfo.add(String.valueOf(rangingResult.getDistanceMm()));
                    RangingInfo.add(String.valueOf(rangingResult.getDistanceStdDevMm()));
                    RangingInfo.add(String.valueOf(rangingResult.getRssi()));
                }
                Log.d(TAG,"2");

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
                        Log.i("onFailure", e.getMessage());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response)
                            throws IOException {
                        String result = response.body().string();
                        response.close();
                        Log.i("result",result);
                    }
                });
            }
        });
        RTT_thread.start();

         */

        Handler LogRTT_Handler = new Handler();
        Runnable LogRTT_Runnable = new Runnable() {
            @Override
            public void run() {
                if (!Running){
                    LogRTT_Handler.removeCallbacks(this);
                } else{
                    //RangingInfo.clear();
                    //rate of RTT packet sending(optimal is 200)
                    LogRTT_Handler.postDelayed(this,200);

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
            }
        };

        Thread IMU_thread = new Thread(() -> {
            while (Running) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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
                            throws IOException {
                        String result = response.body().string();
                        response.close();
                        Log.i("result",result);
                    }
                });
            }
        });
        IMU_thread.start();

        /*
        Handler LogIMU_Handler = new Handler();
        Runnable LogIMU_Runnable = new Runnable() {
            @Override
            public void run() {
                if (!Running){
                    LogIMU_Handler.removeCallbacks(this);
                } else {
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
            }
        };
        //wait x ms (only once) before running

        LogIMU_Handler.postDelayed(LogIMU_Runnable,1000);
         */
        LogRTT_Handler.postDelayed(LogRTT_Runnable,1000);
    }



    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        IMU_timestamp = SystemClock.elapsedRealtime();
        switch (sensorEvent.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                //Log.d(TAG, "Acc: "+sensorEvent.timestamp);
                accx = sensorEvent.values[0];
                accy = sensorEvent.values[1];
                accz = sensorEvent.values[2];
                /*
                String AccX = this.getString(R.string.AccelerometerX,accx);
                String AccY = this.getString(R.string.AccelerometerY,accy);
                String AccZ = this.getString(R.string.AccelerometerZ,accz);
                textAccx.setText(AccX);
                textAccy.setText(AccY);
                textAccz.setText(AccZ);

                 */
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                //Log.d(TAG, "Mag: "+sensorEvent.timestamp);
                magx = sensorEvent.values[0];
                magy = sensorEvent.values[1];
                magz = sensorEvent.values[2];
                /*
                String MagX = this.getString(R.string.Magnetic_FieldX,magx);
                String MagY = this.getString(R.string.Magnetic_FieldY,magy);
                String MagZ = this.getString(R.string.Magnetic_FieldZ,magz);
                textMagx.setText(MagX);
                textMagy.setText(MagY);
                textMagz.setText(MagZ);

                 */
                break;

            case Sensor.TYPE_GYROSCOPE:
                //Log.d(TAG,"Gyr: "+sensorEvent.timestamp);
                gyrox = sensorEvent.values[0];
                gyroy = sensorEvent.values[1];
                gyroz = sensorEvent.values[2];
                /*
                String GyroX = this.getString(R.string.GyroscopeX,gyrox);
                String GyroY = this.getString(R.string.GyroscopeY,gyroy);
                String GyroZ = this.getString(R.string.GyroscopeZ,gyroz);
                textGrox.setText(GyroX);
                textGroy.setText(GyroY);
                textGroz.setText(GyroZ);

                 */
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private class WifiScanReceiver extends BroadcastReceiver {

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
            Snackbar.make(findViewById(R.id.btnBackgroundScan),
                    "AP list updated",Snackbar.LENGTH_SHORT).show();
            RTT_APs = findRTTAPs(myWifiManager.getScanResults());
            Log.d(TAG,"Received and updated AP list(" + RTT_APs.size() + "): " + RTT_APs);
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
            Log.d(TAG, list.toString());

            //Only keep valid ranging results
            List<RangingResult> status0_list = new ArrayList<>();
            for (RangingResult r:list){
                if (r.getStatus() == 0){
                    status0_list.add(r);
                }
            }

            if (Running){
                if (!status0_list.isEmpty()){
                    rangingActivityAdapter.swapData(status0_list);
                }
                queueNextRangingRequest();
            }
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop() RangingActivity");
        super.onStop();
        unregisterSensors();
        //unregisterReceiver(myWifiReceiver);
        Running = false;
    }

    protected void onResume() {
        Log.d(TAG,"onResume() RangingActivity");
        super.onResume();
        registerSensors();
        startRangingRequest();
        //registerReceiver(myWifiReceiver);
        Running = true;
    }
}