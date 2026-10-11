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
    private static final int WORKER_TIMEOUT_SECONDS = 30;

    public FavoriCitiesWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "Favori şehirler worker başlatıldı.");
            CountDownLatch latch = new CountDownLatch(1);
            FavoriteCitiesUpdater.updateFavorites(getApplicationContext(), latch::countDown);
            boolean finished = latch.await(WORKER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (finished) {
                Log.d(TAG, "Favori şehirler worker başarıyla tamamlandı.");
                return Result.success();
            } else {
                Log.w(TAG, "Favori şehirler worker zaman aşımına uğradı, yeniden denenecek.");
                return Result.retry();
            }
        } catch (Exception e) {
            Log.e(TAG, "Favori şehirler worker hatası: " + e.getMessage());
            return Result.failure();
        }
    }
}