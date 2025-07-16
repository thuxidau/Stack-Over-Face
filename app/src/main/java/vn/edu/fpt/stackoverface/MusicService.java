package vn.edu.fpt.stackoverface;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

import androidx.preference.PreferenceManager;

// Manage music
public class MusicService extends Service {

    private MediaPlayer mediaPlayer; // Player object used for music playback
    private int lastPosition = 0; // Remember where playback left off

    // Initialize the MediaPlayer with a background music file.
    // Set it to loop forever
    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = MediaPlayer.create(this, R.raw.background_music);
        mediaPlayer.setLooping(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Read user preference (music_enabled) to check if music should be played
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean musicEnabled = prefs.getBoolean("music_enabled", true);

        // Intent handling
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "PAUSE":
                    pauseMusic();
                    break;
                case "RESUME":
                    // Only if enabled
                    if (musicEnabled) {
                        resumeMusic();
                    }
                    break;
                case "STOP_IF_DISABLED":
                    stopMusicIfDisabled(); // stop if still playing
                    break;
            }
        }

        return START_STICKY; // Restart the service if it gets killed
    }

    public void stopMusicIfDisabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean musicEnabled = prefs.getBoolean("music_enabled", true);

        // If music is disabled, and it's currently playing, pause and store current position
        if (!musicEnabled && mediaPlayer != null && mediaPlayer.isPlaying()) {
            lastPosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
        }
    }

    @Override
    public void onDestroy() {
        // Clean up when the service is destroyed
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

    public void pauseMusic() {
        // Save the current playback time and pauses
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            lastPosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
        }
    }

    public void resumeMusic() {
        // Start playing from the last saved position
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(lastPosition);
            mediaPlayer.start();
        }
    }

    // Enable activities to bind to this service and call its methods
    @Override
    public IBinder onBind(Intent intent) {
        return new MusicBinder();
    }

    // Inner binder class lets the activity retrieve the actual MusicService instance
    // and call resumeMusic() or pauseMusic() directly
    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }
}