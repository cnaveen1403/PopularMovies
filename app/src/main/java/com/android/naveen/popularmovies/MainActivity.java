package com.android.naveen.popularmovies;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.android.naveen.popularmovies.sync.SyncAdapterMovies;

public class MainActivity extends AppCompatActivity implements MovieListFragment.Callbacks {
    public static final String PREFS_NAME = "MoviesPrefsFile";

    public static final int SORT_ORDER_POPULARITY = 0;
    public static final int SORT_ORDER_RATING = 1;
    public static final int SORT_ORDER_FAVORITES = 2;
    public static final int SORT_ORDER_DEFAULT = SORT_ORDER_POPULARITY;
    public static final String SORT_ORDER = "sortorder";

    private boolean mTwoPane;
    private MenuItem mItemPopular;
    private MenuItem mItemRating;
    private MenuItem mItemFavorites;
    private int mSortOrder;
    private MovieDetailFragment mMovieDetailFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MovieListFragment movieListFragment = (MovieListFragment) getFragmentManager().findFragmentById(R.id.movie_list);
        if (findViewById(R.id.movie_detail_container) != null) {
            mTwoPane = true;
            movieListFragment.setActivateOnItemClick(true);
        }

        SyncAdapterMovies.initializeSyncAdapter(this);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        mSortOrder = settings.getInt(SORT_ORDER, SORT_ORDER_DEFAULT);
        movieListFragment.setSortOrder(mSortOrder);
    }

    @Override
    public void onItemSelected(int id) {
        Bundle bundle = new Bundle();
        bundle.putInt(MovieDetailFragment.MOVIE_ID, id);
        if (mSortOrder == SORT_ORDER_FAVORITES) {
            bundle.putInt(MovieDetailFragment.FAVORITES, mSortOrder);
        }
        if (mTwoPane) {
            mMovieDetailFragment = new MovieDetailFragment();
            mMovieDetailFragment.setArguments(bundle);
            getFragmentManager().beginTransaction().replace(R.id.movie_detail_container, mMovieDetailFragment).commit();
        } else {
            Intent detailIntent = new Intent(this, MovieDetailActivity.class);
            detailIntent.putExtras(bundle);
            startActivity(detailIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        switch (mSortOrder) {
            case SORT_ORDER_POPULARITY:
                mItemPopular = menu.findItem(R.id.action_sort_by_popular);
                mItemPopular.setChecked(true);
                break;
            case SORT_ORDER_RATING:
                mItemRating = menu.findItem(R.id.action_sort_by_rating);
                mItemRating.setChecked(true);
                break;
            case SORT_ORDER_FAVORITES:
                mItemFavorites = menu.findItem(R.id.action_show_favorites);
                mItemFavorites.setChecked(true);
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort_by_popular:
                item.setChecked(true);
                updateSortOrder(SORT_ORDER_POPULARITY);
                return true;
            case R.id.action_sort_by_rating:
                item.setChecked(true);
                updateSortOrder(SORT_ORDER_RATING);
                return true;
            case R.id.action_show_favorites:
                item.setChecked(true);
                updateSortOrder(SORT_ORDER_FAVORITES);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateSortOrder(int sortOrder) {
        mSortOrder = sortOrder;
        if (mTwoPane && mMovieDetailFragment != null) {
            getFragmentManager().beginTransaction().remove(mMovieDetailFragment).commit();
        }
        ((MovieListFragment) getFragmentManager().findFragmentById(R.id.movie_list)).setSortOrder(sortOrder);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        settings.edit().putInt(SORT_ORDER, mSortOrder).commit();
    }
}