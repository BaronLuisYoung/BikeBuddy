package edu.ucsb.ece150.locationplus;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.view.Gravity;
import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
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


import java.lang.reflect.Type;

//may utilize later
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MapsActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback {

    /*---------------------------------Constants------------------------------------*/
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 2;
    /*---------------------------------UI Components--------------------------------*/
    // TextViews for displaying acceleration and velocity
    TextView accelTextView;
    TextView velocityTextView;

    /*---------------------------------Location--------------------------------------*/
    // Manages and interacts with location services
    private GoogleMap mMap; // Google Map object for display and interaction
    private LocationManager mLocationManager; // For obtaining the user's location

    //TODO: check if this is needed
    private FusedLocationProviderClient fusedLocationProviderClient; // Alternative location API
    private Location currentLocation; // Stores the current location
    private Marker currentUserLocationMarker; // Marker for the current user location on the map
    private LatLng userLocation; // Latitude and Longitude of the user
    private CameraPosition mCameraPosition; // Camera position for map view
    private Location oldLocation; // Stores the previous location

    /*---------------------------------Route Management-------------------------------*/
    // Manages the bike route points and drawing on the map
    private final List<Point> bikeRoutePoints = new ArrayList<>(); // Stores points of the bike route
    private Polyline currentBikeRoute; // Polyline to draw the current bike route
    private ArrayList<BikeRoute> bikeRoutes; // List of saved bike routes
    private boolean drawRoute; // Flag to control route drawing
    private ConstraintLayout rideInfo; // Layout for displaying ride information
    private int rideID = 1; // Identifier for the current ride

    /*-------------------------------Sensor Management-------------------------------*/
    // Manages sensor data and updates
    private SensorManager mSensorManager; // Sensor manager for accessing device sensors
    private Sensor accelerometer; // Accelerometer sensor for movement detection
    private long lastUpdateTime; // Time of the last sensor update

    /*---------------------------------Preferences-----------------------------------*/
    // Handles shared preferences for storing and retrieving data
    private static SharedPreferences sharedPref; // Shared preferences object
    /*---------------------------------Timing----------------------------------------*/
    // Manages time measurements and intervals
    long oldTime = 0; // Stores an old time value for calculations
    long rideStartTime = 0; // Stores the time the ride started

    /*---------------------------------Speed & Distance------------------------------*/
    // Variables for speed and distance calculations
    private boolean startSpeed = false; // Flag to indicate if speed calculation should start
    private float totalDistance = 0f; // Accumulated distance traveled

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
        String newBikeRoutePoints = sharedPref.getString("newBikeRoutePoints", "");
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove("newBikeRoutePoints");
        editor.commit();
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

    private void showStartRideConfirmationDialog(View view) {
        new AlertDialog.Builder(this)
                .setTitle("Start Ride")
                .setMessage("Are you sure you want to start a new ride?")
                .setIcon(R.drawable.ic_launcher_foreground)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        handleStartRideButtonClick(view);
                    }})
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void showEndRideConfirmationDialog(View view) {
        new AlertDialog.Builder(this)
                .setTitle("End Ride")
                .setMessage("Are you sure you want to end the ride?")
                .setIcon(R.drawable.ic_launcher_foreground)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        handleStartRideButtonClick(view);
                    }})
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void initializeUIElements() {
        Toolbar mToolbar = findViewById(R.id.appToolbar);
        setSupportActionBar(mToolbar);

        ImageButton btnCenterOnUser = findViewById(R.id.button_center_on_user);

        FloatingActionButton startRideFab = findViewById(R.id.start_ride_button);
        startRideFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: add prompt to confirm start ride
                if(!drawRoute)
                    showStartRideConfirmationDialog(view);
                else
                    showEndRideConfirmationDialog(view);
            }
        });
        rideInfo = findViewById(R.id.ride_info);
        if (drawRoute) {
            rideInfo.setVisibility(View.VISIBLE);
        } else {
            rideInfo.setVisibility(View.INVISIBLE);
        }

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
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 30)); // You can define the zoom level
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
        FloatingActionButton startRideFab = findViewById(R.id.start_ride_button);

        drawRoute = !drawRoute;
        if (drawRoute) {
            Toast toast = Toast.makeText(view.getContext(), "Ride Started!", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, 300);
            toast.show();
            totalDistance = 0.0f;
            rideInfo.setVisibility(View.VISIBLE);
            rideStartTime = System.currentTimeMillis();
            startRideFab.setImageResource(android.R.drawable.ic_delete);

        } else {
            Toast toast = Toast.makeText(view.getContext(), "Ride Ended!", Toast.LENGTH_SHORT);
            toast.show();
            BikeRoute temp = new BikeRoute(bikeRoutePoints, rideID, totalDistance);
            rideID++;
            bikeRoutes.add(temp);
            bikeRoutePoints.clear();
            Gson gson = new Gson();
            String json = gson.toJson(bikeRoutes);
            SharedPreferences shared = getSharedPreferences("RoutesList", MODE_PRIVATE);
            SharedPreferences.Editor editor = shared.edit();
            editor.putString("bikeRoutesList", json);
            editor.apply();
            rideInfo.setVisibility(View.INVISIBLE);
            startRideFab.setImageResource(android.R.drawable.ic_input_add);

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

    private void initializeVariables(){
        accelTextView = findViewById(R.id.currentAcceleration);
        velocityTextView = findViewById(R.id.currentSpeed);
        bikeRoutes = new ArrayList<>();
        oldTime = System.currentTimeMillis();
    }

    private void loadSharedPrefsandRestoreData(){
        SharedPreferences shared = getSharedPreferences("RoutesList", MODE_PRIVATE);
        sharedPref = getSharedPreferences("BikeBuddyPrefs", Context.MODE_PRIVATE);
        drawRoute = sharedPref.getBoolean("drawRoute", false);
        Gson gson = new Gson();
        String json = shared.getString("bikeRoutesList", "");
        if (!json.equals("")) {
            BikeRoute[] routes = gson.fromJson(json, BikeRoute[].class);
            bikeRoutes.addAll(Arrays.asList(routes));
        }
        String bikeRoutePointsJson = sharedPref.getString("bikeRoutePoints", "");
        Log.d("MapsActivity", "loadSharedPrefsandRestoreData: " + bikeRoutePointsJson);
        if (drawRoute && !bikeRoutePointsJson.equals("")) {
            updateBikeRoutePointsFromJson(bikeRoutePointsJson);
        }
    }

    /*----------------------------//ACTIVITY STATE//-------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("MapsActivity", "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        initializeVariables();
        loadSharedPrefsandRestoreData();
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
        rideID = sharedPref.getInt("rideID", 0);
        if(drawRoute) {
            restoreBikeRoutePoints();
            rideStartTime = sharedPref.getLong("rideStartTime", 0);
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
        Log.d("MapsActivity", "onResume: drawRoute is" + drawRoute);
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MapsActivity", "onPause: entered");
        if (drawRoute) {
            if (currentBikeRoute != null) {
                currentBikeRoute.remove();
            }
        }
        if (sharedPref != null) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putLong("rideStartTime", rideStartTime);
            //save camera position
            if (mMap != null) {
                mCameraPosition = mMap.getCameraPosition();
                editor.putFloat("camera_lat", (float) mCameraPosition.target.latitude);
                editor.putFloat("camera_lng", (float) mCameraPosition.target.longitude);
                editor.putFloat("camera_zoom", mCameraPosition.zoom);
                editor.apply();
            }

            editor.putBoolean("drawRoute", drawRoute);
            editor.putInt("rideID", rideID);

            Gson gson = new Gson();
            Type type = new TypeToken<List<Point>>() {}.getType();
            String jsonString = gson.toJson(bikeRoutePoints,type);
            editor.putString("bikeRoutePoints", jsonString);
            editor.commit();
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
        //if a ride is in progress end the ride
        if(drawRoute){
//            drawRoute = false;
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("drawRoute", drawRoute);
            //editor.putString("bikeRoutePoints", bikeRoutePoints);
            editor.commit();
        }
        //stop DrawRouteService since we are closing the app
        Log.d("MapsActivity", "onDestroy: closing app, stopping service");
        Intent serviceIntent = new Intent(this, DrawRouteService.class);
        stopService(serviceIntent);
    }
    /*-----------------//MAP & LOCATION IMPLEMENTED FUNCTION//------------------*/
    /* These classes are from the implementation used in class definition
         ... implements LocationListener, OnMapReadyCallback    */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        sharedPref = getSharedPreferences("BikeBuddyPrefs", Context.MODE_PRIVATE);
        Log.d("MapsActivity", "onMapReady: entered");

        //for when user clicks on a route in the list
        //get the index of the route clicked on and draw it on the map
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
        //set camera location on position of user back to where user was
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
    public void onLocationChanged(@NonNull Location location) {
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

            long currentTimeMS = System.currentTimeMillis();
            long timeDifference = currentTimeMS - rideStartTime;

            TextView textView = findViewById(R.id.current_time);
            String timeText = String.format(Locale.US, "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(timeDifference),
                    TimeUnit.MILLISECONDS.toMinutes(timeDifference) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(timeDifference) % TimeUnit.MINUTES.toSeconds(1));

            textView.setText(timeText);

            Gson gson = new Gson();
            Type type = new TypeToken<List<Point>>() {}.getType();
            String jsonString = gson.toJson(bikeRoutePoints,type);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("bikeRoutePoints", jsonString);
            editor.commit();

            updateTotalDistance();
            updateBikeRouteOnMap();
            LatLng tempLoc = new LatLng(location.getLatitude(), location.getLongitude());
            updateCameraBearing(mMap,location.getBearing(), tempLoc);
        }

        velocityTextView = findViewById(R.id.currentSpeed);
        String velocityText = String.format(Locale.US, "%.2f", location.getSpeed()*3.6) + " KPH";
        velocityTextView.setText(velocityText);

        TextView elevationTextView = findViewById(R.id.elevation);
        String elevationText = String.format(Locale.US, "ELV. %.2f", location.getAltitude()) + " m";
        elevationTextView.setText(elevationText);


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
        String accelText = String.format(Locale.US, "%.2f", acceleration) + " m/s²";
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
        distanceTextView.setText(String.format(Locale.US, "%.2f km", totalDistance / 1000));
    }
    //TODO add bearing from compass, adjust the zoom
    //function used to update camera orientation
    private void updateCameraBearing(GoogleMap googleMap, float bearing, LatLng latLng) {
        if (googleMap == null){ return;}
        Log.d("MapsActivity", "updateCameraBearing: Bearing = " + bearing);
        mCameraPosition = CameraPosition
                .builder(googleMap.getCameraPosition())
                .bearing(bearing)
                .target(latLng)
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
        if(mMap != null){
            currentBikeRoute = mMap.addPolyline(polylineOptions);
        }
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

