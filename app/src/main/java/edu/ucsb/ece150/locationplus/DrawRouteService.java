package edu.ucsb.ece150.locationplus;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.location.Location;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class DrawRouteService extends Service {

    private static final String CHANNEL_ID = "DrawRouteServiceChannel";
    private LocationManager locationManager;
    private LocationListener locationListener;
    private List<Point> bikeRoutePoints; // List to store location points

    boolean drawRoute;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("DrawRouteService", "onCreate: entered");
        createNotificationChannel();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        bikeRoutePoints = new ArrayList<>();
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                Log.d("DrawRouteService", "onLocationChanged: adding point");
                Point currentPoint = new Point(location.getLatitude(), location.getLongitude(), location.getTime());
                bikeRoutePoints.add(currentPoint);
                if(drawRoute) {
                    Log.d("MapsActivity", "onDestroy: points saved to shared pref");
                    saveBikeRoutePoints();
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) { }

            @Override
            public void onProviderEnabled(@NonNull String provider) { }

            @Override
            public void onProviderDisabled(@NonNull String provider) { }

        };



        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } catch (SecurityException e) {
            // Handle exception if location permission is not granted
        }
    }

    private void saveBikeRoutePoints() {
        Log.d("DrawRouteService", "saveBikeRoutePoints: entered");
        SharedPreferences prefs = getSharedPreferences("BikeBuddyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String bikeRoutePointsJson = gson.toJson(bikeRoutePoints);
        Log.d("DrawRouteService", "saveBikeRoutePoints: " + bikeRoutePointsJson );
        editor.putString("newBikeRoutePoints", bikeRoutePointsJson);
        editor.commit();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("DrawRouteService", "onStartCommand: entered");
        drawRoute = false;
        if (intent != null) {
            drawRoute = intent.getBooleanExtra("drawRoute", false);
            Log.d("DrawRouteService", "onStartCommand: drawRoute is " + drawRoute);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Tracking")
                .setContentText("Tracking your location in the background")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();

        startForeground(1, notification);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("DrawRouteService", "onDestroy: entered, " + drawRoute);
        // Stop location updates to conserve battery and resources
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
        stopForeground(true);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Location Service Channel";
            String description = "Channel for Location Service";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    //*-----Unused Binder methods-----*//
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        DrawRouteService getService() {
            return DrawRouteService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
