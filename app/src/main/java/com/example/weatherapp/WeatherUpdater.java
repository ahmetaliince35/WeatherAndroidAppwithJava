package com.example.weatherapp;

import android.content.Context;
import android.telecom.Call;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Arka planda periyodik hava durumu güncellemesi yapan Worker
 * WorkManager ile günde 2 kere çalışır
 */
public class WeatherUpdater extends Worker {

    private static final String TAG = "WeatherUpdateWorker";
    private static final String API_KEY = URL_API.API_KEY;
    private static final String BASE_URL = URL_API.CurrentURL;

    public WeatherUpdater(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork()
    {

        PreferencesManager prefsManager = new PreferencesManager(getApplicationContext());

        // Otomatik güncelleme kapalıysa işlem yapma
        if (!prefsManager.isAutoUpdateEnabled()) {
            return Result.success();
        }

        // Kayıtlı şehir var mı?
        String cityName = prefsManager.getSavedCityName();
        if (cityName == null || cityName.isEmpty()) {
            return Result.failure();
        }

        // Hava durumu verisini güncelle
        if(!prefsManager.shouldUpdate())
        {
            return Result.success();
        }
        boolean success= updateWeatherData(cityName,prefsManager);
        if (success)
        {
            return Result.success();
        } else
        {
            return Result.retry();
        }
    }
    /**
     * Retrofit arayüzü
     */
    /*public interface WeatherService {
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
    }*/


    /**
     * Hava durumu verisini API'den çek ve kaydet
     */
    private boolean updateWeatherData(String cityName, PreferencesManager prefsManager) {
        final boolean[] success = {false};
        final CountDownLatch latch = new CountDownLatch(1);

        String url = BASE_URL + cityName + "&appid=" + API_KEY + "&units=metric&lang=tr";
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject main = response.getJSONObject("main");
                            String cityName = response.getString("name");

                            double temperature = main.getDouble("temp");
                            int humidity = main.getInt("humidity");
                            double feelsLike = main.getDouble("feels_like");
                            int pressure = main.getInt("pressure");

                            // Hava durumu açıklaması
                            JSONObject weather = response.getJSONArray("weather").getJSONObject(0);
                            String description = weather.getString("description");

                            String icon = weather.getString("icon");

                            JSONObject wind = response.getJSONObject("wind");
                            double windSpeed = wind.getDouble("speed");

                            prefsManager.saveWeatherData(cityName, temperature, description, humidity, windSpeed * 3.6,
                                    feelsLike, pressure, icon
                            );
                            // Başarılı - son güncelleme zamanını kaydet
                            prefsManager.updateLastUpdateTime();
                            success[0] = true;
                        } catch (Exception e) {
                            Log.e(TAG, "Veri işleme hatası: " + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "API hatası: " + error.getMessage());
                        latch.countDown();
                    }
                }
        );

        queue.add(jsonObjectRequest);

        try {
            // İsteğin tamamlanmasını bekle (max 30 saniye)
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Bekleme hatası: " + e.getMessage());
        }

        return success[0];
    }
}