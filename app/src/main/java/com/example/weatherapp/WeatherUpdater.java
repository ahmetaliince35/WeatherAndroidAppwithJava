package com.example.weatherapp;

import android.content.Context;
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
        Log.d(TAG, "WeatherUpdateWorker başlatıldı");

        PreferencesManager prefsManager = new PreferencesManager(getApplicationContext());

        // Otomatik güncelleme kapalıysa işlem yapma
        if (!prefsManager.isAutoUpdateEnabled()) {
            Log.d(TAG, "Otomatik güncelleme kapalı");
            return Result.success();
        }

        // Kayıtlı şehir var mı?
        String cityName = prefsManager.getSavedCityName();
        if (cityName == null || cityName.isEmpty()) {
            Log.d(TAG, "Kayıtlı şehir yok");
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
            Log.d(TAG, "Hava durumu güncellendi: " + cityName);
            return Result.success();
        } else
        {
            Log.e(TAG, "Hava durumu güncellenemedi");
            return Result.retry();
        }
    }

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
                            // Başarılı - son güncelleme zamanını kaydet
                            prefsManager.updateLastUpdateTime();
                            success[0] = true;
                            Log.d(TAG, "API'den veri alındı");
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