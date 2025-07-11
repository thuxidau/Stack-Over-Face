package vn.edu.fpt.stackoverface;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.appcompat.app.AppCompatActivity;

public abstract class MusicBoundActivity extends AppCompatActivity {

    protected MusicService musicService;
    private boolean isBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            musicService.resumeMusic(); // resume immediately when connected
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
            musicService.resumeMusic();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound && musicService != null) {
//            musicService.pauseMusic();
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}