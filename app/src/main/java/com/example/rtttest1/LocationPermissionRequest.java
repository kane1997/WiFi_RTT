package com.example.rtttest1;

import android.Manifest;
import android.Manifest.permission;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.view.View;

public class LocationPermissionRequest extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    //Debug logs in logcat
    private static final String TAG = "LocationPermissionRequest";

    // ID to identify location permission request
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        // if permission is granted already, no need to ask again
        if (ActivityCompat.checkSelfPermission(
                this,
                permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            finish();
        }

        //otherwise go to request screen
        setContentView(R.layout.permission_request);
    }

    public void onClickPermissionGranted(View view){
        Log.d(TAG, "onClickPermissionGranted()");

        // Present UI to request location
        ActivityCompat.requestPermissions(
                this,
                new String[] {permission.ACCESS_FINE_LOCATION},
                PERMISSION_REQUEST_FINE_LOCATION);
    }

    public void onClickPermissionDenied(View view){
        Log.d(TAG,"onClickPermissionDenied()");
        finish();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permission, @NonNull int[] grantResult) {
        Log.d(TAG, "onRequestPermissionResult");
        super.onRequestPermissionsResult(requestCode, permission, grantResult);

        String PermissionResult = "Request code: " + requestCode + ", permission: "
                + permission + ", result: " + grantResult;

        Log.d(TAG, "onRequestPermissionResult()" + PermissionResult);

        if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {
            finish();
        }

    }
}
