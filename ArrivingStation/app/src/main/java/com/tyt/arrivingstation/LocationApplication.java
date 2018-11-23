package com.tyt.arrivingstation;

import android.app.Application;
import android.app.Service;
import android.os.Vibrator;

import com.tyt.arrivingstation.service.LocationService;

/**
 * Created by TYT on 2018/10/17.
 **/

public class LocationApplication extends Application {
    public LocationService locationService;
    public Vibrator mVibrator;
    @Override
    public void onCreate() {
        super.onCreate();
        locationService = new LocationService(getApplicationContext());
        mVibrator =(Vibrator)getApplicationContext().getSystemService(Service.VIBRATOR_SERVICE);
//        SDKInitializer.initialize(getApplicationContext());
    }
}
