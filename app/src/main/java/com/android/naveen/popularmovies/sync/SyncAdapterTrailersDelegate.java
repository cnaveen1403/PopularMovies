package com.android.naveen.popularmovies.sync;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.android.naveen.popularmovies.BuildConfig;
import com.android.naveen.popularmovies.R;
import com.android.naveen.popularmovies.Utility;
import com.android.naveen.popularmovies.data.MoviesContract.TrailersEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class SyncAdapterTrailersDelegate {
    private static final String LOG_TAG = SyncAdapterTrailersDelegate.class.getSimpleName();
    private Context mContext;

    public SyncAdapterTrailersDelegate(Context context) {
        this.mContext = context;
    }

    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "onPerformSync");
        String movieId = extras.getString(SyncAdapterMovies.MOVIE_ID);
        if (movieId != null && !movieId.trim().isEmpty() && Utility.isNetworkConnected(mContext)) {
            retrieveTrailerData(movieId);
        }
    }

    private boolean retrieveTrailerData(String movieId) {
        Log.d(LOG_TAG, "retrieveTrailerData movieId[" + movieId + "]");

        boolean result = false;

        HttpURLConnection urlConnection = null;
        JsonReader reader = null;
        try {
            final String API_KEY = "api_key";

            Uri builtUri = Uri.parse(MessageFormat.format(mContext.getString(R.string.tmdb_trailer_url), movieId))
                    .buildUpon()
                    .appendQueryParameter(API_KEY, BuildConfig.TMDB_API_KEY)
                    .build();

            URL url = new URL(builtUri.toString());
            Log.d(LOG_TAG, "URL: " + url.toString());
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));

            result = parseResultValues(movieId, reader);

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

    private boolean parseResultValues(String movieId, JsonReader reader) throws IOException {
        int inserted = 0;
        while (reader.hasNext()) {
            List<ContentValues> trailerList = new ArrayList();

            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("results") && reader.peek() != JsonToken.NULL) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        trailerList.add(parseContentValues(movieId, reader));
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            inserted += insertDataIntoContentProvider(trailerList);
        }
        Log.d(LOG_TAG, "Found [" + inserted + "] trailers");
        return inserted > 0;
    }

    private int insertDataIntoContentProvider(List<ContentValues> moviesList) {
        int result = 0;
        if (moviesList.size() > 0) {
            result = mContext.getContentResolver().bulkInsert(
                    TrailersEntry.CONTENT_URI,
                    moviesList.toArray(new ContentValues[moviesList.size()]));
        }
        return result;
    }

    @NonNull
    private ContentValues parseContentValues(String movieId, JsonReader reader) throws IOException {
        reader.beginObject();

        ContentValues trailerValues = new ContentValues();
        trailerValues.put(TrailersEntry.COLUMN_MOVIE_ID, movieId);
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("id")) {
                trailerValues.put(TrailersEntry.COLUMN_TRAILER_ID, reader.nextString());
            } else if (name.equals("iso_639_1")) {
                trailerValues.put(TrailersEntry.COLUMN_ISO_639_1, reader.nextString());
            } else if (name.equals("key")) {
                trailerValues.put(TrailersEntry.COLUMN_KEY, reader.nextString());
            } else if (name.equals("name")) {
                trailerValues.put(TrailersEntry.COLUMN_NAME, reader.nextString());
            } else if (name.equals("site")) {
                trailerValues.put(TrailersEntry.COLUMN_SITE, reader.nextString());
            } else if (name.equals("size")) {
                trailerValues.put(TrailersEntry.COLUMN_SIZE, reader.nextInt());
            } else if (name.equals("type")) {
                trailerValues.put(TrailersEntry.COLUMN_TYPE, reader.nextString());
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return trailerValues;
    }

}