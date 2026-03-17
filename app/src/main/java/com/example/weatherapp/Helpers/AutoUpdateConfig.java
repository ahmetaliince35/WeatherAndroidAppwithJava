package com.example.weatherapp.Helpers;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.weatherapp.WeatherUpdater;

import java.util.concurrent.TimeUnit;

public class AutoUpdateConfig {
    public static void setupAutoUpdate(Context context ) {
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
    //Worker a ait görevleri denemek için oluşturuldu
    /*public static void setupAutoUpdateNow(Context context) {
        // Worker için OneTimeWorkRequest oluştur
        OneTimeWorkRequest weatherWorkRequest = new OneTimeWorkRequest.Builder(WeatherUpdater.class)
                .build();

        // Worker'ı kuyruğa ekle
        WorkManager.getInstance(context)
                .enqueue(weatherWorkRequest);

        Log.d("WeatherWorkerLauncher", "WeatherWorker kuyruğa eklendi");
    }*/
}
