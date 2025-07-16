package vn.edu.fpt.stackoverface;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public abstract class MusicBoundActivity extends AppCompatActivity {

    protected MusicService musicService; // Reference to the actual service once bound
    private boolean isBound = false; // Track whether the activity is currently connected to the service

    // Handles the service binding lifecycle
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // Call when the service is successfully connected
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;

            // Checks user preference (music_enabled) and resumes music if allowed
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MusicBoundActivity.this);
            if (prefs.getBoolean("music_enabled", true)) {
                musicService.resumeMusic(); // only resume if user wants music
            }
        }

        // Trigger if the service crashes or disconnects unexpectedly
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        // Binds the activity to the MusicService when it becomes visible
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE); // Start the service if it isn’t already running
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Music resumes only if the setting is enabled
        if (musicService != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if (prefs.getBoolean("music_enabled", true)) {
                musicService.resumeMusic(); // only when setting is true
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service when the activity is no longer visible
        if (isBound && musicService != null) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}