package com.example.finishble;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

public class AudioPlaybackService extends Service {
    private MediaPlayer mediaPlayer;
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        AudioPlaybackService getService() {
            return AudioPlaybackService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = MediaPlayer.create(this, R.raw.calming_music);
        mediaPlayer.setLooping(true); // Loop the music
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void play() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void pause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, createNotification());
        return START_STICKY;
    }

    private Notification createNotification() {
        // Create a notification to keep the service running in the foreground
        Intent notificationIntent = new Intent(this, MusicActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("Music Player")
                .setContentText("Playing Music")
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(pendingIntent)
                .build();

        return notification;
    }

    @Override
    public void onDestroy() {
        mediaPlayer.stop();
        mediaPlayer.release();
    }
}
