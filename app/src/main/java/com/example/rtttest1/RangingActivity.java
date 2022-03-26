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

//TODO put common class in service?
//TODO Try different layout view(linear?)
/**
 * Send ranging requests and display distance and RSSI values
 */
public class RangingActivity extends AppCompatActivity implements SensorEventListener{
    private static final String TAG = "RangingActivity";

    //RTT
    private WifiRttManager myWifiRTTManager;
    private RTTRangingResultCallback myRTTResultCallback;
    private WifiManager myWifiManager;
    private WifiScanReceiver myWifiScanReceiver;
    private RangingActivityAdapter rangingActivityAdapter;

    List<ScanResult> RTT_APs = new ArrayList<>();
    List<RangingResult> Ranging_Results = new ArrayList<>();
    private final List<String> APs_MacAddress = new ArrayList<>();

    final Handler RangingRequestDelayHandler = new Handler();

    //IMU
    private SensorManager sensorManager;

    private final HashMap<String,Sensor> sensors = new HashMap<>();

    public long IMU_timestamp;

    private final float[] rotationMatrix = new float[9];
    private final float[] inclinationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private final float[] LastAccReading = new float[3];
    private final float[] LastMagReading = new float[3];
    private final float[] LastGyroReading = new float[3];

    int Acc_Flag, Mag_Flag, Gyro_Flag = 0;

    long Acc_reference_time, Mag_reference_time, Gyro_reference_time,
            Acc_current_time, Mag_current_time, Gyro_current_time;

    private Boolean Running = true;

    //Ranging layout

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate() RangingActivity");

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

            myWifiScanReceiver = new WifiScanReceiver();
            registerReceiver(myWifiScanReceiver,
                    new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

            for (ScanResult AP:RTT_APs){
                APs_MacAddress.add(AP.BSSID);
            }

            rangingActivityAdapter = new RangingActivityAdapter(Ranging_Results);
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

            sensors.put("Magnetic",sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
            sensors.put("Accelerometer",sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
            sensors.put("Gyroscope",sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));

            //Start
            registerSensors();
            startRangingRequest();
        }
    }

    private void registerSensors(){
        for (Sensor eachSensor:sensors.values()){
            sensorManager.registerListener(this,
                    eachSensor,SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    private void unregisterSensors(){
        for (Sensor eachSensor:sensors.values()){
            sensorManager.unregisterListener(this,eachSensor);
        }
    }

    @SuppressLint("MissingPermission")
    private void startRangingRequest() {
        RangingRequest rangingRequest =
                new RangingRequest.Builder().addAccessPoints(RTT_APs).build();

        myWifiRTTManager.startRanging(
                rangingRequest, getApplication().getMainExecutor(), myRTTResultCallback);

        String delay = RangingDelayEditText.getText().toString();
        if (!delay.equals("")){
            RangingDelay = Integer.parseInt(RangingDelayEditText.getText().toString());
        }else{
            Snackbar.make(findViewById(R.id.textViewDelayBeforeNextRequest),
                    "Please enter a valid number",Snackbar.LENGTH_SHORT).show();
            //TODO edit snackbar
        }
    }

    public void onClickBackgroundScan(View view){
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
        Snackbar.make(view,"Start sending data",Snackbar.LENGTH_SHORT).show();

        EditText url_text = findViewById(R.id.editTextServer);
        String url_bit = url_text.getText().toString();

        //IP address of Nest Router
        //String url = "http://192.168.86.52:5000/server";
        String url = "http://192.168.86." + url_bit + ":5000/server";
        //TODO editText

        final OkHttpClient client = new OkHttpClient();

        //TODO will AsyncTask/Thread/Queue work better?

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
                    //rate of RTT packet sending(optimal is 200)
                    LogRTT_Handler.postDelayed(this,200);

                    List<String> RangingInfo = new ArrayList<>();
                    for (RangingResult rangingResult: Ranging_Results){
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

        Thread IMU_thread = new Thread(() -> {
            while (Running) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                RequestBody IMU_Body = new FormBody.Builder()
                        .add("Flag","IMU")
                        .add("Timestamp", String.valueOf(IMU_timestamp))
                        .add("Accx", String.valueOf(LastAccReading[0]))
                        .add("Accy", String.valueOf(LastAccReading[1]))
                        .add("Accz", String.valueOf(LastAccReading[2]))
                        .add("Gyrox", String.valueOf(LastGyroReading[0]))
                        .add("Gyroy", String.valueOf(LastGyroReading[1]))
                        .add("Gyroz", String.valueOf(LastGyroReading[2]))
                        .add("Magx", String.valueOf(LastMagReading[0]))
                        .add("Magy", String.valueOf(LastMagReading[1]))
                        .add("Magz", String.valueOf(LastMagReading[2]))
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
                    public void onResponse(@NonNull Call call, @NonNull Response response)
                            throws IOException {
                        String result = Objects.requireNonNull(response.body()).string();
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
        final float alpha = 0.97f;
        IMU_timestamp = SystemClock.elapsedRealtimeNanos();

        switch (sensorEvent.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                if (Acc_Flag == 0) {
                    Acc_Flag += 1;
                    //Acc_reference_time = sensorEvent.timestamp;
                    Acc_reference_time = SystemClock.elapsedRealtimeNanos();
                }

                //Acc_current_time = sensorEvent.timestamp - Acc_reference_time;
                Acc_current_time = SystemClock.elapsedRealtimeNanos() - Acc_reference_time;
                //Log.d(TAG,"ACC: "+Acc_current_time);
                LastAccReading[0] = alpha * LastAccReading[0] + (1-alpha) * sensorEvent.values[0];
                LastAccReading[1] = alpha * LastAccReading[1] + (1-alpha) * sensorEvent.values[1];
                LastAccReading[2] = alpha * LastAccReading[2] + (1-alpha) * sensorEvent.values[2];
                /*
                String AccX = this.getString(R.string.AccelerometerX,LastAccReading[0]);
                String AccY = this.getString(R.string.AccelerometerY,LastAccReading[1]);
                String AccZ = this.getString(R.string.AccelerometerZ,LastAccReading[2]);
                textAccx.setText(AccX);
                textAccy.setText(AccY);
                textAccz.setText(AccZ);
                 */
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                if (Mag_Flag == 0) {
                    Mag_Flag += 1;
                    //Mag_reference_time = sensorEvent.timestamp;
                    Mag_reference_time = SystemClock.elapsedRealtimeNanos();
                }
                //Mag_current_time = sensorEvent.timestamp - Mag_reference_time;
                Mag_current_time = SystemClock.elapsedRealtimeNanos() - Mag_reference_time;
                //Log.d(TAG, "MAG: "+ Mag_current_time);
                LastMagReading[0] = alpha * LastMagReading[0] + (1-alpha) * sensorEvent.values[0];
                LastMagReading[1] = alpha * LastMagReading[1] + (1-alpha) * sensorEvent.values[1];
                LastMagReading[2] = alpha * LastMagReading[2] + (1-alpha) * sensorEvent.values[2];
                /*
                String MagX = this.getString(R.string.Magnetic_FieldX,LastMagReading[0]);
                String MagY = this.getString(R.string.Magnetic_FieldY,LastMagReading[1]);
                String MagZ = this.getString(R.string.Magnetic_FieldZ,LastMagReading[2]);
                textMagx.setText(MagX);
                textMagy.setText(MagY);
                textMagz.setText(MagZ);
                 */
                break;

            case Sensor.TYPE_GYROSCOPE:
                if (Gyro_Flag == 0) {
                    Gyro_Flag += 1;
                    //Gyro_reference_time = sensorEvent.timestamp;
                    Gyro_reference_time = SystemClock.elapsedRealtimeNanos();
                }
                //Gyro_current_time = sensorEvent.timestamp - Gyro_reference_time;
                Gyro_current_time = SystemClock.elapsedRealtimeNanos() - Gyro_reference_time;
                //Log.d(TAG,"GYRO: "+ Gyro_current_time);
                LastGyroReading[0] = sensorEvent.values[0];
                LastGyroReading[1] = sensorEvent.values[1];
                LastGyroReading[2] = sensorEvent.values[2];
                /*
                String GyroX = this.getString(R.string.GyroscopeX,LastGyroReading[0]);
                String GyroY = this.getString(R.string.GyroscopeY,LastGyroReading[1]);
                String GyroZ = this.getString(R.string.GyroscopeZ,LastGyroReading[2]);
                textGrox.setText(GyroX);
                textGroy.setText(GyroY);
                textGroz.setText(GyroZ);
                 */
        }

        // Rotation matrix based on current readings from accelerometer and magnetometer.
        SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix,
                LastAccReading, LastMagReading);
        // Express the updated rotation matrix as three orientation angles.
        SensorManager.getOrientation(rotationMatrix, orientationAngles);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        switch (i) {
            case -1:
                Log.d(TAG,"No Contact");
                break;
            case 0:
                Log.d(TAG,"Unreliable");
                break;
            case 1:
                Log.d(TAG,"Low Accuracy");
                break;
            case 2:
                Log.d(TAG,"Medium Accuracy");
                break;
            case 3:
                Log.d(TAG,"High Accuracy");
        }
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
                    "APs Updated",Snackbar.LENGTH_SHORT).show();
            List<ScanResult> scanResults = myWifiManager.getScanResults();
            RTT_APs = findRTTAPs(scanResults);
            Log.d(TAG,"Received and updated AP list(" + RTT_APs.size() + "): " + RTT_APs);
        }
        /*
        @Override
        public void onReceive(Context context, Intent intent) {
            for (ScanResult scanResult:myWifiManager.getScanResults()){
                if (scanResult.is80211mcResponder()) {
                    if (!APs_MacAddress.contains(scanResult.BSSID)) {
                        RTT_APs.add(scanResult);
                        Log.d(TAG,"APs_MacAddress: "+APs_MacAddress);
                        Log.d(TAG, "RTT_APs: "+RTT_APs);
                        //TODO Handler getmaxpeers
                    }
                }
            }
            Log.d(TAG,"New AP list(" + RTT_APs.size() + "): " + RTT_APs);
        }
         */
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
            Ranging_Results.clear();
            for (RangingResult result:list) {
                if (result.getStatus() == 0){
                    Ranging_Results.add(result);
                }
            }
            if (Running) {
                Log.d(TAG, Ranging_Results.size()+String.valueOf(Ranging_Results));
                rangingActivityAdapter.swapData(Ranging_Results);
                queueNextRangingRequest();
            }
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop() RangingActivity");
        super.onStop();
        unregisterSensors();
        unregisterReceiver(myWifiScanReceiver);
        Running = false;
    }

    protected void onResume() {
        Log.d(TAG,"onResume() RangingActivity");
        super.onResume();
        registerSensors();
        //registerReceiver(myWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        Running = true;
    }
}