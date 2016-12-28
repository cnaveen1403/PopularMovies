package com.android.naveen.popularmovies;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Utility {
    private static final String LOG_TAG = Utility.class.getSimpleName();
    private static final SimpleDateFormat DATE_FORMAT_TMDB = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATE_FORMAT_PRETTY = new SimpleDateFormat("MMMM yyyy");

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static String formatReleaseDate(Context context, String releaseDate) {
        String result;
        if (releaseDate != null && !releaseDate.trim().isEmpty()) {
            try {
                result = DATE_FORMAT_PRETTY.format(DATE_FORMAT_TMDB.parse(releaseDate));
            } catch (ParseException e) {
                Log.d(LOG_TAG, "Unable to parse date [" + releaseDate + "] Error:" + e.getMessage());
                result = context.getString(R.string.unknown_release_date);
            }
        } else {
            result = context.getString(R.string.unknown_release_date);
        }
        return result;
    }

    public static String formatVoteAverage(Context context, String voteAverage) {
        String result;
        if (voteAverage != null && !voteAverage.trim().isEmpty()) {
            result = voteAverage + " / 10";
        } else {
            result = context.getString(R.string.unknown_vote_average);
        }
        return result;
    }
}