package com.example.rtttest1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class LocalizationActivity_test extends AppCompatActivity {

    private static final String TAG = "LocalizationActivity";

    //For communication with service
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localization);

        //Start localization service in background
        Intent Localization_service = new Intent(this,
                LocalizationRangingService_test.class);
        startService(Localization_service);
        Log.d(TAG,"Start localization service");

        Set_AP_Pins();
    }

    public void Set_AP_Pins(){

    }

    public void Update_Location_Pin(double x, double y){

    }

    @Override
    protected void onResume() {
        super.onResume();
        //registerReceiver(receiver)
        //TODO
    }

    @Override
    protected void onPause() {
        super.onPause();
        //TODO
    }

}
