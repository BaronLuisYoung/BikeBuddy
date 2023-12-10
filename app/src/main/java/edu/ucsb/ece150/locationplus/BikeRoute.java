package edu.ucsb.ece150.locationplus;

import android.util.Pair;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;
import java.util.List;

public class BikeRoute {

    private final int id;
    //create private list of pair of doubles
    private List<Point> coordinates;

    public BikeRoute(List<Point>latLngList, int id) { //add timestamps argument later
        this.id = id;
        coordinates = new ArrayList<>();
        coordinates.addAll(latLngList);

        //this.timestamps = timestamps;
        //this.videoReference = videoReference;

    }

    public List<Point> getCoordinates() {
        return coordinates;
    }
    //create a toString method that returns the coordinates
    @NonNull
    @Override
    public String toString() {
        return "ID: " + id;
    }

    //private String videoReference; // Can be a file path, URL, or database ID
    // Constructors, getters and setters for timestamps and polyline...

    //Getter that converts coordinates to latlng

    /*
    public String getVideoReference() {
        return videoReference;
    }

    public void setVideoReference(String videoReference) {
        this.videoReference = videoReference;
    }
     */

    // Other methods...

}
