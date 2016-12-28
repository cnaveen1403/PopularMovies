package com.android.naveen.popularmovies.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.android.naveen.popularmovies.BuildConfig;
import com.android.naveen.popularmovies.R;
import com.android.naveen.popularmovies.Utility;
import com.android.naveen.popularmovies.data.MoviesContract.MoviesEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SyncAdapterMovies extends AbstractThreadedSyncAdapter {
    private static final String LOG_TAG = SyncAdapterMovies.class.getSimpleName();

    public static final String NEXT_PAGE = "nextpage";
    public static final String SORT_TYPE = "sorttype";
    public static final String SORT_BY_POPULAR = "popular";
    public static final String SORT_BY_RATING = "top_rated";
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;
    public static final String MOVIE_ID = "movieid";
    private final SyncAdapterTrailersDelegate syncAdapterTrailersDelegate;
    private final SyncAdapterReviewsDelegate syncAdapterReviewsDelegate;

    private int mTotalPages = 1;
    private int mLastPage = 0;
    private String mCurrentSortType = SORT_BY_POPULAR;

    public SyncAdapterMovies(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        syncAdapterTrailersDelegate = new SyncAdapterTrailersDelegate(context);
        syncAdapterReviewsDelegate = new SyncAdapterReviewsDelegate(context);
    }

    public SyncAdapterMovies(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        syncAdapterTrailersDelegate = new SyncAdapterTrailersDelegate(context);
        syncAdapterReviewsDelegate = new SyncAdapterReviewsDelegate(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "onPerformSync");

        if (extras.containsKey(MOVIE_ID)) {
            syncAdapterTrailersDelegate.onPerformSync(account, extras, authority, provider, syncResult);
            syncAdapterReviewsDelegate.onPerformSync(account, extras, authority, provider, syncResult);
        } else {
            String prevSortType = mCurrentSortType;
            mCurrentSortType = extras.getString(SORT_TYPE, mCurrentSortType);
            if (!prevSortType.equals(mCurrentSortType)) {
                getContext().getContentResolver().delete(MoviesEntry.CONTENT_URI, null, null);
                mTotalPages = 1;
                mLastPage = 0;
            }
            if (Utility.isNetworkConnected(getContext())) {
                if (extras.getBoolean(NEXT_PAGE) && mLastPage < mTotalPages) {
                    if (retrieveThemoviedbData(mCurrentSortType, mLastPage + 1)) {
                        mLastPage++;
                    }
                } else {
                    retrieveThemoviedbData(mCurrentSortType, 1);
                    mLastPage = 1;
                }
            }
        }
    }

    private boolean retrieveThemoviedbData(String sortType, int pageNumber) {
        Log.d(LOG_TAG, "getThemoviedbData page[" + pageNumber + "]");

        boolean result = false;

        HttpURLConnection urlConnection = null;
        JsonReader reader = null;
        try {
            final String API_KEY = "api_key";
            final String PAGE = "page";
            Uri builtUri = Uri.parse(getContext().getString(R.string.tmdb_popular_url)).buildUpon()
                    .appendPath(sortType)
                    .appendQueryParameter(API_KEY, BuildConfig.TMDB_API_KEY)
                    .appendQueryParameter(PAGE, Integer.toString(pageNumber))
                    .build();

            URL url = new URL(builtUri.toString());
Log.d(LOG_TAG, "URL: " + url.toString());
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));

            parseResultValues(reader);

            result = true;

        } catch (Exception e) {
            Log.e(LOG_TAG, "Error ", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return result;
    }

    private void parseResultValues(JsonReader reader) throws IOException {
        while (reader.hasNext()) {
            List<ContentValues> moviesList = new ArrayList();

            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("results") && reader.peek() != JsonToken.NULL) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        moviesList.add(parseContentValues(reader));
                    }
                    reader.endArray();
                } else if (name.equals("total_pages")) {
                    mTotalPages = reader.nextInt();
                } else {
                    reader.skipValue();
                }
            }
            int inserted = insertDataIntoContentProvider(moviesList);
            Log.d(LOG_TAG, "Inserted [" + inserted + "] movies");
        }
    }

    private int insertDataIntoContentProvider(List<ContentValues> moviesList) {
        int result = 0;
        if (moviesList.size() > 0) {
            result = getContext().getContentResolver().bulkInsert(
                    MoviesEntry.CONTENT_URI,
                    moviesList.toArray(new ContentValues[moviesList.size()]));
        }
        return result;
    }

    @NonNull
    private ContentValues parseContentValues(JsonReader reader) throws IOException {
        reader.beginObject();

        ContentValues movieValues = new ContentValues();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("id")) {
                movieValues.put(MoviesEntry.COLUMN_MOVIE_ID, reader.nextLong());
            } else if (name.equals("original_title")) {
                movieValues.put(MoviesEntry.COLUMN_ORIGINAL_TITLE, reader.nextString());
            } else if (name.equals("overview") && reader.peek() != JsonToken.NULL) {
                movieValues.put(MoviesEntry.COLUMN_OVERVIEW, reader.nextString());
            } else if (name.equals("release_date") && reader.peek() != JsonToken.NULL) {
                movieValues.put(MoviesEntry.COLUMN_RELEASE_DATE, reader.nextString());
            } else if (name.equals("poster_path") && reader.peek() != JsonToken.NULL) {
                movieValues.put(MoviesEntry.COLUMN_POSTER_PATH, reader.nextString());
            } else if (name.equals("popularity")) {
                movieValues.put(MoviesEntry.COLUMN_POPULARITY, reader.nextDouble());
            } else if (name.equals("vote_average")) {
                movieValues.put(MoviesEntry.COLUMN_VOTE_AVERAGE, reader.nextDouble());
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return movieValues;
    }

    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context, null);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account, authority, new Bundle(), syncInterval);
        }
    }

    public static void syncMoviesListImmediately(Context context, String sortType) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        if (sortType != null) {
            bundle.putString(SORT_TYPE, sortType);
        }
        ContentResolver.requestSync(getSyncAccount(context, sortType),
                context.getString(R.string.content_authority), bundle);
    }

    public static void syncMovieDetailsImmediately(Context context, String movieId) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        if (movieId != null) {
            bundle.putString(MOVIE_ID, movieId);
        }
        ContentResolver.requestSync(getSyncAccount(context, movieId),
                context.getString(R.string.content_authority), bundle);
    }

    public static Account getSyncAccount(Context context, String sortType) {
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        Account newAccount = new Account(context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        if (null == accountManager.getPassword(newAccount)) {
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            onAccountCreated(newAccount, context, sortType);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context, String sortType) {
        SyncAdapterMovies.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        syncMoviesListImmediately(context, sortType);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context, null);
    }

    public static void loadNextPageOnScroll(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putBoolean(SyncAdapterMovies.NEXT_PAGE, true);
        ContentResolver.requestSync(getSyncAccount(context, null), context.getString(R.string.content_authority), bundle);
    }
}