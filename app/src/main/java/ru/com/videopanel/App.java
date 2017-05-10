package ru.com.videopanel;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * Main App class
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //Leak analyzer setup
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(this);

    }
}
