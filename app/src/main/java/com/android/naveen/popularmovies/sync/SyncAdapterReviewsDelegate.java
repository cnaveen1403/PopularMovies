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
import com.android.naveen.popularmovies.data.MoviesContract.ReviewsEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class SyncAdapterReviewsDelegate {
    private static final String LOG_TAG = SyncAdapterReviewsDelegate.class.getSimpleName();
    private Context mContext;

    public SyncAdapterReviewsDelegate(Context context) {
        this.mContext = context;
    }

    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "onPerformSync");
        String movieId = extras.getString(SyncAdapterMovies.MOVIE_ID);
        if (movieId != null && !movieId.trim().isEmpty() && Utility.isNetworkConnected(mContext)) {
            retrieveReviewData(movieId);
        }
    }

    private boolean retrieveReviewData (String movieId){
        Log.d(LOG_TAG, "retrieveReviewData movieId[" + movieId + "]");

        boolean flag = false;

        HttpURLConnection urlConnection = null;
        JsonReader reader = null;
        try{

            final String API_KEY = "api_key";

            Uri builtUri = Uri.parse(MessageFormat.format(mContext.getString(R.string.tmdb_review_url), movieId))
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

            parseResultValues(movieId, reader);

            flag = true;

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
        return flag;
    }

    private void parseResultValues(String movieId, JsonReader reader) throws IOException {
        while (reader.hasNext()) {
            List<ContentValues> reviewList = new ArrayList();

            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("results") && reader.peek() != JsonToken.NULL) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        reviewList.add(parseContentValues(movieId, reader));
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            int inserted = insertDataIntoContentProvider(reviewList);
            Log.d(LOG_TAG, "Found [" + inserted + "] reviews");
        }
    }

    private int insertDataIntoContentProvider(List<ContentValues> moviesList) {
        int result = 0;
        if (moviesList.size() > 0) {
            result = mContext.getContentResolver().bulkInsert(
                    ReviewsEntry.CONTENT_URI,
                    moviesList.toArray(new ContentValues[moviesList.size()]));
        }
        return result;
    }

    @NonNull
    private ContentValues parseContentValues(String movieId, JsonReader reader) throws IOException {
        reader.beginObject();

        ContentValues reviewValues = new ContentValues();
        reviewValues.put(ReviewsEntry.COLUMN_MOVIE_ID, movieId);
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("id")) {
                reviewValues.put(ReviewsEntry.COLUMN_REVIEW_ID, reader.nextString());
            } else if (name.equals("author")) {
                reviewValues.put(ReviewsEntry.COLUMN_AUTHOR, reader.nextString());
            } else if (name.equals("content")) {
                reviewValues.put(ReviewsEntry.COLUMN_CONTENT, reader.nextString());
            } else if (name.equals("url")) {
                reviewValues.put(ReviewsEntry.COLUMN_URL, reader.nextString());
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return reviewValues;
    }

}