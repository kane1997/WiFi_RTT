package com.example.rtttest1;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
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
import java.util.Arrays;
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

    //TODO publuc WifiManager/WifiRTTManager/RTTRangingResultCallback for all activities?

    /**
     * For RTT service
     */
    private WifiRttManager myWifiRTTManager;
    private WifiManager myWifiManager;
    private RTTRangingResultCallback myRTTRangingResultCallback;
    private WifiScanReceiver myWifiScanReceiver;

    private List<ScanResult> RTT_APs = new ArrayList<>();
    private final List<RangingResult> Ranging_Results = new ArrayList<>();
    private final List<String> APs_MacAddress = new ArrayList<>();

    final Handler RangingRequestDelayHandler = new Handler();

    /**
     * For IMU service
     */
    private SensorManager sensorManager;

    private final HashMap<String, Sensor> sensors = new HashMap<>();

    private long IMU_timestamp;

    private final float[] rotationMatrix = new float[9];
    private final float[] inclinationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private final float[] LastAccReading = new float[3];
    private final float[] LastMagReading = new float[3];
    private final float[] LastGyroReading = new float[3];

    /**
     * For Localization service
     */
    private Paint paint;
    private Path path;
    private Bitmap temp_bitmap;
    private Canvas temp_canvas;
    
    private ImageView floor_plan, location_pin, AP1_ImageView,
            AP2_ImageView, AP3_ImageView, AP4_ImageView, AP5_ImageView, AP6_ImageView;

    //int[] floor_plan_location = new int[2];
    //int[] AP_location = new int[2];
    //int[] pin_location = new int[2];
    double meter2pixel = 32.5; // 1 meter <--> 32.5 pixels for THIS PARTICULAR FLOOR PLAN!
    double bitmap2floorplan = 2.994;
    double screen_offsetX = 241; //in pixels
    //int testing_i, testing_j, path_y;

    private String RTT_response;
    private String[] Calculated_coordinates = new String[2];

    private final AccessPoints AP1 = new AccessPoints("b0:e4:d5:39:26:89",40.91,13.15);
    private final AccessPoints AP2 = new AccessPoints("cc:f4:11:8b:29:4d",34.86,11.45);
    private final AccessPoints AP3 = new AccessPoints("b0:e4:d5:01:26:f5",48.12,11.45);
    private final AccessPoints AP4 = new AccessPoints("b0:e4:d5:5f:f2:ad",28.92,12.91);
    private final AccessPoints AP5 = new AccessPoints("b0:e4:d5:96:3b:95",22.04,13.80);
    private final AccessPoints AP6 = new AccessPoints("b0:e4:d5:91:ba:5d",18.94,11.45);

    //flag for leaving the activity
    private Boolean Running = false;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate() LocalizationActivity");
        Objects.requireNonNull(getSupportActionBar()).hide();

        //receive RTT_APs from main activity
        Intent intent = getIntent();
        RTT_APs = intent.getParcelableArrayListExtra("SCAN_RESULT");

        //TODO edit Toast
        if (RTT_APs == null || RTT_APs.isEmpty()) {
            Log.d(TAG, "RTT_APs null");
            Toast.makeText(getApplicationContext(),
                    "Please scan for available APs first",
                    Toast.LENGTH_SHORT).show();
            finish();

        } else {
            setContentView(R.layout.activity_localization);

            //RTT Initiation
            myWifiRTTManager = (WifiRttManager) getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
            myWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            myRTTRangingResultCallback = new RTTRangingResultCallback();

            WifiScanReceiver myWifiScanReceiver = new WifiScanReceiver();
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

            //Localization Initiation
            floor_plan = findViewById(R.id.imageViewFloorplan);
            location_pin = findViewById(R.id.imageViewLocationPin);
            AP1_ImageView = findViewById(R.id.imageViewAP1);
            AP2_ImageView = findViewById(R.id.imageViewAP2);
            AP3_ImageView = findViewById(R.id.imageViewAP3);
            AP4_ImageView = findViewById(R.id.imageViewAP4);
            AP5_ImageView = findViewById(R.id.imageViewAP5);
            AP6_ImageView = findViewById(R.id.imageViewAP6);

            Bitmap bitmap_floor_plan = BitmapFactory.decodeResource(getResources(),
                    R.drawable.floor_plan);
            temp_bitmap = Bitmap.createBitmap(bitmap_floor_plan.getWidth(),
                    bitmap_floor_plan.getHeight(),Bitmap.Config.RGB_565);

            temp_canvas = new Canvas(temp_bitmap);
            temp_canvas.drawBitmap(bitmap_floor_plan,0,0,null);

            paint = new Paint();
            path = new Path();

            paint.setAntiAlias(true);
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(10);
            paint.setPathEffect(new DashPathEffect(new float[] {20,10,10,10},1));

            //Start Localization
            setup_pin_location();
            registerSensors();
            startRangingRequest();
            startLoggingData();
            ScanInBackground();
            update_location_pin();
        }
    }

    /*
      The following method is used to determine the dimension of floor plan
      in aid of constructing an coordinate plane.
     */
    /*
      public void onWindowFocusChanged(boolean hasFocus) {
          super.onWindowFocusChanged(hasFocus);
          if (hasFocus) {
              //left top coordinate
              floor_plan.getLocationOnScreen(floor_plan_location);
              location_pin.getLocationOnScreen(pin_location);
              AP6_ImageView.getLocationOnScreen(AP_location);

              //floor_plan.getLayoutParams();
              Log.i(TAG, "Floorplan" + floor_plan_location[0] + ", " + floor_plan_location[1]);
              Log.i(TAG, "Pin" + pin_location[0] + ", " + pin_location[1]);
              Log.i(TAG, "AP6" + AP_location[0] + ", " + AP_location[1]);
              Log.i(TAG, "Image Width: " + floor_plan.getWidth());
              Log.i(TAG, "Image Height: " + floor_plan.getHeight());
          }
      }
     */

    /** To calculate coordinates
     * top left corner of the screen (55,145), top left corner of the floor plan (241,145)
     * SetY(-26) > left edge of the floor plan
     * width of floor plan (597), height of floor plan (2151)
     * width of bitmap (1788), height of bitmap (6438)
     *
     * FOR PIN LOCATION:
     * setX = y*<meter2pixel>(32.533)+<screen_offsetX>(241), setY = x*<meter2pixel>(32.533)
     *
     * FOR PATH EFFECT:
     * path.moveTo/lineTo( (y*32.533*bitmap2floorplan / Pin_y*bitmap2floorplan), ((x*32.533+26)*bitmap2floorplan) )
     */

    private void setup_pin_location(){
        AP1_ImageView.setX((float) (AP1.getY()*meter2pixel+screen_offsetX));
        AP1_ImageView.setY((float) (AP1.getX()*meter2pixel));
        AP2_ImageView.setX((float) (AP2.getY()*meter2pixel+screen_offsetX));
        AP2_ImageView.setY((float) (AP2.getX()*meter2pixel));
        AP3_ImageView.setX((float) (AP3.getY()*meter2pixel+screen_offsetX));
        AP3_ImageView.setY((float) (AP3.getX()*meter2pixel));
        AP4_ImageView.setX((float) (AP4.getY()*meter2pixel+screen_offsetX));
        AP4_ImageView.setY((float) (AP4.getX()*meter2pixel));
        AP5_ImageView.setX((float) (AP5.getY()*meter2pixel+screen_offsetX));
        AP5_ImageView.setY((float) (AP5.getX()*meter2pixel));
        AP6_ImageView.setX((float) (AP6.getY()*meter2pixel+screen_offsetX));
        AP6_ImageView.setY((float) (AP6.getX()*meter2pixel));

        //my desk
        location_pin.setX((float) (392+screen_offsetX));
        location_pin.setY(570);
    }

    //TODO animated drawable?
    private void update_location_pin(){
        //TODO better coordinate system?

        /*
        testing_i = 1500;
        testing_j = 570;
        path_y = (int) ((570+26)*bitmap2floorplan);

        location_pin.getLocationOnScreen(pin_location);

        Handler Update_location_Handler = new Handler();
        Runnable Update_location_Runnable = new Runnable() {
            @Override
            public void run() {
                if (Running && (pin_location[1] < testing_i)){
                    Update_location_Handler.postDelayed(this,1000);

                    path.moveTo(1174, path_y);
                    testing_j += 20;
                    path_y += 59.88;

                    location_pin.setY(testing_j);
                    location_pin.getLocationOnScreen(pin_location);

                    Log.d(TAG,"Current location: "+pin_location[0]+", "+pin_location[1]);
                    path.lineTo(1174, path_y);
                    temp_canvas.drawPath(path,paint);
                    floor_plan.setImageBitmap(temp_bitmap);
                } else {
                    Update_location_Handler.removeCallbacks(this);
                }
            }
        };
        Update_location_Handler.postDelayed(Update_location_Runnable,1000);
         */

        Handler Update_Location_Handler = new Handler();
        Runnable Update_Location_Runnable = new Runnable() {
            @Override
            public void run() {
                if (Running) {
                    Update_Location_Handler.postDelayed(this,1000);
                    Log.d(TAG, Arrays.toString(Calculated_coordinates));

                    if (Calculated_coordinates[0] != null && Calculated_coordinates[1] != null) {
                        location_pin.setX((float) (Float.parseFloat
                                (Calculated_coordinates[1]) *meter2pixel+screen_offsetX));
                        location_pin.setY((float) (Float.parseFloat
                                (Calculated_coordinates[0])*meter2pixel));
                    }
                } else {
                    Update_Location_Handler.removeCallbacks(this);
                }
            }
        };
        Update_Location_Handler.postDelayed(Update_Location_Runnable,1300);

    }

    @SuppressLint("MissingPermission")
    private void startRangingRequest() {
        RangingRequest rangingRequest =
                new RangingRequest.Builder().addAccessPoints(RTT_APs).build();

        myWifiRTTManager.startRanging(
                rangingRequest, getApplication().getMainExecutor(), myRTTRangingResultCallback);
    }

    private void startLoggingData(){
        String url = "http://192.168.86.52:5000/server";
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
                                throws IOException {
                            RTT_response = Objects.requireNonNull(response.body()).string();
                            response.close();
                            Calculated_coordinates = RTT_response.split(" ");
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
                                throws IOException{
                            //String IMU_response = Objects.requireNonNull(response.body()).string();
                            response.close();
                            //Log.i("result",IMU_response);
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

    private void ScanInBackground(){
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

    private void unregisterSensors(){
        for (Sensor eachSensor:sensors.values()){
            sensorManager.unregisterListener(this,eachSensor);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        final float alpha = 0.97f;
        IMU_timestamp = SystemClock.elapsedRealtime();

        switch (sensorEvent.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                //Log.d(TAG,"TYPE_ACCELEROMETER: "+sensorEvent.timestamp);
                //System.arraycopy(sensorEvent.values,0,LastAccReading,0,sensorEvent.values.length);
                LastAccReading[0] = alpha * LastAccReading[0] + (1-alpha) * sensorEvent.values[0];
                LastAccReading[1] = alpha * LastAccReading[1] + (1-alpha) * sensorEvent.values[1];
                LastAccReading[2] = alpha * LastAccReading[2] + (1-alpha) * sensorEvent.values[2];
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                //Log.d(TAG,"TYPE_MAGNETIC_FIELD: "+sensorEvent.timestamp);
                //System.arraycopy(sensorEvent.values,0,LastMagReading,0,sensorEvent.values.length);
                LastMagReading[0] = alpha * LastMagReading[0] + (1-alpha) * sensorEvent.values[0];
                LastMagReading[1] = alpha * LastMagReading[1] + (1-alpha) * sensorEvent.values[1];
                LastMagReading[2] = alpha * LastMagReading[2] + (1-alpha) * sensorEvent.values[2];
                break;

            case Sensor.TYPE_GYROSCOPE:
                //Log.d(TAG,"TYPE_GYROSCOPE: "+sensorEvent.timestamp);
                LastGyroReading[0] = sensorEvent.values[0];
                LastGyroReading[1] = sensorEvent.values[1];
                LastGyroReading[2] = sensorEvent.values[2];
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
        @Override
        public void onReceive(Context context, Intent intent) {
            for (ScanResult scanResult:myWifiManager.getScanResults()){
                if (scanResult.is80211mcResponder()) {
                    if (!APs_MacAddress.contains(scanResult.BSSID)) {
                        RTT_APs.add(scanResult);
                        Log.d(TAG,"APs_MacAddress: "+APs_MacAddress);
                        Log.d(TAG, "RTT_APs: "+RTT_APs);
                        //TODO Handler getmaxpeer
                    }
                }
            }
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
                queueNextRangingRequest();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() LocalizationActivity");
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop() LocalizationActivity");
        super.onStop();
        unregisterSensors();

        Running = false;
    }

    protected void onResume() {
        Log.d(TAG,"onResume() LocalizationActivity");
        super.onResume();
        registerSensors();
        Running = true;
    }
}
