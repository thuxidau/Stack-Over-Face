package vn.edu.fpt.stackoverface;

/*
    Listens to the entire app's foreground/background state
    Controls background music by sending intent actions to the `MusicService` when:
        - The app comes to the foreground → resume music
        - The app goes to the background → pause music
*/

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

// Observe the app's process-wide lifecycle
// Register it in `SOFApp` class
public class AppLifecycleObserver implements DefaultLifecycleObserver {

    // Stores the application context so we can safely start a service from anywhere
    private final Context context;

    // Receives the context from the `SOFApp` class
    // Makes sure to use the application context (not tied to any activity).
    public AppLifecycleObserver(Context context) {
        this.context = context.getApplicationContext();
    }

    // Runs when the app comes to the foreground
    // Sends a "RESUME" action to the MusicService to continue music
    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction("RESUME");
        context.startService(intent);
    }

    // Runs when the entire app goes to the background
    // It sends a "PAUSE" action to the MusicService to stop or pause music
    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction("PAUSE");
        context.startService(intent);
    }
}