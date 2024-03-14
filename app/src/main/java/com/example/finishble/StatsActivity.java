package com.example.finishble;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class StatsActivity extends AppCompatActivity {

    private List<Float> bluetoothSensorData = new ArrayList<>();
    private TextView avgBluetoothSensorTextView;

    // Declare MediaPlayer instances for the two audio files
    private MediaPlayer calmingMediaPlayer;
    private MediaPlayer hardRockMediaPlayer;

    private MediaPlayer classicalMediaPlayer;
    private MediaPlayer jazzMediaPlayer;
    private AudioPlaybackService audioService;
    private boolean isServiceBound = false;

    // Track the playing state for each genre
    private boolean isCalmingMusicPlaying = false;
    private boolean isHardRockMusicPlaying = false;
    private boolean isClassicalMusicPlaying = false;
    private boolean isJazzMusicPlaying = false;


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioPlaybackService.LocalBinder binder = (AudioPlaybackService.LocalBinder) service;
            audioService = binder.getService();
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stats);

//        avgBluetoothSensorTextView = findViewById(R.id.avg_bluetooth_sensor);


        // Initialize MediaPlayer instances for the audio files
        calmingMediaPlayer = MediaPlayer.create(this, R.raw.calming_music);
        hardRockMediaPlayer = MediaPlayer.create(this, R.raw.hard_rock_music);
        classicalMediaPlayer = MediaPlayer.create(this, R.raw.classical_music);
        jazzMediaPlayer = MediaPlayer.create(this, R.raw.jazz_music);

        // Start the audio service
        Intent audioIntent = new Intent(this, AudioPlaybackService.class);
        bindService(audioIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(audioIntent);


        // Your button listeners
        Button playCalmingMusicButton = findViewById(R.id.play_calming_music_button);
        playCalmingMusicButton.setOnClickListener(v -> togglePlayback(calmingMediaPlayer, isCalmingMusicPlaying));

        Button playHardRockMusicButton = findViewById(R.id.play_hard_rock_button);
        playHardRockMusicButton.setOnClickListener(v -> togglePlayback(hardRockMediaPlayer, isHardRockMusicPlaying));

        Button playClassicalMusicButton = findViewById(R.id.play_classical_button);
        playClassicalMusicButton.setOnClickListener(v -> togglePlayback(classicalMediaPlayer, isClassicalMusicPlaying));

        Button playJazzMusicButton = findViewById(R.id.play_jazz_button);
        playJazzMusicButton.setOnClickListener(v -> togglePlayback(jazzMediaPlayer, isJazzMusicPlaying));

        // In StatsActivity.onCreate method after findViewById
     //   Button refreshButton = findViewById(R.id.refresh_button);
       // refreshButton.setOnClickListener(v -> {
            // Recalculate and refresh the average data here
          //  float newAverage = calculateAverage(sensorDataList);
            //updateAverageUI(newAverage);
       // });


// Add more listeners for other genres

        // Assuming you've set up communication with MyBleManager, receive Bluetooth data here.
        // Register a receiver or implement the callback mechanism to receive data.

        // Example: Register a BroadcastReceiver to receive Bluetooth data
        // In StatsActivity.onCreate method
// Register a BroadcastReceiver to receive sensor data
        BroadcastReceiver sensorDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("SENSOR_DATA_RECEIVED".equals(intent.getAction())) {
                    float sensorData = intent.getFloatExtra("SENSOR_DATA", 0.0f); // Default value if data is not present
                    // Handle the received sensor data here
                    addSensorData(sensorData); // Add data to the list and update UI
                }
            }
        };

        IntentFilter filter = new IntentFilter("SENSOR_DATA_RECEIVED"); // Use the same action string
        registerReceiver(sensorDataReceiver, filter);

    }

    private void togglePlayback(MediaPlayer mediaPlayer, boolean isPlaying) {
        if (isPlaying) {
            mediaPlayer.pause();
        } else {
            // Stop any other playing genres
            if (mediaPlayer != calmingMediaPlayer && calmingMediaPlayer.isPlaying()) {
                calmingMediaPlayer.pause();
                isCalmingMusicPlaying = false;
            }
            if (mediaPlayer != hardRockMediaPlayer && hardRockMediaPlayer.isPlaying()) {
                hardRockMediaPlayer.pause();
                isHardRockMusicPlaying = false;
            }
            if (mediaPlayer != classicalMediaPlayer && classicalMediaPlayer.isPlaying()) {
                classicalMediaPlayer.pause();
                isClassicalMusicPlaying = false;
            }
            if (mediaPlayer != jazzMediaPlayer && jazzMediaPlayer.isPlaying()) {
                jazzMediaPlayer.pause();
                isJazzMusicPlaying = false;
            }

            mediaPlayer.start();
        }
        updateGenreFlags(mediaPlayer);
    }
    private void updateGenreFlags(MediaPlayer currentGenrePlayer) {
        isCalmingMusicPlaying = (currentGenrePlayer == calmingMediaPlayer);
        isHardRockMusicPlaying = (currentGenrePlayer == hardRockMediaPlayer);
        isClassicalMusicPlaying = (currentGenrePlayer == classicalMediaPlayer);
        isJazzMusicPlaying = (currentGenrePlayer == jazzMediaPlayer);
    }

    private List<Float> sensorDataList = new ArrayList<>();
    private static final int MAX_DATA_POINTS = 100; // Adjust this based on your desired time window

    // Add received sensor data to the list
    private void addSensorData(float sensorData) {
        sensorDataList.add(sensorData);

        // Ensure the list doesn't exceed the maximum data points
        if (sensorDataList.size() > MAX_DATA_POINTS) {
            sensorDataList.remove(0);
        }

        // Calculate the average
        float average = calculateAverage(sensorDataList);

        // Debugging: Print the contents of data list and the calculated average
        Log.d("StatsActivity", "Sensor Data List: " + sensorDataList.toString());
        Log.d("StatsActivity", "Calculated Average: " + average);

        // Update UI with the average value
        //updateAverageUI(average);
    }

    // Calculate the average of a list of data points
    private float calculateAverage(List<Float> data) {
        if (data.isEmpty()) {
            return 0.0f; // Default to 0 if there's no data
        }

        float sum = 0.0f;
        for (Float value : data) {
            sum += value;
        }

        // Debugging: Print the contents of data and the calculated sum
        Log.d("StatsActivity", "Data List: " + data.toString());
        Log.d("StatsActivity", "Sum: " + sum);

        float average = sum / data.size();

        // Debugging: Print the calculated average
        Log.d("StatsActivity", "Average: " + average);

        return average;
    }


    // Be sure to release the MediaPlayer instances when they are no longer needed
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unbind the audio service
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    @Override
    public void onBackPressed() {
        // Instead of finishing the activity, navigate back to the main activity.
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class); // Replace MainActivity with the name of your main activity.
        startActivity(intent);
        finish();
    }
    // Update UI with the calculated average value
  //  private void updateAverageUI(float average) {
  //      avgBluetoothSensorTextView.setText("Average Bluetooth Sensor Data: " + average);
  //  }
}
