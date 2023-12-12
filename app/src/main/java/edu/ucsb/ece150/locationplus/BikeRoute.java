package edu.ucsb.ece150.locationplus;

import android.util.Pair;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BikeRoute {
    private final float distanceTraveled;
    private final int id;

    //TODO display these fields in the MapsActivity when
    // the user clicks on a route from RoutesListActivity
    private float maxSpeed;
    private float avgSpeed;
    private float avgAltitude;
    private float maxAltitude;
    private float maxAcceleration;
    private String videoPath;
    private final List<Point> coordinates;

    public BikeRoute(List<Point>latLngList, int id, float distanceTraveled, String path) { //add timestamps argument later
        this.id = id;
        this.distanceTraveled = distanceTraveled;
        this.videoPath = path;
        coordinates = new ArrayList<>();
        coordinates.addAll(latLngList);
    }

    public List<Point> getCoordinates() {
        return coordinates;
    }
    //create a toString method that returns the coordinates
    @NonNull
    @Override
    public String toString() {
        Date dateStartTS = new Date(getStartTimestamp());
        Date dateEndTS  = new Date(getEndTimestamp());
        return "Ride ID: " + id + '\n'
                + "Distance Traveled: " + distanceTraveled + 'm' + '\n'
                + "Start Time: "  + dateStartTS + '\n'
                + "End Time: " + dateEndTS + '\n';
        }

    public float getDistanceTraveled() {
        return distanceTraveled;
    }
    public long getStartTimestamp() {
        if (coordinates.size() == 0) {
            return -1;
        }
        return coordinates.get(0).getTimeStamp();
    }

    public long getEndTimestamp() {
        if (coordinates.size() == 0) {
            return -1;
        }
        return coordinates.get(coordinates.size() - 1).getTimeStamp();
    }

    public String getVideoPath() {
        return videoPath;
    }



}
