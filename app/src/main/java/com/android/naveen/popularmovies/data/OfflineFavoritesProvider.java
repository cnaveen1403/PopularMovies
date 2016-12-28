package com.android.naveen.popularmovies.data;

import android.content.UriMatcher;

public class OfflineFavoritesProvider extends MoviesProvider {

    @Override
    public boolean onCreate() {
        mOpenHelper = new OfflineFavoritesDbHelper(getContext());
        mUriMatcher = buildUriMatcher();
        return true;
    }

    @Override
    protected UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = MoviesContract.FAVORITES_CONTENT_AUTHORITY;

        matcher.addURI(authority, MoviesContract.PATH_MOVIES, MOVIES);
        matcher.addURI(authority, MoviesContract.PATH_MOVIES + "/#", MOVIE_ITEM);
        matcher.addURI(authority, MoviesContract.PATH_MOVIES + "/" + MoviesContract.PATH_MOVIE_DETAILS + "/#", MOVIE_DETAILS);

        matcher.addURI(authority, MoviesContract.PATH_TRAILERS, TRAILERS);
        matcher.addURI(authority, MoviesContract.PATH_TRAILERS + "/#", TRAILER_ITEM);

        matcher.addURI(authority, MoviesContract.PATH_REVIEWS, REVIEWS);
        matcher.addURI(authority, MoviesContract.PATH_REVIEWS + "/#", REVIEW_ITEM);

        return matcher;
    }
}