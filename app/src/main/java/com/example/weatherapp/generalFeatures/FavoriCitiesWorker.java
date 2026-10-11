package com.example.weatherapp.generalFeatures;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.weatherapp.Helpers.FavoriteCitiesUpdater;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FavoriCitiesWorker extends Worker {
    private static final String TAG = "FavoriCitiesWorker";

    public FavoriCitiesWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "Favori şehirler güncelleniyor...");
            CountDownLatch latch = new CountDownLatch(1);
            FavoriteCitiesUpdater.updateFavorites(getApplicationContext(), latch::countDown);
            latch.await(35, TimeUnit.SECONDS);
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Worker hatası: " + e.getMessage());
            return Result.failure();
        }
    }
}