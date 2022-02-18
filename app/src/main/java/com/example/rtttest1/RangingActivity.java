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
public class RangingActivity extends AppCompatActivity {
    private static final String TAG = "RangingActivity";

    //RTT
    private WifiRttManager myWifiRTTManager;
    private RTTRangingResultCallback myRTTResultCallback;
    private RangingActivityAdapter rangingActivityAdapter;
    private WifiManager myWifiManager;
    private WifiScanReceiver myWifiReceiver;

    //IMU
    private SensorManager sensorManager;

    //flag for leaving the activity
    Boolean Running = true;

    List<ScanResult> RTT_APs = new ArrayList<>();
    List<RangingResult> Ranging_Result = new ArrayList<>();

    private EditText RangingDelayEditText;
    private static final int RangingDelayDefault = 100;
    private int RangingDelay;

    private TextView textViewAccx;
    private TextView textViewAccy;
    private TextView textViewAccz;
    private TextView textViewGrox;
    private TextView textViewGroy;
    private TextView textViewGroz;
    private TextView textViewMagx;
    private TextView textViewMagy;
    private TextView textViewMagz;

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
            myWifiReceiver = new WifiScanReceiver();

            registerReceiver(myWifiReceiver,
                    new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

            rangingActivityAdapter = new RangingActivityAdapter(Ranging_Result);
            myRecyclerView.setAdapter(rangingActivityAdapter);

            //IMU
            textViewAccx = findViewById(R.id.textViewAccX);
            textViewAccy = findViewById(R.id.textViewAccY);
            textViewAccz = findViewById(R.id.textViewAccZ);
            textViewGrox = findViewById(R.id.textViewGroX);
            textViewGroy = findViewById(R.id.textViewGroY);
            textViewGroz = findViewById(R.id.textViewGroZ);
            textViewMagx = findViewById(R.id.textViewMagX);
            textViewMagy = findViewById(R.id.textViewMagY);
            textViewMagz = findViewById(R.id.textViewMagZ);

            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

            Sensor sensor_acc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(
                    listener_acc,sensor_acc,SensorManager.SENSOR_DELAY_GAME);

            Sensor sensor_gro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            sensorManager.registerListener(
                    listener_gro,sensor_gro,SensorManager.SENSOR_DELAY_GAME);

            Sensor sensor_mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            sensorManager.registerListener(
                    listener_mag,sensor_mag,SensorManager.SENSOR_DELAY_GAME);

            startRangingRequest();
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

    @Override
    protected void onStop() {
        Log.d(TAG, "Stopping ranging activity...");
        super.onStop();
        if (sensorManager != null){
            sensorManager.unregisterListener(listener_acc);
            sensorManager.unregisterListener(listener_gro);
            sensorManager.unregisterListener(listener_mag);
        }
        unregisterReceiver(myWifiReceiver);
        Running = false;
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
                    Update_Handler.postDelayed(this,3000);

                    myWifiManager.startScan();
                }
            }
        };
        Update_Handler.postDelayed(Update_Runnable,1000);
    }

    public void onClickLogData(View view){
        Log.d(TAG,"onClickLogData()");

        //IP address of Nest Router
        String url = "http://192.168.86.31:5000/server";

        OkHttpClient client = new OkHttpClient.Builder().build();
        //OkHttpClient client = new OkHttpClient();
        //OkHttpClient client = new OkHttpClient().newBuilder().build();

        Handler LogData_Handler = new Handler();
        Runnable LogData_Runnable = new Runnable() {
            @Override
            public void run() {
                if (!Running){
                    LogData_Handler.removeCallbacks(this);
                } else{

                    //rate of packet sending(optimal is 200)
                    LogData_Handler.postDelayed(this,200);

                    List<String> RangingInfo = new ArrayList<>();
                    Log.d(TAG, "Packet:"+ Ranging_Result);

                    for (RangingResult rangingResult: Ranging_Result){
                        RangingInfo.add(String.valueOf(rangingResult.getMacAddress()));
                        RangingInfo.add(String.valueOf(rangingResult.getDistanceMm()));
                        RangingInfo.add(String.valueOf(rangingResult.getDistanceStdDevMm()));
                        RangingInfo.add(String.valueOf(rangingResult.getRssi()));
                    }

                    //Log.d(TAG, String.valueOf(System.currentTimeMillis()));

                    RequestBody body = new FormBody.Builder()

                            .add("Timestamp", String.valueOf(System.currentTimeMillis()))
                            .add("RTT_Result", String.valueOf(RangingInfo))
                            .add("IMU_Result", " Accx "+ textViewAccx.getText()
                                    +" Accy "+ textViewAccy.getText()
                                    +" Accz "+ textViewAccz.getText()
                                    +" Grox "+ textViewGrox.getText()
                                    +" Groy "+ textViewGroy.getText()
                                    +" Groz "+ textViewGroz.getText()
                                    +" Magx "+ textViewMagx.getText()
                                    +" Magy "+ textViewMagy.getText()
                                    +" Magz "+ textViewMagz.getText())
                            .build();

                    Request request = new Request.Builder()
                            .url(url)
                            .post(body)
                            .build();

                    final Call call = client.newCall(request);

                    call.enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            Log.i("onFailure",e.getMessage());
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            assert response.body() != null;
                            String result = response.body().string();
                            response.close();
                            Log.i("result",result);
                        }
                    });
                }
            }
        };
        //wait x ms (only once) before running
        LogData_Handler.postDelayed(LogData_Runnable,1000);
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
                    "AP list updated",Snackbar.LENGTH_LONG).show();
            List<ScanResult> scanResults = myWifiManager.getScanResults();
            RTT_APs = findRTTAPs(scanResults);
            Log.d(TAG,"Received and updated AP list(" + RTT_APs.size() + "): " + RTT_APs);
        }
    }

    private class RTTRangingResultCallback extends RangingResultCallback {

        private void queueNextRangingRequest(){
            RangingRequestDelayHandler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            startRangingRequest();
                        }
                    },RangingDelay);


                    //RangingActivity.this::startRangingRequest,
                    //RangingDelay);
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

            /*
            for (RangingResult r:list){
                status += r.getStatus();
            }
            
            Log.d(TAG,"Status: "+ status);
             */

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

    private final SensorEventListener listener_acc = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            textViewAccx.setText(
                    String.format(Locale.getDefault(),"%.2f",sensorEvent.values[0]));
            textViewAccy.setText(
                    String.format(Locale.getDefault(),"%.2f",sensorEvent.values[1]));
            textViewAccz.setText(
                    String.format(Locale.getDefault(),"%.2f",sensorEvent.values[2]));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {}
    };

    private final SensorEventListener listener_gro = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            textViewGrox.setText(
                    String.format(Locale.getDefault(),"%.2f",sensorEvent.values[0]));
            textViewGroy.setText(
                    String.format(Locale.getDefault(),"%.2f",sensorEvent.values[1]));
            textViewGroz.setText(
                    String.format(Locale.getDefault(),"%.2f",sensorEvent.values[2]));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {}
    };

    private final SensorEventListener listener_mag = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            textViewMagx.setText(
                    String.format(Locale.getDefault(),"%.2f",sensorEvent.values[0]));
            textViewMagy.setText(
                    String.format(Locale.getDefault(),"%.2f",sensorEvent.values[1]));
            textViewMagz.setText(
                    String.format(Locale.getDefault(),"%.2f",sensorEvent.values[2]));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };
}