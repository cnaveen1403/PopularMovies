package com.android.naveen.popularmovies;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.text.MessageFormat;

import com.android.naveen.popularmovies.data.MoviesContract;
import com.android.naveen.popularmovies.data.MoviesContract.MoviesEntry;
import com.android.naveen.popularmovies.data.MoviesContract.ReviewsEntry;
import com.android.naveen.popularmovies.data.MoviesContract.TrailersEntry;

public class MovieDetailAdapter extends CursorAdapter {

    private static final int MOVIE = 0;
    private static final int TRAILER = 1;
    private static final int REVIEW = 2;

    private OnClickListener mOnClickListener;
    private boolean mFavorite;

    public MovieDetailAdapter(Context context, Cursor cursor, int flags, OnClickListener onClickListener, boolean favorite) {
        super(context, cursor, flags);
        this.mOnClickListener = onClickListener;
        this.mFavorite = favorite;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return getCursorType(cursor);
    }

    private int getCursorType(Cursor cursor) {
        int result = -1;
        String type = cursor.getString(cursor.getColumnIndex(MoviesContract.MOVIE_DETAIL_TABLE));
        switch (type) {
            case MoviesEntry.TABLE_NAME:
                result = MOVIE;
                break;
            case TrailersEntry.TABLE_NAME:
                result = TRAILER;
                break;
            case ReviewsEntry.TABLE_NAME:
                result = REVIEW;
                break;
        }
        return result;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = null;
        switch (getItemViewType(cursor.getPosition())) {
            case MOVIE:
                view = LayoutInflater.from(context).inflate(R.layout.list_row_movie_detail, parent, false);
                view.setTag(R.id.LIST_ITEM_OVERVIEW, view.findViewById(R.id.overview));
                view.setTag(R.id.LIST_ITEM_ORIGINAL_TITLE, view.findViewById(R.id.original_title));
                view.setTag(R.id.LIST_ITEM_POSTER, view.findViewById(R.id.poster));
                view.setTag(R.id.LIST_ITEM_RELEASE_DATE, view.findViewById(R.id.release_date));
                view.setTag(R.id.LIST_ITEM_VOTE_AVERAGE, view.findViewById(R.id.vote_average));
                View star = view.findViewById(R.id.star);
                View starBlack = view.findViewById(R.id.star_black);
                if (mFavorite) {
                    star.setVisibility(View.GONE);
                    starBlack.setVisibility(View.VISIBLE);
                } else {
                    view.setTag(R.id.LIST_ITEM_STAR, star);
                    star.setTag(R.id.LIST_ITEM_STAR_BLACK, starBlack);
                }
                break;
            case TRAILER:
                view = LayoutInflater.from(context).inflate(R.layout.list_row_trailer, parent, false);
                view.setTag(R.id.LIST_ITEM_TYPE, view.findViewById(R.id.type));
                view.setTag(R.id.LIST_ITEM_NAME, view.findViewById(R.id.name));
                view.setTag(R.id.LIST_ITEM_SIZE, view.findViewById(R.id.size));
                view.setTag(R.id.LIST_ITEM_THUMBNAIL, view.findViewById(R.id.thumbnail));
                view.setTag(R.id.LIST_ITEM_SHARE, view.findViewById(R.id.share));
                break;
            case REVIEW:
                view = LayoutInflater.from(context).inflate(R.layout.list_row_review, parent, false);
                view.setTag(R.id.LIST_ITEM_AUTHOR, view.findViewById(R.id.author));
                view.setTag(R.id.LIST_ITEM_CONTENT, view.findViewById(R.id.content));
                break;
        }
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        switch (getItemViewType(cursor.getPosition())) {
            case MOVIE:
                String title = cursor.getString(cursor.getColumnIndex(MoviesEntry.COLUMN_ORIGINAL_TITLE));
                ((TextView) view.getTag(R.id.LIST_ITEM_ORIGINAL_TITLE)).setText(title);

                String overview = cursor.getString(cursor.getColumnIndex(MoviesEntry.COLUMN_OVERVIEW));
                ((TextView) view.getTag(R.id.LIST_ITEM_OVERVIEW)).setText(overview);

                ImageView posterView = (ImageView) view.getTag(R.id.LIST_ITEM_POSTER);
                String posterPath = cursor.getString(cursor.getColumnIndex(MoviesEntry.COLUMN_POSTER_PATH));
                if (posterPath != null) {
                    Picasso.with(context).load(context.getString(R.string.tmdb_poster_url) + "/" + posterPath).into(posterView);
                } else {
                    posterView.setImageDrawable(ContextCompat.getDrawable(context, R.mipmap.ic_movie));
                }

                String releaseDate = cursor.getString(cursor.getColumnIndex(MoviesEntry.COLUMN_RELEASE_DATE));
                ((TextView) view.getTag(R.id.LIST_ITEM_RELEASE_DATE)).setText(Utility.formatReleaseDate(context, releaseDate));

                String voteAverage = cursor.getString(cursor.getColumnIndex(MoviesEntry.COLUMN_VOTE_AVERAGE));
                ((TextView) view.getTag(R.id.LIST_ITEM_VOTE_AVERAGE)).setText(Utility.formatVoteAverage(context, voteAverage));

                View star = (View) view.getTag(R.id.LIST_ITEM_STAR);
                if (star != null) {
                    star.setOnClickListener(mOnClickListener);
                    star.setTag(R.id.FAVORITES_KEY, cursor.getString(cursor.getColumnIndex(MoviesEntry.COLUMN_MOVIE_ID)));
                }

                break;
            case TRAILER:
                String trailerName = cursor.getString(cursor.getColumnIndex(TrailersEntry.COLUMN_NAME));
                ((TextView) view.getTag(R.id.LIST_ITEM_NAME)).setText(trailerName);
                ((TextView) view.getTag(R.id.LIST_ITEM_TYPE)).setText(cursor.getString(cursor.getColumnIndex(TrailersEntry.COLUMN_TYPE)));
                ((TextView) view.getTag(R.id.LIST_ITEM_SIZE)).setText(String.valueOf(cursor.getInt(cursor.getColumnIndex(TrailersEntry.COLUMN_SIZE))));

                ImageView imageView = (ImageView) view.getTag(R.id.LIST_ITEM_THUMBNAIL);
                String trailerKey = cursor.getString(cursor.getColumnIndex(TrailersEntry.COLUMN_KEY));
                if (trailerKey != null) {
                    String url = MessageFormat.format(context.getString(R.string.youtube_thumbnail_url), trailerKey);
                    Picasso.with(context).load(url).into(imageView);
                    imageView.setOnClickListener(mOnClickListener);
                    imageView.setTag(R.id.TRAILER_KEY, trailerKey);
                }

                View shareImageView = (View) view.getTag(R.id.LIST_ITEM_SHARE);
                shareImageView.setOnClickListener(mOnClickListener);
                shareImageView.setTag(R.id.SHARE_KEY, trailerKey);
                shareImageView.setTag(R.id.SHARE_NAME, trailerName);

                break;
            case REVIEW:
                ((TextView) view.getTag(R.id.LIST_ITEM_AUTHOR)).setText(cursor.getString(cursor.getColumnIndex(ReviewsEntry.COLUMN_AUTHOR)));
                ((TextView) view.getTag(R.id.LIST_ITEM_CONTENT)).setText(cursor.getString(cursor.getColumnIndex(ReviewsEntry.COLUMN_CONTENT)));
                view.setTag(R.id.REVIEW_URL, cursor.getString(cursor.getColumnIndex(ReviewsEntry.COLUMN_URL)));
                break;
        }
    }
}
