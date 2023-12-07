package edu.ucsb.ece150.locationplus;

import android.util.Pair;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;
import java.util.List;

public class BikeRoute {
    private List<String> timestamps;
    //create private list of pair of doubles
    private List<Pair<Double, Double>> coordinates;
    public BikeRoute(List<LatLng>latLngList) { //add timestamps argument later
        coordinates = new ArrayList<>();
        for (LatLng latLng : latLngList) {
            coordinates.add(new Pair<>(latLng.latitude, latLng.longitude));
        }

        //this.timestamps = timestamps;
        //this.videoReference = videoReference;
    }

    //create a tostring method that returns the coordinates
    @NonNull
    @Override
    public String toString() {
        return coordinates.toString();
    }
    //private String videoReference; // Can be a file path, URL, or database ID

    // Constructors, getters and setters for timestamps and polyline...


    //gettter that converts coordinates to latlng
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
