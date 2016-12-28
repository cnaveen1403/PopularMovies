package com.android.naveen.popularmovies.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ThemoviedbSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static SyncAdapterMovies sSyncAdapterMovies = null;

    @Override
    public void onCreate() {
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapterMovies == null) {
                sSyncAdapterMovies = new SyncAdapterMovies(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapterMovies.getSyncAdapterBinder();
    }
}
