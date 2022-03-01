package com.example.rtttest1;

import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class LocalizationActivity extends AppCompatActivity {

    private static final String TAG = "LocalizationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localization);

        Set_AP_Pins();
    }

    public void Set_AP_Pins(){

    }

    public void Update_Location_Pin(double x, double y){

    }

}
