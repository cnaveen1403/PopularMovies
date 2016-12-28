package com.android.naveen.popularmovies.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.android.naveen.popularmovies.data.MoviesContract.MoviesEntry;
import com.android.naveen.popularmovies.data.MoviesContract.ReviewsEntry;
import com.android.naveen.popularmovies.data.MoviesContract.TrailersEntry;

public class MoviesProvider extends ContentProvider {
    protected static final int MOVIES = 100;
    protected static final int MOVIE_ITEM = 101;
    protected static final int MOVIE_DETAILS = 103;
    protected static final int TRAILERS = 200;
    protected static final int TRAILER_ITEM = 201;
    protected static final int REVIEWS = 300;
    protected static final int REVIEW_ITEM = 301;

    protected MoviesDbHelper mOpenHelper;
    protected UriMatcher mUriMatcher;

    @Override
    public boolean onCreate() {
        mOpenHelper = new MoviesDbHelper(getContext());
        mUriMatcher = buildUriMatcher();
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor retCursor;
        switch (mUriMatcher.match(uri)) {
            case MOVIES:
                retCursor = mOpenHelper.getReadableDatabase().query(MoviesEntry.TABLE_NAME,
                        projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case TRAILERS:
                retCursor = mOpenHelper.getReadableDatabase().query(TrailersEntry.TABLE_NAME,
                        projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case REVIEWS:
                retCursor = mOpenHelper.getReadableDatabase().query(ReviewsEntry.TABLE_NAME,
                        projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case MOVIE_DETAILS:
                long movieId = ContentUris.parseId(uri);
                String where = "(" + MoviesEntry.COLUMN_MOVIE_ID + " = '" + movieId + "')";
                String[] columns = new String[]{"*", "'" + MoviesEntry.TABLE_NAME + "' as " + MoviesContract.MOVIE_DETAIL_TABLE};
                Cursor[] cursors = new Cursor[3];

                cursors[0] = mOpenHelper.getReadableDatabase().query(MoviesEntry.TABLE_NAME,
                        columns, where, selectionArgs, null, null, sortOrder);
                cursors[0].setNotificationUri(getContext().getContentResolver(), MoviesEntry.CONTENT_URI);

                columns[1] = "'" + TrailersEntry.TABLE_NAME + "' as " + MoviesContract.MOVIE_DETAIL_TABLE;
                cursors[1] = mOpenHelper.getReadableDatabase().query(TrailersEntry.TABLE_NAME,
                        columns, where, selectionArgs, null, null, sortOrder);
                cursors[1].setNotificationUri(getContext().getContentResolver(), TrailersEntry.CONTENT_URI);

                columns[1] = "'" + ReviewsEntry.TABLE_NAME + "' as " + MoviesContract.MOVIE_DETAIL_TABLE;
                cursors[2] = mOpenHelper.getReadableDatabase().query(ReviewsEntry.TABLE_NAME,
                        columns, where, selectionArgs, null, null, sortOrder);
                cursors[2].setNotificationUri(getContext().getContentResolver(), ReviewsEntry.CONTENT_URI);

                retCursor = new MergeCursor(cursors);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = mUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case MOVIES:
                long movieId = db.insert(MoviesEntry.TABLE_NAME, null, values);
                if (movieId > 0) {
                    returnUri = MoviesEntry.buildMovieUri(movieId);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            case TRAILERS:
                long trailerId = db.insert(TrailersEntry.TABLE_NAME, null, values);
                if (trailerId > 0) {
                    returnUri = TrailersEntry.buildTrailerUri(trailerId);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            case REVIEWS:
                long reviewId = db.insert(ReviewsEntry.TABLE_NAME, null, values);
                if (reviewId > 0) {
                    returnUri = ReviewsEntry.buildReviewsUri(reviewId);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = mUriMatcher.match(uri);
        int rowsDeleted;
        if (null == selection) selection = "1";
        switch (match) {
            case MOVIES:
                rowsDeleted = db.delete(MoviesEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case TRAILERS:
                rowsDeleted = db.delete(TrailersEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case REVIEWS:
                rowsDeleted = db.delete(ReviewsEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = mUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case MOVIES:
                rowsUpdated = db.update(MoviesEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case TRAILERS:
                rowsUpdated = db.update(TrailersEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case REVIEWS:
                rowsUpdated = db.update(ReviewsEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        int result = 0;
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = mUriMatcher.match(uri);
        switch (match) {
            case MOVIES:
                db.beginTransaction();
                int returnMoviesCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insertWithOnConflict(MoviesEntry.TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_IGNORE);
                        if (_id != -1) {
                            returnMoviesCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                result = returnMoviesCount;
                break;
            case TRAILERS:
                db.beginTransaction();
                int returnTrailersCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insertWithOnConflict(TrailersEntry.TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_IGNORE);
                        if (_id != -1) {
                            returnTrailersCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                result = returnTrailersCount;
                break;
            case REVIEWS:
                db.beginTransaction();
                int returnReviewsCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insertWithOnConflict(ReviewsEntry.TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_IGNORE);
                        if (_id != -1) {
                            returnReviewsCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                result = returnReviewsCount;
                break;
            default:
                result = super.bulkInsert(uri, values);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return result;
    }

    @Override
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case MOVIE_ITEM:
                return MoviesEntry.CONTENT_ITEM_TYPE;
            case MOVIES:
                return MoviesEntry.CONTENT_TYPE;
            case TRAILER_ITEM:
                return TrailersEntry.CONTENT_ITEM_TYPE;
            case TRAILERS:
                return TrailersEntry.CONTENT_TYPE;
            case REVIEW_ITEM:
                return ReviewsEntry.CONTENT_ITEM_TYPE;
            case REVIEWS:
                return ReviewsEntry.CONTENT_TYPE;
            case MOVIE_DETAILS:
                return MoviesEntry.CONTENT_DETAILS_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    protected UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = MoviesContract.CONTENT_AUTHORITY;

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