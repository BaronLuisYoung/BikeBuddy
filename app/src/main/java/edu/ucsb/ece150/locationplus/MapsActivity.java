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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.Bundle;
import android.util.Log;
import android.Manifest;
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
    Location currentLocation;
    private Marker currentUserLocationMarker;
    FusedLocationProviderClient fusedLocationProviderClient;
    private LatLng userLocation;

    private static SharedPreferences sharedPref;

    private FloatingActionButton deleteGeoFenceBtn;

    private CameraPosition cameraPosition; //may use later

    private List<LatLng> bikeRoutePoints = new ArrayList<>();
    private Polyline currentBikeRoute;
    private ArrayList<BikeRoute> bikeRoutes;

    private boolean drawRoute = false;
    private ArrayAdapter<BikeRoute> adapter;

    /*--------------------//ACTIVITY STATE//----------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("MapsActivity", "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        sharedPref = getSharedPreferences("BikeBuddyPrefs", Context.MODE_PRIVATE);

        bikeRoutes = new ArrayList<BikeRoute>();

        // Set up Google Maps
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);

        assert mapFragment != null;  //checks if google map is NULL
        mapFragment.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //System level access to GPS, Notifications, and more.
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Log.d("MapsActivity", "onCreate: created mLocation Manager, success");

        //Sets up Toolbar
        Toolbar mToolbar = (Toolbar) findViewById(R.id.appToolbar);
        setSupportActionBar(mToolbar);

        //sets up button to center on user location
        ImageButton btnCenterOnUser = findViewById(R.id.button_center_on_user);

        //Start Ride Button
        FloatingActionButton startRideFab = findViewById(R.id.start_ride_button);
        startRideFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MapsActivity", "onClick: Ride start button clicked");

                drawRoute = !drawRoute;
                if(drawRoute) {
                    Toast.makeText(view.getContext(), "Ride Started!", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(view.getContext(), "Ride Ended!", Toast.LENGTH_SHORT).show();
                    bikeRoutes.add(new BikeRoute(bikeRoutePoints));
                    bikeRoutePoints.clear();
//
//                  currentBikeRoute.remove();

                }
            }
        });

        // If you need to enable the FAB based on certain conditions
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

        //sets up button in toolbar to view previous rides
        ImageButton btnViewPreviousRides = findViewById(R.id.list_routes_button);
        btnViewPreviousRides.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MapsActivity", "onClick: View Previous Rides button clicked");
                Intent intent = new Intent(MapsActivity.this, RoutesListActivity.class);
                //convert bikeRoutes to json
                Gson gson = new Gson();
                String json = gson.toJson(bikeRoutes);
                intent.putExtra("bikeRoutesKey", json);
                startActivity(intent);
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (sharedPref != null) {
            SharedPreferences.Editor editor = sharedPref.edit();

            //save camera position
            if (mMap != null) {
                cameraPosition = mMap.getCameraPosition();
                editor.putFloat("camera_lat", (float) cameraPosition.target.latitude);
                editor.putFloat("camera_lng", (float) cameraPosition.target.longitude);
                editor.putFloat("camera_zoom", cameraPosition.zoom);
                editor.apply();
            }

            editor.apply();
        }

        if (mLocationManager != null) {
            mLocationManager.  removeUpdates(this);
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        mLocationManager.removeUpdates(this);
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


        //set camera location and position of user
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
            //new points are not added to map until you call updateBikeOnMap();
            LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());
            bikeRoutePoints.add(newPoint);
            updateBikeRouteOnMap();
        }

        // Move the camera to the user's location and zoom in
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
    }


    //------------------------------------------------------------------------//
    //Functions below need to be overridden but not used
    @Override
    public void onProviderDisabled(@NonNull String provider) {}
    @Override
    public void onProviderEnabled(@NonNull String provider) {}
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    //------------------------//MAP ROUTE DRAWING//-----------------------------//
    private void updateBikeRouteOnMap() {
        if (currentBikeRoute != null) {
            currentBikeRoute.remove(); // Remove the old polyline
        }
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(bikeRoutePoints)
                .width(5) // Width of the polyline
                .color(Color.BLUE); // Color of the polyline

        currentBikeRoute = mMap.addPolyline(polylineOptions);
}

}

