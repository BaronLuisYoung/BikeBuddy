package edu.ucsb.ece150.locationplus;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
public class RoutesListActivity extends AppCompatActivity {

    ArrayAdapter<BikeRoute> adapter;

    private ArrayList<BikeRoute> bikeRoutesList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routes_list);
        bikeRoutesList = new ArrayList<>();

        SharedPreferences shared = getSharedPreferences("RoutesList", MODE_PRIVATE);
        String json = shared.getString("bikeRoutesList", "");
        if (!json.equals("")) {
            Gson gson = new Gson();
            BikeRoute[] route = gson.fromJson(json, BikeRoute[].class);
            bikeRoutesList.addAll(Arrays.asList(route));
        }

        initializeToolbar();
        setupListView();
        setupFabButton();
    }
    @Override
    protected void onPause() {
        super.onPause();
    }
    private void initializeToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }
    private void retrieveIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            Gson gson = new Gson();
            String json = intent.getStringExtra("bikeRoutesKey");
            BikeRoute[] bikeRoutes = gson.fromJson(json, BikeRoute[].class);
            if (bikeRoutes != null) {
                bikeRoutesList.addAll(Arrays.asList(bikeRoutes));
                intent.removeExtra("bikeRoutesKey");
            }
        }
    }

    private void handleSelectedAction(int position, int action, ArrayAdapter<BikeRoute> adapter) {
        switch (action) {
            case 0: // Delete Route
                new AlertDialog.Builder(RoutesListActivity.this)
                        .setTitle("Confirm Delete")
                        .setMessage("Are you sure you want to delete this route?")
                        .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                            // Code to delete the route
                            bikeRoutesList.remove(position);

                            Gson gson = new Gson();
                            String json = gson.toJson(bikeRoutesList);
                            SharedPreferences shared = getSharedPreferences("RoutesList", MODE_PRIVATE);
                            SharedPreferences.Editor editor = shared.edit();
                            editor.putString("bikeRoutesList", json);
                            editor.apply();

                            adapter.notifyDataSetChanged();
                            // Update your adapter here
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show();
                break;
            case 1: // View Ride on Map
                // Code to view the ride on map
                Intent mapIntent = new Intent(RoutesListActivity.this, MapsActivity.class);
                mapIntent.putExtra("routeIndex", position);
                startActivity(mapIntent);
                break;
            case 2: // Watch Video
                // Code to watch the video
                break;
        }
    }

    private void setupListView()
    {
        ListView cardList = findViewById(R.id.cardList);
        if (bikeRoutesList != null) {
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, bikeRoutesList);
            cardList.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
        cardList.setOnItemClickListener((parent, view, position, id) -> {
            final CharSequence[] items = {"Delete Route", "View Ride on Map", "Watch Video"};
            AlertDialog.Builder builder = new AlertDialog.Builder(RoutesListActivity.this);
            builder.setTitle("Choose an action");
            builder.setItems(items, (dialog, which) -> handleSelectedAction(position, which, adapter));
            builder.show();
        });

    }

    private void setupFabButton() {
        FloatingActionButton fab = findViewById(R.id.fabExit);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Currently goes back to previous activity
                finish();
            }
        });
    }
}
