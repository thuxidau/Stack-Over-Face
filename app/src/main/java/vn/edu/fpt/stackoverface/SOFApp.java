package vn.edu.fpt.stackoverface;

import android.app.Application;

import androidx.lifecycle.ProcessLifecycleOwner;

public class SOFApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(
                new AppLifecycleObserver(this)
        );
    }
}