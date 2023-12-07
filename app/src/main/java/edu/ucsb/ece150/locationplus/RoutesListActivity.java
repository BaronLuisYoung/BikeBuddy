package edu.ucsb.ece150.locationplus;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;

public class RoutesListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routes_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //get intent from MapsActivity

        Intent intent = getIntent();
        ArrayList<BikeRoute> bikeRoutesList;
        ListView cardList = findViewById(R.id.cardList);

        if(intent != null) {
            Gson gson = new Gson();
            String json = intent.getStringExtra("bikeRoutesKey");
            BikeRoute[] bikeRoutes = gson.fromJson(json, BikeRoute[].class);
            bikeRoutesList = new ArrayList<>(Arrays.asList(bikeRoutes));
            ArrayAdapter<BikeRoute> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, bikeRoutesList);
            cardList.setAdapter(adapter);
            // If Parcelable was used
            // bikeRoutesList = intent.getParcelableArrayListExtra("bikeRoutesKey");
        }


        FloatingActionButton fab = findViewById(R.id.fabExit);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent toAddCardActivity = new Intent(getApplicationContext(), MapsActivity.class);
                //go back to previous activity
                finish();
            }
        });
    }
}