package com.example.weatherapp.Helpers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.weatherapp.Helpers.WeatherReceiver.WeatherCallback;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherData;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherRepository;
import com.example.weatherapp.data.Databases.AppDatabase;
import com.example.weatherapp.data.Entities.WeatherEntity;
import com.example.weatherapp.generalFeatures.PreferencesManager;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FavoriteCitiesUpdater {
    private static final String TAG = "FavoriteCitiesUpdater";
    private static final int MAX_PARALLEL_THREADS = 4;
    private static final int PER_CITY_TIMEOUT_SECONDS = 10;
    private static final int TOTAL_TIMEOUT_SECONDS = 25;

    public static void updateFavorites(Context context, @Nullable Runnable onComplete) {
        if (context == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        Context appContext = context.getApplicationContext();

        ExecutorService supervisorExecutor = Executors.newSingleThreadExecutor();
        supervisorExecutor.execute(() -> {
            ExecutorService workerPool = null;
            try {
                AppDatabase db = AppDatabase.getInstance(appContext);
                List<WeatherEntity> favorites = db.weatherDao().getAllFavoritesSync();
                if (favorites == null || favorites.isEmpty()) {
                    Log.d(TAG, "Favori şehir listesi boş.");
                    return;
                }

                PreferencesManager prefs = PreferencesManager.getInstance(appContext);
                String provider = prefs.getSelectedProvider();
                String vcModel = prefs.getVisualCrossingModel();
                WeatherRepository repository = new WeatherRepository(appContext);

                int poolSize = Math.min(MAX_PARALLEL_THREADS, Math.max(1, favorites.size()));
                workerPool = Executors.newFixedThreadPool(poolSize);

                CountDownLatch latch = new CountDownLatch(favorites.size());

                for (WeatherEntity cityEntity : favorites) {
                    final String cityName = cityEntity.cityName;
                    workerPool.submit(() -> {
                        try {
                            updateSingleCity(repository, db, cityName, provider, vcModel);
                        } catch (Exception e) {
                            Log.e(TAG, cityName + " güncelleme hatası: " + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                boolean finishedInTime = latch.await(TOTAL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!finishedInTime) {
                    Log.w(TAG, "Favori şehirler zaman aşımına uğradı (" + TOTAL_TIMEOUT_SECONDS + "s). Kalan görevler sonlandırılıyor.");
                } else {
                    Log.d(TAG, "Tüm favori şehirler başarıyla güncellendi.");
                }

            } catch (Exception e) {
                Log.e(TAG, "updateFavorites genel hata: " + e.getMessage());
            } finally {
                if (workerPool != null) {
                    workerPool.shutdownNow();
                }
                supervisorExecutor.shutdown();
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    private static void updateSingleCity(WeatherRepository repository, AppDatabase db,
                                         String cityName, String provider, String model) {
        if (cityName == null || cityName.trim().isEmpty()) return;

        final CountDownLatch cityLatch = new CountDownLatch(1);
        repository.getCurrentWeather(provider, cityName, model, new WeatherCallback<WeatherData>() {
            @Override
            public void onSuccess(WeatherData data) {
                try {
                    db.weatherDao().updateFavoriteWeather(
                            cityName,
                            data.temp,
                            data.description,
                            data.icon
                    );
                    Log.d(TAG, "Favori güncellendi: " + cityName + " -> " + data.temp + "°C");
                } catch (Exception e) {
                    Log.e(TAG, cityName + " veritabanı yazma hatası: " + e.getMessage());
                } finally {
                    cityLatch.countDown();
                }
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, cityName + " güncellenemedi: " + error);
                cityLatch.countDown();
            }
        });

        try {
            cityLatch.await(PER_CITY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
    }
}
