package com.android.naveen.popularmovies.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ThemoviedbAuthenticatorService extends Service {

    private ThemoviedbAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        mAuthenticator = new ThemoviedbAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
