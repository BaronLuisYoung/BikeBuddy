package edu.ucsb.ece150.locationplus;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.Bundle;
import android.util.Log;
import android.Manifest;
import android.util.Pair;
import android.util.TimeUtils;
import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import android.widget.ArrayAdapter;


import java.lang.reflect.Type;

//may utilize later
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MapsActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback {

    /*---------------------------------Constants------------------------------------*/

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;


    /*------------------------------------------------------------------------------*/
    long oldTime = 0;
    private GoogleMap mMap;
    private LocationManager mLocationManager;
    private SensorManager mSensorManager;
    private Sensor accelerometer;

    private long lastUpdateTime;

    TextView accelTextView;
    TextView velocityTextView;
    Location currentLocation;
    private Marker currentUserLocationMarker;
    FusedLocationProviderClient fusedLocationProviderClient;
    private LatLng userLocation;
    private static SharedPreferences sharedPref;
    private CameraPosition mCameraPosition;
    private List<Point> bikeRoutePoints = new ArrayList<>();
    private Polyline currentBikeRoute;
    private ArrayList<BikeRoute> bikeRoutes;
    private boolean drawRoute;
    private boolean startSpeed = false;

    private Location oldLocation;
    private int count = 0;
    private ArrayAdapter<BikeRoute> adapter;

    private float totalDistance = 0f;

    /*----------------------onCreate Set Up functions----------------------------------*/

    private void updateBikeRoutePointsFromJson(String bikeRoutePointsJson) {
        Log.d("MapsActivity", "updateBikeRoutePointsFromJson: entered");
        Gson gson = new Gson();
        Type type = new TypeToken<List<Point>>() {}.getType();
        List<Point> bikeRoutePointsList = gson.fromJson(bikeRoutePointsJson, type);
        //bikeRoutePoints.clear();
        bikeRoutePoints.addAll(bikeRoutePointsList);
    }

    private void restoreBikeRoutePoints() {
        Log.d("MapsActivity", "restoreBikeRoutePoints: entered");
        String bikeRoutePoints1 = sharedPref.getString("bikeRoutePoints", "");
        String newBikeRoutePoints = sharedPref.getString("newBikeRoutePoints", "");
        if (!bikeRoutePoints1.isEmpty()) {
            updateBikeRoutePointsFromJson(bikeRoutePoints1);
        }
        Log.d("MapsActivity", "restored newBikeRoutePoints: " + newBikeRoutePoints);
        if(!newBikeRoutePoints.isEmpty() && !newBikeRoutePoints.equals("null")){
            updateBikeRoutePointsFromJson(newBikeRoutePoints);
            Log.d("MapsActivity", "restoreBikeRoutePoints: data restored from service");
        }
    }

    private void initializeGoogleMaps() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    private void initializeSensors() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    private void initializeUIElements() {
        Toolbar mToolbar = findViewById(R.id.appToolbar);
        setSupportActionBar(mToolbar);

        ImageButton btnCenterOnUser = findViewById(R.id.button_center_on_user);

        FloatingActionButton startRideFab = findViewById(R.id.start_ride_button);
        startRideFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleStartRideButtonClick(view);
            }
        });

        ImageButton btnViewPreviousRides = findViewById(R.id.list_routes_button);
        btnViewPreviousRides.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MapsActivity", "onClick: View Previous Rides button clicked");
                Intent intent = new Intent(MapsActivity.this, RoutesListActivity.class);
                //convert bikeRoutes to json
                startActivityForResult(intent, 1);
            }
        });

        startRideFab.setEnabled(true); // Enable the FAB
        btnCenterOnUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MapsActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE);
                }
                Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (lastKnownLocation != null) {
                    userLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15)); // You can define the zoom level
                }
            }
        });
    }

    private void setupLocationServices() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void handleStartRideButtonClick(View view) {
        Log.d("MapsActivity", "onClick: Ride start button clicked");
        drawRoute = !drawRoute;
        if (drawRoute) {
            Toast.makeText(view.getContext(), "Ride Started!", Toast.LENGTH_SHORT).show();
            totalDistance = 0.0f;
        } else {
            Toast.makeText(view.getContext(), "Ride Ended!", Toast.LENGTH_SHORT).show();
            BikeRoute temp = new BikeRoute(bikeRoutePoints, count);
            count++;
            bikeRoutes.add(temp);
            bikeRoutePoints.clear();
            Gson gson = new Gson();
            String json = gson.toJson(bikeRoutes);
            SharedPreferences shared = getSharedPreferences("RoutesList", MODE_PRIVATE);
            SharedPreferences.Editor editor = shared.edit();
            editor.putString("bikeRoutesList", json);
            editor.apply();

        }
    }

    @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("MapsActivity", "onActivityResult: entered");
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Log.d("MapsActivity", "onActivityResult: result ok");
                int index = data.getIntExtra("routeIndex", -1);
                if(index != -1){
                    Log.d("MapsActivity", "onActivityResult: index = " + index);
                    BikeRoute route = bikeRoutes.get(index);
                    List<Point> points = route.getCoordinates();
                    List<LatLng> latLngList = new ArrayList<>();
                    for (Point point : points) {
                        Pair<Double, Double> location = point.getLocation();
                        LatLng latLng = new LatLng(location.first, location.second);
                        latLngList.add(latLng);
                    }
                    drawBikeRoutePolyline(latLngList);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngList.get(0), 35));
                }
            }
        }
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // Calculate acceleration and velocity
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // You can leave this method empty if you're not using it
        }
    };

    /*----------------------------//ACTIVITY STATE//-------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("MapsActivity", "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        accelTextView = findViewById(R.id.currentAcceleration);
        velocityTextView = findViewById(R.id.currentSpeed);
        bikeRoutes = new ArrayList<>();
        oldTime = System.currentTimeMillis();


        SharedPreferences shared = getSharedPreferences("RoutesList", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = shared.getString("bikeRoutesList", "");
        if (!json.equals("")) {
            BikeRoute[] routes = gson.fromJson(json, BikeRoute[].class);
            bikeRoutes.addAll(Arrays.asList(routes));
        }

        initializeGoogleMaps();
        initializeSensors();
        initializeUIElements();
        setupLocationServices();


    }
    @Override
    protected void onResume() {
        super.onResume();
        Intent serviceIntent = new Intent(this, DrawRouteService.class);
        stopService(serviceIntent);

        Log.d("MapsActivity", "onResume: entered");
        sharedPref = getSharedPreferences("BikeBuddyPrefs", Context.MODE_PRIVATE);
        count = sharedPref.getInt("count", 0);
        if(drawRoute) {
            restoreBikeRoutePoints();
            if (bikeRoutePoints.size() > 0) {
                updateBikeRouteOnMap();
                //new points are not added to map until you call updateBikeOnMap();
                Log.d("MapsActivity", "drawing poly lines");

            }
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        drawRoute = sharedPref.getBoolean("drawRoute", false);
        Log.d("MapsActivity", "onResume: " + drawRoute);
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MapsActivity", "onPause: entered");
        if (drawRoute) {
            currentBikeRoute.remove();
        }
        if (sharedPref != null) {
            SharedPreferences.Editor editor = sharedPref.edit();

            //save camera position
            if (mMap != null) {
                mCameraPosition = mMap.getCameraPosition();
                editor.putFloat("camera_lat", (float) mCameraPosition.target.latitude);
                editor.putFloat("camera_lng", (float) mCameraPosition.target.longitude);
                editor.putFloat("camera_zoom", mCameraPosition.zoom);
                editor.apply();
            }

            editor.putBoolean("drawRoute", drawRoute);
            editor.putInt("count", count);

            Gson gson = new Gson();
            Type type = new TypeToken<List<Point>>() {}.getType();
            String jsonString = gson.toJson(bikeRoutePoints,type);
            editor.putString("bikeRoutePoints", jsonString);
            editor.apply();
        }
        if (mLocationManager != null) {
            mLocationManager.  removeUpdates(this);
        }

        Log.d("MapsActivity", "onPause: starting service");
        Intent serviceIntent = new Intent(this, DrawRouteService.class);
        serviceIntent.putExtra("drawRoute", drawRoute);
        startService(serviceIntent);
    }
    @Override
    protected void onStop() {
        super.onStop();
        mLocationManager.removeUpdates(this);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    /*-----------------//MAP & LOCATION IMPLEMENTED FUNCTION//------------------*/
    /* These classes are from the implementation used in class definition
         ... implements LocationListener, OnMapReadyCallback    */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        sharedPref = getSharedPreferences("BikeBuddyPrefs", Context.MODE_PRIVATE);
        Log.d("MapsActivity", "onMapReady: entered");

        Intent intent = getIntent();
        int index = intent.getIntExtra("routeIndex", -1);
        if(index != -1){
            Log.d("MapsActivity", "onMapReady: index = " + index);
            BikeRoute route = bikeRoutes.get(index);
            List<Point> points = route.getCoordinates();
            List<LatLng> latLngList = new ArrayList<>();
            for (Point point : points) {
                Pair<Double, Double> location = point.getLocation();
                LatLng latLng = new LatLng(location.first, location.second);
                latLngList.add(latLng);
            }
            drawBikeRoutePolyline(latLngList);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngList.get(0), 15));
        }
        //set camera location on position of user may not be necessary
        if (mMap != null) {
            SharedPreferences sharedPref = getSharedPreferences("BikeBuddyPrefs", Context.MODE_PRIVATE);
            float lat = sharedPref.getFloat("camera_lat", 34); // Provide a default value
            float lng = sharedPref.getFloat("camera_lng", 119);
            float zoom = sharedPref.getFloat("camera_zoom", 15);

            if (lat != 34 && lng != 119) {
                LatLng cameraLatLng = new LatLng(lat, lng);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cameraLatLng, zoom));
            }
        }

    }
    @Override
    public void onLocationChanged(Location location) {
        Log.d("MapsActivity", "onLocationChanged: ");
        if (!startSpeed) {
            oldLocation = location;
            startSpeed = true;
        }
        // Behavior for when a location update is received
        userLocation = new LatLng(location.getLatitude(), location.getLongitude());

        // If it's the first location update or if marker doesn't exist
        if (currentUserLocationMarker == null) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(userLocation);
            markerOptions.title("Current Location");

            //TODO: change marker color and style
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

            currentUserLocationMarker = mMap.addMarker(markerOptions);
            Log.d("MapsActivity", "onLocationChanged: created new user position and marker success");
        } else {
            currentUserLocationMarker.setPosition(userLocation);
            Log.d("MapsActivity", "onLocationChanged: updated user position and marker success");
        }
        // Update the polyline on the map
        if(drawRoute) {
            Log.d("MainActivity", "onLocationChanged: drawing route");
            //new points are not added to map until you call updateBikeOnMap();
            Point newPoint = new Point(location.getLatitude(), location.getLongitude(), location.getTime());
            bikeRoutePoints.add(newPoint);
            updateTotalDistance();
            updateBikeRouteOnMap();
            updateCameraBearing(mMap,location.getBearing());
        }

        velocityTextView = findViewById(R.id.currentSpeed);
        String velocityText = "Velocity: " + String.format(Locale.US, "%.2f", location.getSpeed()*3.6) + " KPH";
        velocityTextView.setText(velocityText);

        long currentTimeMS = System.currentTimeMillis();

        long timeDifference = currentTimeMS - oldTime;

        oldTime = currentTimeMS;

        long TimeDifferenceInSec = TimeUnit.MILLISECONDS.toSeconds(timeDifference);
        float SpeedDifference = location.getSpeed() - oldLocation.getSpeed();
        float acceleration = SpeedDifference / (float)TimeDifferenceInSec;
        if (Float.isInfinite(acceleration) || Float.isNaN(acceleration)) {
            acceleration = 0.00f;
        }
        accelTextView = findViewById(R.id.currentAcceleration);
        String accelText = "Acceleration: " + String.format(Locale.US, "%.2f", acceleration) + " m/s²";
        accelTextView.setText(accelText);

    }

    public float calculateDistance(Point start, Point end) {
        float[] results = new float[1];
        Pair<Double, Double> startLocation = start.getLocation();
        Pair<Double, Double> endLocation = end.getLocation();
        Location.distanceBetween(startLocation.first,
                                startLocation.second,
                                endLocation.first,
                                endLocation.second, results);
        return results[0]; // Distance in meters
    }
    public void updateTotalDistance() {
        if(bikeRoutePoints.size() > 1){
            Point start = bikeRoutePoints.get(bikeRoutePoints.size()-1);
            Point end = bikeRoutePoints.get(bikeRoutePoints.size()-2);
            totalDistance += calculateDistance(start, end);
        }
        // Display the total distance in a TextView
        TextView distanceTextView = findViewById(R.id.current_distance_traveled); // Make sure you have this TextView in your layout
        distanceTextView.setText(String.format(Locale.US, "Distance Traveled: %.2f km", totalDistance / 1000));
    }
    //TODO add bearing from compass, adjust the zoom
    //function used to update camera orientation
    private void updateCameraBearing(GoogleMap googleMap, float bearing) {
        if (googleMap == null){ return;}
        Log.d("MapsActivity", "updateCameraBearing: Bearing = " + bearing);
        mCameraPosition = CameraPosition
                .builder(googleMap.getCameraPosition())
                .bearing(bearing)
                .build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));
    }

    //------------------------//MAP ROUTE DRAWING//-----------------------------//
    private void updateBikeRouteOnMap() {
        if (currentBikeRoute != null) {
            currentBikeRoute.remove(); // Remove the old polyline
        }

        List<LatLng> tempLatLngList = new ArrayList<>();
        for (Point point : bikeRoutePoints) {
            Pair<Double, Double> location = point.getLocation();
            LatLng latLng = new LatLng(location.first, location.second);
            tempLatLngList.add(latLng);
        }

        drawBikeRoutePolyline(tempLatLngList);
    }

    private void drawBikeRoutePolyline(List<LatLng> bikeRoutePoints) {
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(bikeRoutePoints)
                .width(5) // Width of the polyline
                .color(Color.BLUE); // Color of the polyline
        if (currentBikeRoute != null) {
            currentBikeRoute.remove(); // Remove the old polyline
        }
        currentBikeRoute = mMap.addPolyline(polylineOptions);
    }

    //------------------------------------------------------------------------//
    //Functions below need to be overridden but not used
    @Override
    public void onProviderDisabled(@NonNull String provider) {}
    @Override
    public void onProviderEnabled(@NonNull String provider) {}
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}


}

