package com.example.weatherapp.Helpers.WeatherReceiver;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.weatherapp.BuildConfig;
import com.example.weatherapp.Helpers.ForecastItem;
import com.example.weatherapp.Helpers.GeminiHelperPackages.GeminiPrompter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class VisualCrossingService {

    private static final String TAG = "VisualCrossingService";
    private static final String API_KEY = BuildConfig.visualCrossingKey; // Key'inizi buraya girin
    private final RequestQueue requestQueue;

    public interface WeatherCallback {
        void onSuccess(WeatherJsonAPI.WeatherData data);
        void onError(String error);
    }

    public interface ForecastCallback {
        void onSuccess(List<ForecastItem> forecastList);
        void onError(String error);
    }

    public VisualCrossingService(Context context) {
        this.requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    // 1. ANLIK HAVA DURUMU
    public void getCurrentWeather(String cityName, boolean isAIactive, final WeatherCallback callback) {
        String url = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/"
                + cityName.trim() + "?unitGroup=metric&key=" + API_KEY + "&contentType=json";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        WeatherJsonAPI.WeatherData data = new WeatherJsonAPI.WeatherData();
                        data.cityName = response.optString("address", cityName);

                        JSONObject current = response.getJSONObject("currentConditions");
                        data.temp = current.optDouble("temp", 0.0);
                        data.humidity = current.optInt("humidity", 0);
                        data.pressure = (int) current.optDouble("pressure", 1013.0);

                        double windKph = current.optDouble("windspeed", 0.0);
                        data.windSpeed = windKph / 3.6; // km/h -> m/s

                        data.windDirection = (float) current.optDouble("winddir", 0.0);
                        data.cloudiness = current.optDouble("cloudcover", 0.0);
                        data.description = current.optString("conditions", "Parçalı Bulutlu");
                        data.icon = current.optString("icon", "02d");

                        if (isAIactive) {
                            new Thread(() -> {
                                try {
                                    GeminiPrompter aiProvider = new GeminiPrompter();
                                    data.AIAdvice = aiProvider.getWeatherAdvice(data.cityName, data.temp, data.description, data.icon);
                                } catch (Exception e) {
                                    Log.e(TAG, "AI Hatası: " + e.getMessage());
                                    data.AIAdvice = "Hava durumu bilgisi alındı.";
                                } finally {
                                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(data));
                                }
                            }).start();
                        } else {
                            data.AIAdvice = "";
                            callback.onSuccess(data);
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Parsing Hatası: " + e.getMessage());
                        callback.onError("Visual Crossing verisi işlenemedi.");
                    }
                },
                error -> {
                    Log.e(TAG, "Volley Hatası: " + error.toString());
                    callback.onError("Visual Crossing bağlantı hatası. API Key kontrol edin.");
                }
        );

        requestQueue.add(request);
    }

    // 2. GÜNLÜK TAHMİN
    public void getDailyForecast(String cityName, final ForecastCallback callback) {
        String url = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/"
                + cityName.trim() + "?unitGroup=metric&key=" + API_KEY + "&contentType=json";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        List<ForecastItem> list = new ArrayList<>();
                        JSONArray days = response.getJSONArray("days");

                        for (int i = 0; i < Math.min(days.length(), 7); i++) {
                            JSONObject dayObj = days.getJSONObject(i);
                            String date = dayObj.optString("datetime", "");
                            double temp = dayObj.optDouble("temp", 0.0);
                            double minTemp = dayObj.optDouble("tempmin", 0.0);
                            double maxTemp = dayObj.optDouble("tempmax", 0.0);
                            double windSpeed = dayObj.optDouble("windspeed", 0.0);
                            double precip = dayObj.optDouble("precip", 0.0);
                            double rainProb = dayObj.optDouble("precipprob", 0.0);
                            String desc = dayObj.optString("conditions", "Parçalı Bulutlu");
                            String icon = dayObj.optString("icon", "02d");

                            ForecastItem item = new ForecastItem(
                                    date, temp, desc, icon, 0, rainProb, precip, windSpeed, minTemp, maxTemp
                            );
                            list.add(item);
                        }
                        callback.onSuccess(list);
                    } catch (JSONException e) {
                        callback.onError("Visual Crossing günlük verisi işlenemedi.");
                    }
                },
                error -> callback.onError("Visual Crossing bağlantı hatası.")
        );

        requestQueue.add(request);
    }

    // 3. SAATLİK TAHMİN
    public void getHourlyForecast(String cityName, final ForecastCallback callback) {
        String url = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/"
                + cityName.trim() + "?unitGroup=metric&key=" + API_KEY + "&contentType=json";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        List<ForecastItem> list = new ArrayList<>();
                        JSONArray days = response.getJSONArray("days");

                        for (int i = 0; i < Math.min(days.length(), 2); i++) {
                            JSONObject dayObj = days.getJSONObject(i);
                            String dateStr = dayObj.optString("datetime", "");
                            JSONArray hours = dayObj.getJSONArray("hours");

                            for (int j = 0; j < hours.length(); j++) {
                                JSONObject hourObj = hours.getJSONObject(j);
                                String rawTime = hourObj.optString("datetime", "00:00:00");
                                String timeStr = dateStr + " " + (rawTime.length() >= 5 ? rawTime.substring(0, 5) : rawTime);
                                double temp = hourObj.optDouble("temp", 0.0);
                                double humidity = hourObj.optDouble("humidity", 0.0);
                                double wind = hourObj.optDouble("windspeed", 0.0);
                                double precip = hourObj.optDouble("precip", 0.0);
                                double rainProb = hourObj.optDouble("precipprob", 0.0);
                                String desc = hourObj.optString("conditions", "Parçalı Bulutlu");
                                String icon = hourObj.optString("icon", "02d");

                                ForecastItem item = new ForecastItem(
                                        timeStr, temp, desc, icon, humidity, rainProb, precip, wind, temp, temp
                                );
                                list.add(item);
                            }
                        }
                        callback.onSuccess(list);
                    } catch (JSONException e) {
                        callback.onError("Visual Crossing saatlik verisi işlenemedi.");
                    }
                },
                error -> callback.onError("Visual Crossing bağlantı hatası.")
        );

        requestQueue.add(request);
    }
}