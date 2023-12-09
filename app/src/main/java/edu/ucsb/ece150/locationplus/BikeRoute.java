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
    private List<Pair<Double, Double>> coordinates;
    public BikeRoute(List<LatLng>latLngList, int id) { //add timestamps argument later
        this.id = id;
        coordinates = new ArrayList<>();
        for (LatLng latLng : latLngList) {
            coordinates.add(new Pair<>(latLng.latitude, latLng.longitude));
        }

        //this.timestamps = timestamps;
        //this.videoReference = videoReference;

    }

    //create a toString method that returns the coordinates
    @NonNull
    @Override
    public String toString() {
        return "ID: " + id + "\n Coord: " + coordinates.toString();
    }

    //private String videoReference; // Can be a file path, URL, or database ID
    // Constructors, getters and setters for timestamps and polyline...

    //Getter that converts coordinates to latlng
    public List<LatLng> getLatLngList() {
        List<LatLng> latLngList = new ArrayList<>();
        for (Pair<Double, Double> coordinate : coordinates) {
            latLngList.add(new LatLng(coordinate.first, coordinate.second));
        }
        return latLngList;
    }

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
