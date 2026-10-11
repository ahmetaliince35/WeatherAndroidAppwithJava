package com.example.weatherapp.Helpers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.weatherapp.Helpers.WeatherReceiver.OpenMeteoService;
import com.example.weatherapp.Helpers.WeatherReceiver.VisualCrossingService;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherAPIService;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherJsonAPI;
import com.example.weatherapp.data.Databases.AppDatabase;
import com.example.weatherapp.data.Entities.WeatherEntity;
import com.example.weatherapp.generalFeatures.PreferencesManager;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FavoriteCitiesUpdater {
    private static final String TAG = "FavoriteCitiesUpdater";

    public static void updateFavorites(Context context, @Nullable Runnable onComplete) {
        if (context == null) return;
        Context appContext = context.getApplicationContext();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(appContext);
                List<WeatherEntity> favorites = db.weatherDao().getAllFavoritesSync();
                if (favorites == null || favorites.isEmpty()) {
                    if (onComplete != null) onComplete.run();
                    return;
                }

                PreferencesManager prefs = PreferencesManager.getInstance(appContext);
                String provider = prefs.getSelectedProvider();
                String vcModel = prefs.getVisualCrossingModel();

                CountDownLatch latch = new CountDownLatch(favorites.size());

                for (WeatherEntity city : favorites) {
                    fetchCityWeather(appContext, db, city.cityName, provider, vcModel, latch);
                }

                latch.await(35, TimeUnit.SECONDS);
            } catch (Exception e) {
                Log.e(TAG, "Favori güncelleme hatası: " + e.getMessage());
            } finally {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    private static void fetchCityWeather(Context context, AppDatabase db, String cityName, String provider, String vcModel, CountDownLatch latch) {
        if (provider == null) provider = "OPEN_METEO";

        final String cleanCity;
        if (cityName != null && cityName.contains("/")) {
            cleanCity = cityName.split("/")[0].trim();
        } else {
            cleanCity = cityName != null ? cityName.trim() : "";
        }

        switch (provider) {
            case "OWM": {
                WeatherJsonAPI api = new WeatherJsonAPI(context);
                api.getWeatherfromOpenWEatherMAp(cleanCity, false, new WeatherJsonAPI.WeatherCallback() {
                    @Override
                    public void onSuccess(WeatherJsonAPI.WeatherData data) {
                        saveData(db, cityName, data.temp, data.description, data.icon, latch);
                    }

                    @Override
                    public void onError(String error) {
                        latch.countDown();
                    }
                });
                break;
            }

            case "WEATHER_API": {
                WeatherAPIService api = new WeatherAPIService(context);
                api.getCurrentWeather(cleanCity, false, new WeatherAPIService.WeatherCallback() {
                    @Override
                    public void onSuccess(WeatherJsonAPI.WeatherData data) {
                        saveData(db, cityName, data.temp, data.description, data.icon, latch);
                    }

                    @Override
                    public void onError(String error) {
                        latch.countDown();
                    }
                });
                break;
            }

            case "VISUAL_CROSSING": {
                VisualCrossingService api = new VisualCrossingService(context);
                api.getCurrentWeather(cleanCity, vcModel, false, new VisualCrossingService.WeatherCallback() {
                    @Override
                    public void onSuccess(WeatherJsonAPI.WeatherData data) {
                        saveData(db, cityName, data.temp, data.description, data.icon, latch);
                    }

                    @Override
                    public void onError(String error) {
                        latch.countDown();
                    }
                });
                break;
            }

            case "OPEN_METEO":
            default: {
                OpenMeteoService api = new OpenMeteoService(context);
                api.searchGlobalLocation(cleanCity, new OpenMeteoService.GeoSearchCallback() {
                    @Override
                    public void onSuccess(List<OpenMeteoService.GeoSearchResult> results) {
                        if (results != null && !results.isEmpty()) {
                            OpenMeteoService.GeoSearchResult loc = results.get(0);
                            api.getWeatherFromOpenMeteo(loc.latitude, loc.longitude, cleanCity, false, new OpenMeteoService.WeatherCallback() {
                                @Override
                                public void onSuccess(OpenMeteoService.WeatherData data) {
                                    saveData(db, cityName, data.temp, data.description, data.icon, latch);
                                }

                                @Override
                                public void onError(String error) {
                                    latch.countDown();
                                }
                            });
                        } else {
                            latch.countDown();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        latch.countDown();
                    }
                });
                break;
            }
        }
    }

    private static void saveData(AppDatabase db, String cityName, double temp, String desc, String icon, CountDownLatch latch) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                db.weatherDao().updateFavoriteWeather(cityName, temp, desc, icon);
                Log.d(TAG, "Favori güncellendi: " + cityName + " -> " + temp + "°C");
            } catch (Exception e) {
                Log.e(TAG, "DB update error: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
    }
}
