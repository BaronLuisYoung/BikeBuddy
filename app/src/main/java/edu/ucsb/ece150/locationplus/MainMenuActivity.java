package edu.ucsb.ece150.locationplus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainMenuActivity extends AppCompatActivity {

    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        initializeVideoView();
        initializeStartRideButton();
        initializeViewPreviousRidesButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        Log.d("MainMenuActivity", "onDestroy: entered");
        SharedPreferences sharedPref = getSharedPreferences("BikeBuddyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("drawRoute", false);
        editor.apply();
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
                //startMapsActivity();
                startRoutesListActivity();
            }
        });
    }

    private void initializeVideoView() {
        TextureView textureView = findViewById(R.id.textureView);
        textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                playVideo(surface);
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                // This is called when the size of the TextureView changes.
                // Implement this if you need to handle size changes.
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                // Return true to indicate that the SurfaceTexture is being released.
                // Implement cleanup if necessary.
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                // This is called when the SurfaceTexture is updated.
                // Implement this if you have specific actions to take when this happens.
            }
        });
    }

    private void playVideo(SurfaceTexture surfaceTexture) {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(MainMenuActivity.this, Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.output));
            mediaPlayer.setSurface(new Surface(surfaceTexture));
            mediaPlayer.setLooping(true);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void startMapsActivity() {
        // Start the MapsActivity
        Intent intent = new Intent(MainMenuActivity.this, MapsActivity.class);
        startActivity(intent);
    }

    private void startRoutesListActivity() {
        // Start an activity for viewing previous rides
        Intent intent = new Intent(MainMenuActivity.this, RoutesListActivity.class);
        intent.putExtra("goToMapsActivity", true);
        startActivityForResult(intent, 2);
    }
}
