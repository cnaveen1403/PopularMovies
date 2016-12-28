package com.android.naveen.popularmovies;

import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import java.text.MessageFormat;

import com.android.naveen.popularmovies.data.MoviesContract;
import com.android.naveen.popularmovies.data.MoviesContract.MoviesEntry;
import com.android.naveen.popularmovies.data.MoviesContract.TrailersEntry;
import com.android.naveen.popularmovies.data.MoviesContract.ReviewsEntry;
import com.android.naveen.popularmovies.sync.SyncAdapterMovies;

public class MovieDetailFragment extends Fragment implements OnClickListener, LoaderCallbacks<Cursor> {

    public static final String MOVIE_ID = "movie_id";
    public static final String FAVORITES = "favorites";
    public static final String LIST_VIEW_STATE = "list_view_state";

    private static final int DETAIL_LOADER = 0;

    private int mMovieId = -1;
    private boolean mFavorite = false;

    private ListView mListView;
    private CursorLoader mDetailLoader;
    private MovieDetailAdapter mMovieDetailAdapter;
    private Parcelable mRestoreListViewState;

    public MovieDetailFragment() {
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(MOVIE_ID)) {
            mMovieId = getArguments().getInt(MOVIE_ID);
        }
        mFavorite = getArguments().containsKey(FAVORITES);
        if (!mFavorite) {
            mFavorite = isFavorite(mMovieId);
        }
        if (!mFavorite) {
            SyncAdapterMovies.syncMovieDetailsImmediately(getActivity(), Integer.toString(mMovieId));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mMovieId = savedInstanceState.getInt(MOVIE_ID);
            mFavorite = savedInstanceState.getBoolean(FAVORITES);
            mRestoreListViewState = savedInstanceState.getParcelable(LIST_VIEW_STATE);
        }

        View rootView = inflater.inflate(R.layout.fragment_movie_detail, container, false);
        mListView = (ListView) rootView.findViewById(R.id.listview);

        mMovieDetailAdapter = new MovieDetailAdapter(getActivity(), null, 0, this, mFavorite);
        mListView.setAdapter(mMovieDetailAdapter);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Bundle bundle = new Bundle();
        bundle.putInt(MOVIE_ID, mMovieId);
        getLoaderManager().restartLoader(DETAIL_LOADER, bundle, this);

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(MOVIE_ID, mMovieId);
        outState.putBoolean(FAVORITES, mFavorite);
        outState.putParcelable(LIST_VIEW_STATE, mListView.onSaveInstanceState());
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        if (id == DETAIL_LOADER && bundle.containsKey(MOVIE_ID)) {
            int movieId = bundle.getInt(MOVIE_ID);
            Uri baseContentUri = mFavorite ? MoviesContract.FAVORITES_BASE_CONTENT_URI : MoviesContract.BASE_CONTENT_URI;
            Builder builder = baseContentUri.buildUpon()
                    .appendPath(MoviesContract.PATH_MOVIES)
                    .appendPath(MoviesContract.PATH_MOVIE_DETAILS);
            ContentUris.appendId(builder, movieId);
            mDetailLoader = new CursorLoader(getActivity(), builder.build(), null, null, null, null);
            return mDetailLoader;
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader == mDetailLoader) {
            mMovieDetailAdapter.swapCursor(data);

            if (mRestoreListViewState != null) {
                mListView.onRestoreInstanceState(mRestoreListViewState);
                mRestoreListViewState = null;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader == mDetailLoader) {
            mMovieDetailAdapter.swapCursor(null);
        }
    }

    @Override
    public void onClick(View view) {
        String tag;
        if ((tag = (String) view.getTag(R.id.FAVORITES_KEY)) != null) {
            String title = copyMovieToFavorites(Integer.parseInt(tag));
            if (title != null) {
                String toastText = MessageFormat.format(getString(R.string.toast_add_to_favorites), title);
                Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT).show();
                view.setVisibility(View.GONE);
                ((View)view.getTag(R.id.LIST_ITEM_STAR_BLACK)).setVisibility(View.VISIBLE);
            }
        } else if ((tag = (String) view.getTag(R.id.SHARE_KEY)) != null) {
            shareTrailerUrl(tag, (String) view.getTag(R.id.SHARE_NAME));
        } else if ((tag = (String) view.getTag(R.id.TRAILER_KEY)) != null) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MessageFormat.format(getString(R.string.youtube_watch_url), tag))));
        }
    }

    private void shareTrailerUrl(String trailerKey, String trailerName) {
        Intent share = new Intent(android.content.Intent.ACTION_SEND);
        share.setType("text/plain");
        share.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        share.putExtra(Intent.EXTRA_SUBJECT, trailerName);
        share.putExtra(Intent.EXTRA_TEXT, MessageFormat.format(getString(R.string.youtube_watch_url), trailerKey));
        startActivity(Intent.createChooser(share, "Share trailer"));
    }

    private String copyMovieToFavorites(int movieId) {
        String title = null;

        String[] projection = new String[]{"*"};
        String selection = MoviesEntry.COLUMN_MOVIE_ID + " = ?";
        String[] selectionArgs = {String.valueOf(movieId)};

        ContentResolver contentResolver = getActivity().getContentResolver();
        Cursor cursorMovies = null;
        Cursor cursorTrailers = null;
        Cursor cursorReviews = null;

        try {
            cursorMovies = contentResolver.query(MoviesEntry.CONTENT_URI, projection, selection, selectionArgs, null);
            if (cursorMovies.moveToFirst()) {
                ContentValues contentValues = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(cursorMovies, contentValues);
                contentValues.remove("_id");
                title = contentValues.getAsString(MoviesEntry.COLUMN_ORIGINAL_TITLE);
                contentResolver.insert(MoviesEntry.FAVORITES_CONTENT_URI, contentValues);
            }

            cursorTrailers = contentResolver.query(TrailersEntry.CONTENT_URI, projection, selection, selectionArgs, null);
            while (cursorTrailers.moveToNext()) {
                ContentValues contentValues = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(cursorTrailers, contentValues);
                contentValues.remove("_id");
                contentResolver.insert(TrailersEntry.FAVORITES_CONTENT_URI, contentValues);
            }

            cursorReviews = contentResolver.query(ReviewsEntry.CONTENT_URI, projection, selection, selectionArgs, null);
            while (cursorReviews.moveToNext()) {
                ContentValues contentValues = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(cursorReviews, contentValues);
                contentValues.remove("_id");
                contentResolver.insert(ReviewsEntry.FAVORITES_CONTENT_URI, contentValues);
            }
        } finally {
            if (cursorMovies != null) {
                cursorMovies.close();
            }
            if (cursorTrailers != null) {
                cursorTrailers.close();
            }
            if (cursorReviews != null) {
                cursorReviews.close();
            }
        }

        return title;
    }

    private boolean isFavorite(int movieId) {

        String[] projection = new String[]{MoviesEntry.COLUMN_MOVIE_ID};
        String selection = MoviesEntry.COLUMN_MOVIE_ID + " = ?";
        String[] selectionArgs = {String.valueOf(movieId)};

        ContentResolver contentResolver = getActivity().getContentResolver();
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(MoviesEntry.FAVORITES_CONTENT_URI, projection, selection, selectionArgs, null);
            return cursor.getCount() > 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}