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
import android.view.View;
import android.widget.EditText;
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

    private WifiRttManager myWifiRTTManager;
    private RTTRangingResultCallback myRTTResultCallback;
    private static final int RangingDelayDefault = 100;

    //flag for leaving the activity
    Boolean Running = true;

    List<RangingResult> list = new ArrayList<>();

    private EditText RangingDelayEditText;

    private RangingActivityAdapter rangingActivityAdapter;

    private int RangingDelay;
    private int status = 0;

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

            RecyclerView myRecyclerView = findViewById(R.id.recyclerViewResults);
            myRecyclerView.setHasFixedSize(true);

            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
            myRecyclerView.setLayoutManager((layoutManager));

            rangingActivityAdapter = new RangingActivityAdapter(list);
            myRecyclerView.setAdapter(rangingActivityAdapter);

            RangingDelayEditText = findViewById(R.id.delayValue);
            RangingDelayEditText.setText(String.format(
                    Locale.getDefault(),"%d", RangingDelayDefault));

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

        String delay = RangingDelayEditText.getText().toString();
        if (!delay.equals("")){
            RangingDelay = Integer.parseInt(RangingDelayEditText.getText().toString());
        }else{
            Snackbar.make(findViewById(R.id.textViewDelayBeforeNextRequest),
                    "Please enter a valid number",Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Stopping ranging activity...");
        super.onStop();
        Running = false;
    }

    public void onClickLogRTT(View view){
        Log.d(TAG,"onClickLogRTT()");

        Handler LogData_Handler = new Handler();
        Runnable LogData_Runnable = new Runnable() {
            @Override
            public void run() {
                if (!Running){
                    LogData_Handler.removeCallbacks(this);
                } else{
                    //rate of packet sending
                    LogData_Handler.postDelayed(this,300);

                    //IP address of Nest Router
                    String url = "http://192.168.86.24:5000/server";

                    //IP address of personal hotspot (not working)
                    //String url = "http://172.20.10.2:5000/server";

                    //IP address of Yilun's laptop
                    //String url = "http://192.168.137.179:5000/server";

                    OkHttpClient client = new OkHttpClient.Builder().build();

                    RequestBody body = new FormBody.Builder()
                            .add("Result", String.valueOf(list)).build();

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
                            Log.i("result",result);
                        }
                    });
                }
            }
        };
        //wait x ms (only once) before running
        LogData_Handler.postDelayed(LogData_Runnable,1000);
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

            for (RangingResult r:list){
                status += r.getStatus();
            }

            if (Running && status == 0){
                if (!list.isEmpty()){
                    rangingActivityAdapter.swapData(list);
                }
            }
            if (Running){
                queueNextRangingRequest();
            }
        }
    }

}
