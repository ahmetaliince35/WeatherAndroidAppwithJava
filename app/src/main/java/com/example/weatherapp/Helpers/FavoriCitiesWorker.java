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

            if (favorites == null || favorites.isEmpty()) return Result.success();

            // CountDownLatch ile bütün şehirlerin bitmesini bekleyeceğiz
            CountDownLatch latch = new CountDownLatch(favorites.size());

            for (WeatherEntity city : favorites) {
                WeatherJsonAPI api = new WeatherJsonAPI(getApplicationContext(), URL_API.CurrentURL);

                // DİKKAT: false gönderiyoruz! Favoriler güncellenirken AI çalışmasın.
                api.getWeather(city.cityName, false, new WeatherJsonAPI.WeatherCallback() {
                    @Override
                    public void onSuccess(WeatherJsonAPI.WeatherData data) {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            db.weatherDao().updateFavoriteWeather(
                                    city.cityName, data.temp, data.description, data.icon);
                            Log.d("FavoriWorker", "Güncellendi: " + city.cityName);
                            latch.countDown(); // Bir şehir bitti
                        });
                    }

                    @Override
                    public void onError(String error) {
                        latch.countDown(); // Hata olsa da sayacı düşür ki Worker takılmasın
                    }
                });
            }

            // Bütün isteklerin bitmesi için Worker'ı burada bekletiyoruz (Max 30 saniye)
            latch.await(30, TimeUnit.SECONDS);
            return Result.success();

        } catch (Exception e) {
            return Result.failure();
        }
}
}