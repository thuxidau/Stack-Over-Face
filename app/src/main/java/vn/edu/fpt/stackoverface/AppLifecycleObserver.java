package vn.edu.fpt.stackoverface;

import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

public class AppLifecycleObserver implements LifecycleObserver {

    private final Context context;

    public AppLifecycleObserver(Context context) {
        this.context = context.getApplicationContext();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        // App goes to background
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction("PAUSE");
        context.startService(intent);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onAppForegrounded() {
        // App comes to foreground
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction("RESUME");
        context.startService(intent);
    }
}