package com.example.weatherapp.generalFeatures;

import android.content.Context;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class AutoUpdateConfig {
    public static void setupAutoUpdate(Context context ) {
        Log.d("Setup","burada sıkıntı yok");
        PeriodicWorkRequest weatherUpdateRequest = new PeriodicWorkRequest.Builder(
                WeatherUpdater.class,
                12,
                TimeUnit.HOURS
        ).build();

        // WorkManager ile görevi planla
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "WeatherAutoUpdate",
                ExistingPeriodicWorkPolicy.KEEP, // Varsa devam ettir
                weatherUpdateRequest
        );

    }
    public static void setupAutoUpdateFavoriCities(Context context)
    {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                FavoriCitiesWorker.class,
                1,
                TimeUnit.HOURS
        ).build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "favorite_weather_update",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
        );

    }
}
