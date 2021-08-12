package com.androidtutz.anushka.moviesapp.view;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import com.androidtutz.anushka.moviesapp.R;
import com.androidtutz.anushka.moviesapp.adapter.MovieAdapter;
import com.androidtutz.anushka.moviesapp.model.Movie;
import com.androidtutz.anushka.moviesapp.model.MovieDBResponse;
import com.androidtutz.anushka.moviesapp.service.MoviesDataService;
import com.androidtutz.anushka.moviesapp.service.RetrofitInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private ArrayList<Movie> movies;
    private RecyclerView recyclerView;
    private MovieAdapter movieAdapter;
    private SwipeRefreshLayout swipeContainer;
    private Observable<MovieDBResponse> movieDBResponseObservable;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setTitle(" TMDb Popular Movies Today");

        getPopularMovies();

        swipeContainer = findViewById(R.id.swipe_layout);
        swipeContainer.setColorSchemeResources(R.color.colorPrimary);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getPopularMovies();
            }
        });

    }

    public void getPopularMovies() {

        movies = new ArrayList<>();
        MoviesDataService getMoviesDataService = RetrofitInstance.getService();
        movieDBResponseObservable = getMoviesDataService.getPopularMoviesWithRx(this.getString(R.string.api_key));

        compositeDisposable.add(
            movieDBResponseObservable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap((Function<MovieDBResponse, Observable<Movie>>) response
                            -> Observable.fromArray(response.getMovies().toArray(new Movie[0])))
                    .filter(movie -> movie.getVoteAverage() > 7.0)
                    .sorted((o1, o2) -> o1.getVoteAverage() > o2.getVoteAverage() ? -1 : o1.getVoteAverage() > o2.getVoteAverage() ? 0 : 1)
                    .subscribeWith(new DisposableObserver<Movie>() {

                        @Override
                        public void onNext(@NonNull Movie movie) {

                            movies.add(movie);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.d("rx", e.getMessage());
                        }

                        @Override
                        public void onComplete() {
                            init();
                        }
                    })
        );

    }

    @Override
    public void onDestroy() {
        compositeDisposable.clear();

        super.onDestroy();
    }

    public void init() {

        recyclerView = findViewById(R.id.rvMovies);
        movieAdapter = new MovieAdapter(this, movies);

        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        }

        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(movieAdapter);
        movieAdapter.notifyDataSetChanged();

    }
}


