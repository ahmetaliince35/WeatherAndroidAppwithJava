package com.example.weatherapp;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class WeatherUpdater extends Worker {

    private static final String TAG = "WeatherUpdateWorker";
    private static final String API_KEY = URL_API.API_KEY;
     private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";;

    public WeatherUpdater(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Worker başlatıldı");

        PreferencesManager prefsManager = new PreferencesManager(getApplicationContext());

        if (!prefsManager.isAutoUpdateEnabled()) {
            Log.d(TAG, "Otomatik güncelleme kapalı, işleme gerek yok");
            return Result.success();
        }

        String cityName = prefsManager.getSavedCityName();
        if (cityName == null || cityName.isEmpty()) {
            Log.e(TAG, "Kayıtlı şehir yok, worker başarısız");
            return Result.failure();
        }

        // Hava durumu güncelle
        boolean success = updateWeatherData(cityName, prefsManager);
        if (success) {
            Log.d(TAG, "Hava durumu başarıyla güncellendi");
            return Result.success();
        } else {
            Log.e(TAG, "Hava durumu güncellemesi başarısız, retry");
            return Result.retry();
        }
    }

    private boolean updateWeatherData(String cityName, PreferencesManager prefsManager) {
        try {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            WeatherService service = retrofit.create(WeatherService.class);
            Log.d(TAG, "Retrofit çağrısı yapılıyor: " + cityName);

            // SENKRON İSTEĞİ
            Call<WeatherResponse> call = service.getCurrentWeather(cityName, API_KEY, "metric", "tr");
            Response<WeatherResponse> response = call.execute(); // Worker thread’de güvenli

            if (!response.isSuccessful() || response.body() == null) {
                Log.e(TAG, "API hatası: " + response.code());
                return false;
            }

            WeatherResponse data = response.body();
            Log.d(TAG, "API’den veri geldi: " + data.name);

            prefsManager.saveWeatherData(
                    data.name,
                    data.main.temp,
                    data.weather.get(0).description,
                    data.main.humidity,
                    data.wind.speed * 3.6,
                    data.main.feels_like,
                    data.main.pressure,
                    data.weather.get(0).icon
            );
            prefsManager.updateLastUpdateTime();
            Log.d(TAG, "Preferences güncellendi");

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Hava durumu güncelleme hatası: " + e.getMessage());
            return false;
        }
    }

    // RETROFIT ARAYÜZÜ
    public interface WeatherService {
        @GET("weather")
        Call<WeatherResponse> getCurrentWeather(
                @Query("q") String city,
                @Query("appid") String apiKey,
                @Query("units") String units,
                @Query("lang") String lang
        );
    }

    public static class WeatherResponse {
        public Main main;
        public List<Weather> weather;
        public Wind wind;
        public String name;

        public static class Main {
            public double temp;
            public int humidity;
            public double feels_like;
            public int pressure;
        }

        public static class Weather {
            public String description;
            public String icon;
        }

        public static class Wind {
            public double speed;
        }
    }
}