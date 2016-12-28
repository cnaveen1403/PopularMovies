package com.android.naveen.popularmovies.data;

import android.content.Context;

public class OfflineFavoritesDbHelper extends MoviesDbHelper {
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "offline_movies.db";

    public OfflineFavoritesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

}