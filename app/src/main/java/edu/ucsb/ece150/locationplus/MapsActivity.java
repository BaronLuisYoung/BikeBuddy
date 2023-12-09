package edu.ucsb.ece150.locationplus;

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
import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.ImageButton;
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

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MapsActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private LocationManager mLocationManager;
    private SensorManager mSensorManager;
    private Sensor magnetometer;
    private Sensor accelerometer;
    Location currentLocation;
    private Marker currentUserLocationMarker;
    FusedLocationProviderClient fusedLocationProviderClient;
    private LatLng userLocation;
    private static SharedPreferences sharedPref;
    private CameraPosition mCameraPosition;
    private List<LatLng> bikeRoutePoints = new ArrayList<>();
    private Polyline currentBikeRoute;
    private ArrayList<BikeRoute> bikeRoutes;
    private boolean drawRoute;
    private int count = 0;
    private ArrayAdapter<BikeRoute> adapter;

    /*----------------------onCreate Set Up functions----------------------------------*/
    private void updateBikeRoutePointsFromJson(String bikeRoutePointsJson) {
        Log.d("MapsActivity", "updateBikeRoutePointsFromJson: entered");
        Gson gson = new Gson();
        Type type = new TypeToken<List<Pair<Double, Double>>>() {}.getType();
        List<Pair<Double, Double>> bikeRoutePointsList = gson.fromJson(bikeRoutePointsJson, type);
        bikeRoutePoints.clear();
        for (Pair<Double, Double> point : bikeRoutePointsList) {
            LatLng temp = new LatLng(point.first, point.second);
            bikeRoutePoints.add(temp);
        }
    }

    private void restoreBikeRoutePoints() {
        Log.d("MapsActivity", "restoreBikeRoutePoints: entered");
        sharedPref = getSharedPreferences("BikeBuddyPrefs", Context.MODE_PRIVATE);
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
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
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
                startActivity(intent);
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

    /*----------------------------//ACTIVITY STATE//-------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("MapsActivity", "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        bikeRoutes = new ArrayList<>();
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
        restoreBikeRoutePoints();

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

            Gson gson = new Gson();
            Type type = new TypeToken<List<Pair<Double, Double>>>() {}.getType();
            String jsonString = gson.toJson(convertLatLngListToPairList(bikeRoutePoints),type);
            editor.putString("bikeRoutePoints", jsonString);
            editor.apply();
        }
        if (mLocationManager != null) {
            mLocationManager.  removeUpdates(this);
        }

        Log.d("MapsActivity", "onPause: staring service");
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

    /*
        These classes are from the implementation used in class definition
         ... implements LocationListener, OnMapReadyCallback
    */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        sharedPref = getSharedPreferences("BikeBuddyPrefs", Context.MODE_PRIVATE);

        if(!bikeRoutePoints.isEmpty()) {
            drawBikeRoutePolyline(bikeRoutePoints);
            Log.d("MapsActivity", "drawing poly lines");
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
            LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());
            bikeRoutePoints.add(newPoint);
            updateBikeRouteOnMap();
            updateCameraBearing(mMap, location.getBearing());
        }
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

    private List<Pair<Double, Double>> convertLatLngListToPairList(List<LatLng> latLngList) {
        List<Pair<Double, Double>> pairList = new ArrayList<>();

        for (LatLng latLng : latLngList) {
            Pair<Double, Double> pair = new Pair<>(latLng.latitude, latLng.longitude);
            pairList.add(pair);
        }
        return pairList;
    }

    //------------------------//MAP ROUTE DRAWING//-----------------------------//
    private void updateBikeRouteOnMap() {
        if (currentBikeRoute != null) {
            currentBikeRoute.remove(); // Remove the old polyline
        }
        drawBikeRoutePolyline(bikeRoutePoints);
    }

    private void drawBikeRoutePolyline(List<LatLng> bikeRoutePoints) {
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(bikeRoutePoints)
                .width(5) // Width of the polyline
                .color(Color.BLUE); // Color of the polyline

        currentBikeRoute = mMap.addPolyline(polylineOptions);
    }

    private void loadBikeRoutePoints() {
        SharedPreferences prefs = getSharedPreferences("BikeRouteData", MODE_PRIVATE);
        String bikeRoutePointsJson = prefs.getString("bikeRoutePoints", null);
        if (bikeRoutePointsJson != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<Pair<Double, Double>>>() {}.getType();
            List<Pair<Double, Double>> bikeRoutePoints = gson.fromJson(bikeRoutePointsJson, type);

            // Now you have the bikeRoutePoints list, update your map accordingly
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

