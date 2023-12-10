package edu.ucsb.ece150.locationplus;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Point {
    private final Pair<Double, Double> location;
    private final long timeStamp;

    public Point(Double lat, Double lng, long timeStamp) {
        this.location = new Pair<>(lat, lng);
        this.timeStamp = timeStamp;
    }

    public Pair<Double, Double> getLocation() {
        return location;
    }

    @NonNull
    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        // Convert the timestamp into a Date object
        Date date = new Date(timeStamp);

        // Format the date into a human-readable string
        String formattedDate = sdf.format(date);
        return "Lat: " + location.first + " Lng: " + location.second + " Time: " + formattedDate;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

}
