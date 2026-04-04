package com.example.weatherapp.Helpers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.weatherapp.data.AppDatabase;
import com.example.weatherapp.data.WeatherEntity;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class FavoriCitiesWorker extends Worker {
    public FavoriCitiesWorker(Context context, WorkerParameters params)
    {
        super(context,params);
    }
    @Override
    @NonNull
    public Result doWork() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        List<WeatherEntity> favorites = db.weatherDao().getAllFavoritesSync();

        CountDownLatch latch = new CountDownLatch(favorites.size());
        for (WeatherEntity city : favorites) {
            WeatherJsonAPI api = new WeatherJsonAPI(getApplicationContext(), URL_API.CurrentURL);

            api.getWeather(city.cityName, new WeatherJsonAPI.WeatherCallback() {
                @Override
                public void onSuccess(WeatherJsonAPI.WeatherData data) {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.weatherDao().updateFavoriteWeather(
                                city.cityName,
                                data.temp,
                                data.description,
                                data.icon
                        );
                        latch.countDown();
                    });
                }

                @Override
                public void onError(String error) {
                    latch.countDown();
                }
            });
        }
            try {
                latch.await(); // Tüm şehirler bitene kadar bekle
            } catch (InterruptedException e) {
                return Result.failure();
            }

            return Result.success();
        }

    }

