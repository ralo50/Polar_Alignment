package com.ralo.luka.polaris;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.devadvance.circularseekbar.CircularSeekBar;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private ImageView image;
    private float currentDegree = 0f;
    LocationManager locationManager;
    LocationListener locationListener;
    CircularSeekBar seekbar;
    static Calendar cal3;
    static double currentLongitude = 21.47;
    static double currentLatitude = 0;
    static int currentAltitude = 0;
    int hours;
    TextView dateAndTimeTextView;
    TextView locationTextView;
    TextView clockPosition;
    String formatted;
    SharedPreferences settings;
    final String PREFS_NAME = "MyPrefsFile";


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        image=findViewById(R.id.compass);
        sensorManager=(SensorManager)getSystemService(SENSOR_SERVICE);
        locationTextView = findViewById(R.id.location);
        settings = getSharedPreferences(PREFS_NAME, 0);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        dateAndTimeTextView = findViewById(R.id.dateAndTime);
        locationTextView.setText(savedLocationString());

        isProviderEnabled();
        isFirstTime();
        getCurrentLocation();


        seekbar = findViewById(R.id.circularSeekBar1);
        cal3 = Calendar.getInstance();

        seekbarSettings();

        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                runEverySecond();
                            }
                        });
                    }
                } catch (InterruptedException ignored) {
                }
            }
        };

        t.start();

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                locationChanged(location);

            }


            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {
                Toast.makeText(MainActivity.this, "Acquiring location...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };


        checkPermissions();

    }



    public void seekbarSettings(){
        seekbar.setMax(1439);

        seekbar.setProgress(1439 - PolarisHourAngle.getJulianDate(currentLongitude));
        seekbar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

    }

    public void showSnackBar(Activity activity){
        View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        Snackbar.make(rootView, "Without GPS, polaris angle is shown for last saved location", Snackbar.LENGTH_INDEFINITE)
                .setAction("CLOSE", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                })
                .setActionTextColor(getResources().getColor(android.R.color.holo_red_dark ))
                .show();

    }

    @SuppressLint("MissingPermission")
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        }
    }

    public void onClickDone(View view) {
        if(!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))){
            Toast.makeText(this, "Turn on GPS location.", Toast.LENGTH_SHORT).show();
        }
        else{
            settings.edit().putFloat("location", (float) currentLongitude).apply();
            settings.edit().putFloat("locationLat", (float)currentLatitude).apply();
            settings.edit().putInt("locationAlt", currentAltitude).apply();
            Toast.makeText(this, "Location saved", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        seekbar.setProgress(1439 - PolarisHourAngle.getJulianDate(currentLongitude));
        sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_GAME);
        super.onResume();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float degree=Math.round(sensorEvent.values[0]);
        RotateAnimation ra=new RotateAnimation(currentDegree,-degree, Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
        ra.setDuration(120);
        ra.setFillAfter(true);
        image.startAnimation(ra);
        currentDegree=-degree;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    public String savedLocationString(){
        currentLongitude = settings.getFloat("location", 0);
        currentLatitude = settings.getFloat("locationLat", 0);
        currentAltitude = settings.getInt("locationAlt", 0);

        String latLong = convert(currentLatitude, currentLongitude);

        String strAltitude = "Altitude: " + currentAltitude;


        return "Last saved location: \n"+
                latLong + "\n" +
                strAltitude + "m";

    }

    public void isProviderEnabled(){
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        if(!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))){
            Toast.makeText(this, "Turn on GPS location.", Toast.LENGTH_SHORT).show();
            locationTextView.setText(savedLocationString());

        }
    }

    public void isFirstTime(){
        if (settings.getBoolean("my_first_time", true)) {
            //the app is being launched for first time, do something
            showSnackBar(this);
            currentLongitude = 0;
            settings.edit().putBoolean("my_first_time", false).apply();
        }
    }

    public void getCurrentLocation(){
        if(settings.contains("location")){
            currentLongitude = settings.getFloat("location", 0);
        }
    }

    @SuppressLint("DefaultLocale")
    public void runEverySecond() {
        Date date = new Date();
        String stringTime = DateFormat.getTimeInstance().format(date);
        String stringDate = DateFormat.getDateInstance(2).format(date);
        String display = ("Date: " + stringDate + "\n" + "Time: " + stringTime);
        dateAndTimeTextView.setText(display);
        int tt = (1439  - PolarisHourAngle.getJulianDate(currentLongitude))/2;
        hours = (tt / 60) - 6;
        if (hours < 0){
            hours +=12;  //since both are ints, you get an int
        }
        int minutes = tt % 60;
        formatted = String.format("%02d", minutes);
        clockPosition = findViewById(R.id.clockPosition);
        String setText = ""+ hours + ":" + formatted;
        clockPosition.setText(setText);

        seekbar.setProgress(1439 - PolarisHourAngle.getJulianDate(currentLongitude));
        
    }

    public void locationChanged(Location location){
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        try {
            List<Address> listAddresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if(listAddresses != null && listAddresses.size() != 0){

                String altitude = "Altitude: " + (int)location.getAltitude() + "m";

                currentLongitude = location.getLongitude();
                currentLatitude = location.getLatitude();
                currentAltitude = (int) location.getAltitude();

                String display ="Current location: \n" + convert(currentLatitude, currentLongitude) + "\n" + altitude;

                locationTextView.setText(display);
                seekbar.setProgress(1439 - PolarisHourAngle.getJulianDate(currentLongitude));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // we ask for permission

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {

            // we have permission

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);

        }
    }

    private String convert(double latitude, double longitude) {
        StringBuilder builder = new StringBuilder();
        builder.append("Latitude: ");

        String latitudeDegrees = Location.convert(Math.abs(latitude), Location.FORMAT_SECONDS);
        String[] latitudeSplit = latitudeDegrees.split(":");
        builder.append(latitudeSplit[0]);
        builder.append("°");

        builder.append(latitudeSplit[1]);
        builder.append("'");
        double latDouble = Double.parseDouble(latitudeSplit[2]);
        @SuppressLint("DefaultLocale") String roundedLat = String.format( "%.1f", latDouble);
        builder.append(roundedLat);
        builder.append("\"");

        if (latitude < 0) {
            builder.append(" S");
        } else {
            builder.append(" N");
        }

        builder.append(" \n");
        builder.append("Longitude: ");

        String longitudeDegrees = Location.convert(Math.abs(longitude), Location.FORMAT_SECONDS);
        String[] longitudeSplit = longitudeDegrees.split(":");
        builder.append(longitudeSplit[0]);
        builder.append("°");
        builder.append(longitudeSplit[1]);
        builder.append("'");
        double longDouble = Double.parseDouble(longitudeSplit[2]);
        @SuppressLint("DefaultLocale") String roundedLong = String.format( "%.1f", longDouble);
        builder.append(roundedLong);
        builder.append("\"");
        if (longitude < 0) {
            builder.append(" W");
        } else {
            builder.append(" E");
        }

        return builder.toString();
    }
}