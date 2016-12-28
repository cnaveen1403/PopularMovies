package com.android.naveen.popularmovies;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

public class MovieListAdapter extends CursorAdapter {

    public MovieListAdapter (Context context, Cursor c, int flags){
        super(context, c, flags);
    }

    @Override
    public View newView (Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.grid_item_movie_poster, parent, false);
        view.setTag(R.id.LIST_ITEM_ICON, view.findViewById(R.id.list_item_icon));
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ImageView imageView = (ImageView) view.getTag(R.id.LIST_ITEM_ICON);
        String posterPath = cursor.getString(MovieListFragment.COLUMN_POSTER_PATH);
        if (posterPath != null) {
            Picasso.with(context).load(context.getString(R.string.tmdb_poster_url) + "/" + posterPath).into(imageView);
        } else {
            imageView.setImageDrawable(ContextCompat.getDrawable(context, R.mipmap.ic_movie));
        }
        view.setTag(R.id.MOVIE_ID, cursor.getInt(MovieListFragment.COLUMN_MOVIE_ID));
    }
}