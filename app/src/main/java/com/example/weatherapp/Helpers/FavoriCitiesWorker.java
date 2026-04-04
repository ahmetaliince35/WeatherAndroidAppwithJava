package com.example.weatherapp.Helpers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.weatherapp.data.AppDatabase;
import com.example.weatherapp.data.WeatherEntity;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FavoriCitiesWorker extends Worker {
    public FavoriCitiesWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {

        try {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            List<WeatherEntity> favorites = db.weatherDao().getAllFavoritesSync();



            if (favorites == null || favorites.isEmpty()) {
                Log.w("FavoriWorker", "UYARI: Liste boş veya null. İşlem bitti.");
                return Result.success();
            }

            CountDownLatch latch = new CountDownLatch(favorites.size());

            for (WeatherEntity city : favorites) {
                Log.d("FavoriWorker", " İstek hazırlanıyor: " + city.cityName);

                WeatherJsonAPI api = new WeatherJsonAPI(getApplicationContext(), URL_API.CurrentURL);
                api.getWeather(city.cityName, new WeatherJsonAPI.WeatherCallback() {
                    @Override
                    public void onSuccess(WeatherJsonAPI.WeatherData data) {

                        Executors.newSingleThreadExecutor().execute(() -> {
                            db.weatherDao().updateFavoriteWeather(
                                    city.cityName, data.temp, data.description, data.icon);
                            Log.d("FavoriWorker", "DB Güncellendi: " + city.cityName);
                            latch.countDown();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("FavoriWorker", "HATA: " + city.cityName + " -> " + error);
                        latch.countDown();
                    }
                });
            }



        } catch (Exception e) {
            Log.e("FavoriWorker", "KRİTİK HATA: " + e.getMessage());
            return Result.failure();
        }

        return Result.success();
    }
}