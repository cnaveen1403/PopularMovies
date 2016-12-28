package com.android.naveen.popularmovies;

import android.app.Activity;
import android.app.Fragment;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;

import com.android.naveen.popularmovies.data.MoviesContract;
import com.android.naveen.popularmovies.data.MoviesContract.MoviesEntry;
import com.android.naveen.popularmovies.sync.SyncAdapterMovies;

public class MovieListFragment extends Fragment implements android.app.LoaderManager.LoaderCallbacks<Cursor>,
        AdapterView.OnItemClickListener, AbsListView.OnScrollListener {

    public static final String SORT_CLAUSE_POPULARITY = MoviesContract.MoviesEntry.COLUMN_POPULARITY + " DESC," + MoviesContract.MoviesEntry._ID + " ASC";
    public static final String SORT_CLAUSE_RATING = MoviesContract.MoviesEntry.COLUMN_VOTE_AVERAGE + " DESC," + MoviesContract.MoviesEntry._ID + " ASC";

    private static final int MOVIE_LOADER = 0;

    private static final String[] COLUMNS = {
            MoviesEntry.TABLE_NAME + "." + MoviesEntry._ID,
            MoviesEntry.COLUMN_MOVIE_ID,
            MoviesEntry.COLUMN_POSTER_PATH
    };

    public static final int COLUMN_ID = 0;
    public static final int COLUMN_MOVIE_ID = 1;
    public static final int COLUMN_POSTER_PATH = 2;

    private static final String STATE_GRID_POSITION = "state_grid_position";
    private static final String STATE_GRID_VIEW = "state_grid_view";

    private Callbacks mCallbacks = sDummyCallbacks;
    private MovieListAdapter mMovieListAdapter;
    private GridView mGridView;
    private int mRestorePosition = -1;
    private Parcelable mRestoreGridViewState;

    public interface Callbacks {
        void onItemSelected(int id);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(int id) {
        }
    };

    public MovieListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_movie_list, container, false);

        mGridView = (GridView) rootView.findViewById(R.id.gridview);
        mMovieListAdapter = new MovieListAdapter(getActivity(), null, 0);
        mGridView.setAdapter(mMovieListAdapter);
        mGridView.setOnItemClickListener(this);
        mGridView.setOnScrollListener(this);


        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_GRID_POSITION)) {
            mRestorePosition = savedInstanceState.getInt(STATE_GRID_POSITION);
            mRestoreGridViewState = savedInstanceState.getParcelable(STATE_GRID_VIEW);
        }

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mCallbacks.onItemSelected((Integer) view.getTag(R.id.MOVIE_ID));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        int position = mGridView.getFirstVisiblePosition();
        outState.putInt(STATE_GRID_POSITION, position);
        outState.putParcelable(STATE_GRID_VIEW, mGridView.onSaveInstanceState());
        super.onSaveInstanceState(outState);
    }

    public void setSortOrder(int sortOrder) {
        Bundle bundle = new Bundle();
        bundle.putInt(MainActivity.SORT_ORDER, sortOrder);
        getLoaderManager().restartLoader(MOVIE_LOADER, bundle, this);

        switch (sortOrder) {
            case MainActivity.SORT_ORDER_POPULARITY:
                SyncAdapterMovies.syncMoviesListImmediately(getActivity(), SyncAdapterMovies.SORT_BY_POPULAR);
                break;
            case MainActivity.SORT_ORDER_RATING:
                SyncAdapterMovies.syncMoviesListImmediately(getActivity(), SyncAdapterMovies.SORT_BY_RATING);
                break;
        }

        mMovieListAdapter.notifyDataSetChanged();
    }

    public void setActivateOnItemClick(boolean activateOnItemClick) {
        mGridView.setChoiceMode(activateOnItemClick ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        int sortOrder = bundle.getInt(MainActivity.SORT_ORDER, MainActivity.SORT_ORDER_DEFAULT);
        if (sortOrder == MainActivity.SORT_ORDER_FAVORITES) {
            return new CursorLoader(getActivity(), MoviesEntry.FAVORITES_CONTENT_URI, COLUMNS, null, null, null);
        } else {
            return new CursorLoader(getActivity(), MoviesEntry.CONTENT_URI, COLUMNS, null, null,
                    sortOrder == MainActivity.SORT_ORDER_POPULARITY ? SORT_CLAUSE_POPULARITY : SORT_CLAUSE_RATING);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mMovieListAdapter.swapCursor(data);

        if (mRestorePosition != -1 && mRestoreGridViewState != null && mGridView.getCount() >= mRestorePosition) {
            mGridView.onRestoreInstanceState(mRestoreGridViewState);
            mRestorePosition = -1;
            mRestoreGridViewState = null;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mMovieListAdapter.swapCursor(null);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (totalItemCount - visibleItemCount < firstVisibleItem + visibleItemCount * 2) {
            SyncAdapterMovies.loadNextPageOnScroll(MovieListFragment.this.getActivity());
        }
    }
}