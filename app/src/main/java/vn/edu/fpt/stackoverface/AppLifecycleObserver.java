package vn.edu.fpt.stackoverface;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public class AppLifecycleObserver implements DefaultLifecycleObserver {

    private final Context context;

    public AppLifecycleObserver(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction("RESUME");
        context.startService(intent);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction("PAUSE");
        context.startService(intent);
    }
}