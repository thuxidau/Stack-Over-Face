package vn.edu.fpt.stackoverface;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MusicService extends Service {

    private MediaPlayer mediaPlayer;
    private int lastPosition = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MusicService", "onCreate – starting music");
        mediaPlayer = MediaPlayer.create(this, R.raw.background_music);
        mediaPlayer.setLooping(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "PAUSE":
                    pauseMusic();
                    break;
                case "RESUME":
                    resumeMusic();
                    break;
            }
        } else if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(lastPosition);
            mediaPlayer.start();
        }

        return START_STICKY;
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
            Log.d("MusicService", "pauseMusic – " + mediaPlayer.isPlaying());
            lastPosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
        }
    }

    public void resumeMusic() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            Log.d("MusicService", "resumeMusic – " + mediaPlayer.isPlaying());
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