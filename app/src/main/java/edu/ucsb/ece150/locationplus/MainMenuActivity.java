package edu.ucsb.ece150.locationplus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        initializeStartRideButton();
        initializeViewPreviousRidesButton();
    }

    private void initializeStartRideButton() {
        Button startRideButton = findViewById(R.id.start_ride_button);
        startRideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMapsActivity();
            }
        });
    }

    private void initializeViewPreviousRidesButton() {
        Button viewPreviousRidesButton = findViewById(R.id.view_previous_rides_button);
        viewPreviousRidesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRoutesListActivity();
            }
        });
    }

    private void startMapsActivity() {
        // Start the MapsActivity
        Intent intent = new Intent(MainMenuActivity.this, MapsActivity.class);
        startActivity(intent);
    }

    private void startRoutesListActivity() {
        // Start an activity for viewing previous rides
        Intent intent = new Intent(MainMenuActivity.this, RoutesListActivity.class);
        startActivity(intent);
    }
}

