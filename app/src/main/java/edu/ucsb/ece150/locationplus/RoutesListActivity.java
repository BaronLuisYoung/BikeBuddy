package edu.ucsb.ece150.locationplus;

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
        //retrieveIntentData();
        setupListView();
        setupFabButton();
    }
    @Override
    protected void onPause() {
        super.onPause();
        /*
        SharedPreferences shared = getSharedPreferences("RoutesList", MODE_PRIVATE);
        SharedPreferences.Editor editor = shared.edit();
        Gson gson = new Gson();
        String json = gson.toJson(bikeRoutesList);
        editor.putString("bikeRoutesList", json);
        editor.apply();
        */
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

    private void setupListView() {
        ListView cardList = findViewById(R.id.cardList);
        if (bikeRoutesList != null) {
            ArrayAdapter<BikeRoute> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, bikeRoutesList);
            cardList.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
    }

    private void setupFabButton() {
        FloatingActionButton fab = findViewById(R.id.fabExit);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Choose a route to interact with
                // Currently goes back to previous activity
                finish();
            }
        });
    }
}
