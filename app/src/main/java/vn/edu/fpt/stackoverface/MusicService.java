package vn.edu.fpt.stackoverface;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

import androidx.preference.PreferenceManager;

public class MusicService extends Service {

    private MediaPlayer mediaPlayer;
    private int lastPosition = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = MediaPlayer.create(this, R.raw.background_music);
        mediaPlayer.setLooping(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean musicEnabled = prefs.getBoolean("music_enabled", true);

        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "PAUSE":
                    pauseMusic();
                    break;
                case "RESUME":
                    if (musicEnabled) {
                        resumeMusic();
                    }
                    break;
                case "STOP_IF_DISABLED":
                    stopMusicIfDisabled(); // stop if still playing
                    break;
            }
        }

        return START_STICKY;
    }

    public void stopMusicIfDisabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean musicEnabled = prefs.getBoolean("music_enabled", true);

        if (!musicEnabled && mediaPlayer != null && mediaPlayer.isPlaying()) {
            lastPosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
        }
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

    public void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            lastPosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
        }
    }

    public void resumeMusic() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(lastPosition);
            mediaPlayer.start();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MusicBinder();
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }
}