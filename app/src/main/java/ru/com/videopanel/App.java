package ru.com.videopanel;

import android.app.Application;
import android.content.Context;

import io.realm.Realm;

/**
 * Main App class. Need to initialize app related libs
 */
public class App extends Application {

    private static Context context;

    public static Context getAppContext() {
        return App.context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        App.context = getApplicationContext();

        Realm.init(this);
        //Leak analyzer setup
//        if (LeakCanary.isInAnalyzerProcess(this)) {
//            return;
//        }
//        LeakCanary.install(this);
//TODO turn on
//        Fabric.with(this, new Crashlytics());

    }
}
