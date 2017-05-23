package ru.com.videopanel;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

import io.realm.Realm;
import ru.com.videopanel.db.DBHelper;

/**
 * Main App class. Need to initialize app related libs
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Realm.init(this);
        //TODO remove after tests
        DBHelper.clearDB();

        //Leak analyzer setup
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(this);

    }
}
