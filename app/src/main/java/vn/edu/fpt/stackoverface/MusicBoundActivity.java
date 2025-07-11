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

    protected MusicService musicService;
    private boolean isBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MusicBoundActivity.this);
            if (prefs.getBoolean("music_enabled", true)) {
                musicService.resumeMusic(); // only resume if user wants music
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        if (isBound && musicService != null) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}