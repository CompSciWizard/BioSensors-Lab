package com.example.finishble;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

public class MusicService extends Service {
    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;

    public class LocalBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }
    public void stopMusic() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            // Reset the current music genre to "None" or similar
            DataCollectorCSV.setCurrentMusicGenre("None");
        }
    }
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // This method prepares MediaPlayer with a music resource.
    public void initMediaPlayer(int musicResId) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(this, musicResId);
        mediaPlayer.setLooping(true);
    }

    // Call this method after initMediaPlayer to start playback.
    public void playMusic(int resId, String musicGenre) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(this, resId);
        mediaPlayer.setLooping(true);
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }

        // Update DataCollectorCSV with the current music genre
        DataCollectorCSV.setCurrentMusicGenre(musicGenre);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("MUSIC_RESOURCE_ID")) {
            int musicResId = intent.getIntExtra("MUSIC_RESOURCE_ID", 0);
            String musicGenre = intent.getStringExtra("MUSIC_GENRE"); // Assuming you pass the genre as a string extra

            if (musicResId != 0) {
                initMediaPlayer(musicResId);
                playMusic(musicResId, musicGenre); // Updated to include genre
            }
        }
        return START_STICKY; // Return START_STICKY to ensure service continues running until explicitly stopped
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}
