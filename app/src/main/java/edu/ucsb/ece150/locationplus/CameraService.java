package edu.ucsb.ece150.locationplus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
public class CameraService extends Service {

    private static final String TAG = "CameraService";
    private static final String CHANNEL_ID = "CameraServiceChannel";
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;

    private String videoPath;



    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: entered");
        createNotificationChannel();
        mediaRecorder = new MediaRecorder();
//        initMediaRecorder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: entered");
        startRecording();

        Notification notification = new NotificationCompat.Builder(this, "CameraServiceChannel")
                .setContentTitle("Camera Service")
                .setContentText("Recording")
                .setSmallIcon(R.drawable.greenbike)
                .build();
        startForeground(2, notification);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: entered");
        stopRecording();
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private String getOutputMediaFilePath() throws IOException {
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "BikeBuddy");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                throw new IOException("Failed to create directory");
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
        return mediaFile.getAbsolutePath();
    }

    public void startRecording() {
        if (isRecording) {
            stopRecording();
        } else {

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)); // Set default settings

            try {
                videoPath = getOutputMediaFilePath();
                mediaRecorder.setOutputFile(videoPath);
                mediaRecorder.prepare();
                mediaRecorder.start();
                isRecording = true;
                Log.d(TAG, "Recording started");
            } catch (Exception e) {
                Log.e(TAG, "Error starting recording: " + e.getMessage());
                return;
            }
        }
    }

    public void stopRecording() {
        if (isRecording) {
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
        }
        SharedPreferences prefs = getSharedPreferences("BikeBuddyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("videoPath", videoPath);
        editor.commit();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Camera Service Channel";
            String description = "Channel for Camera Service";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}