package com.example.finishble;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MusicActivity extends AppCompatActivity {

    private MusicService musicService;
    private boolean isMusicServiceBound = false;

    private ServiceConnection musicServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            isMusicServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isMusicServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stats);

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, musicServiceConnection, Context.BIND_AUTO_CREATE);

        setupButtonListeners();
    }
    private void setupButtonListeners() {
        Button playCalmingMusicButton = findViewById(R.id.play_calming_music_button);
        playCalmingMusicButton.setOnClickListener(v -> {
            if (isMusicServiceBound) {
                if (musicService.isPlaying()) {
                    musicService.stopMusic(); // Stop if music is currently playing
                } else {
                    Intent serviceIntent = new Intent(this, MusicService.class);
                    serviceIntent.putExtra("MUSIC_RESOURCE_ID", R.raw.calming_music);
                    serviceIntent.putExtra("MUSIC_GENRE", "Calming");
                    startService(serviceIntent); // Start service with specific music resource
                }
            }
        });
        Button playHardRockMusicButton = findViewById(R.id.play_hard_rock_button);
        playHardRockMusicButton.setOnClickListener(v -> {
            if (isMusicServiceBound) {
                if (musicService.isPlaying()) {
                    musicService.stopMusic(); // Stop if music is currently playing
                } else {
                    Intent serviceIntent = new Intent(this, MusicService.class);
                    serviceIntent.putExtra("MUSIC_RESOURCE_ID", R.raw.hard_rock_music);
                    serviceIntent.putExtra("MUSIC_GENRE", "Horror");
                    startService(serviceIntent); // Start service with specific music resource
                }
            }
        });
        Button playClassicalMusicButton = findViewById(R.id.play_classical_button);
        playClassicalMusicButton.setOnClickListener(v -> {
            if (isMusicServiceBound) {
                if (musicService.isPlaying()) {
                    musicService.stopMusic(); // Stop if music is currently playing
                } else {
                    Intent serviceIntent = new Intent(this, MusicService.class);
                    serviceIntent.putExtra("MUSIC_RESOURCE_ID", R.raw.classical_music);
                    serviceIntent.putExtra("MUSIC_GENRE", "Classical");
                    startService(serviceIntent); // Start service with specific music resource
                }
            }
        });

        Button playJazzMusicButton = findViewById(R.id.play_jazz_button);
        playJazzMusicButton.setOnClickListener(v -> {
            if (isMusicServiceBound) {
                if (musicService.isPlaying()) {
                    musicService.stopMusic(); // Stop if music is currently playing
                } else {
                    Intent serviceIntent = new Intent(this, MusicService.class);
                    serviceIntent.putExtra("MUSIC_RESOURCE_ID", R.raw.jazz_music);
                    serviceIntent.putExtra("MUSIC_GENRE", "Jazz");
                    startService(serviceIntent); // Start service with specific music resource
                }
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isMusicServiceBound) {
            unbindService(musicServiceConnection);
            isMusicServiceBound = false;
        }
    }
}
