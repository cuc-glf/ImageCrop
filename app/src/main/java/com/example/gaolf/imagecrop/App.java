package com.example.gaolf.imagecrop;

import android.app.Application;

/**
 * Created by gaolf on 15/12/24.
 */
public class App extends Application {
    private static App sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    public static App getInstance() {
        return sInstance;
    }
}
