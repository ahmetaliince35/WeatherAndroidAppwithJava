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

public class WeatherAPIService {

    private static final String API_KEY = BuildConfig.weatherAPIKey ; // WeatherAPI Key'inizi buraya yazın
    private final RequestQueue requestQueue;

    public interface WeatherAPICallback {
        void onSuccess(List<ForecastItem> forecastList);
        void onError(String error);
    }

    public WeatherAPIService(Context context) {
        this.requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }
    public interface WeatherCallback {
        void onSuccess(WeatherJsonAPI.WeatherData data);
        void onError(String error);
    }

    // Anlık Hava Durumu Çekme Metodu
    public void getCurrentWeather(String cityName, boolean isAIactive, final WeatherCallback callback) {
        String url = "https://api.weatherapi.com/v1/current.json?key=" + API_KEY +
                "&q=" + cityName + "&aqi=no&lang=tr";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        WeatherJsonAPI.WeatherData data = new WeatherJsonAPI.WeatherData();
                        JSONObject location = response.getJSONObject("location");
                        JSONObject current = response.getJSONObject("current");

                        data.cityName = location.getString("name");
                        data.temp = current.getDouble("temp_c");
                        data.humidity = current.getInt("humidity");
                        data.pressure = (int) current.getDouble("pressure_mb");
                        data.windSpeed = current.getDouble("wind_kph") / 3.6; // km/h -> m/s (Main'de 3.6 ile çarpılıyor)
                        data.windDirection = (float) current.getDouble("wind_degree");
                        data.cloudiness = current.getDouble("cloud");

                        JSONObject condition = current.getJSONObject("condition");
                        data.description = condition.getString("text");
                        data.icon = condition.getString("icon");

                        if (isAIactive) {
                            new Thread(() -> {
                                try {
                                    GeminiPrompter aiProvider = new GeminiPrompter();
                                    data.AIAdvice = aiProvider.getWeatherAdvice(data.cityName, data.temp, data.description, data.icon);
                                } catch (Exception e) {
                                    Log.e("WeatherAPIService", "AI Hatası: " + e);
                                } finally {
                                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(data));
                                }
                            }).start();
                        } else {
                            callback.onSuccess(data);
                        }
                    } catch (JSONException e) {
                        callback.onError("WeatherAPI anlık veri işlenemedi.");
                    }
                },
                error -> callback.onError("WeatherAPI bağlantı hatası.")
        );

        requestQueue.add(request);
    }
    // 1. Günlük Tahmin (7 Günlük)
    public void getDailyForecast(String cityName, final WeatherAPICallback callback) {
        String url = "https://api.weatherapi.com/v1/forecast.json?key=" + API_KEY +
                "&q=" + cityName + "&days=7&aqi=no&alerts=no&lang=tr";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        List<ForecastItem> list = new ArrayList<>();
                        JSONArray forecastDay = response.getJSONObject("forecast").getJSONArray("forecastday");

                        for (int i = 0; i < forecastDay.length(); i++) {
                            JSONObject dayObj = forecastDay.getJSONObject(i);
                            String date = dayObj.getString("date");
                            JSONObject day = dayObj.getJSONObject("day");

                            double avgTemp = day.getDouble("avgtemp_c");
                            double minTemp = day.getDouble("mintemp_c");
                            double maxTemp = day.getDouble("maxtemp_c");
                            double maxWind = day.getDouble("maxwind_kph");
                            double totalPrecip = day.getDouble("totalprecip_mm");
                            double rainProb = day.optDouble("daily_chance_of_rain", 0);

                            JSONObject condition = day.getJSONObject("condition");
                            String desc = condition.getString("text");
                            String icon = condition.getString("icon");

                            ForecastItem item = new ForecastItem(
                                    date, avgTemp, desc, icon, 0, rainProb, totalPrecip, maxWind, minTemp, maxTemp
                            );
                            list.add(item);
                        }
                        callback.onSuccess(list);
                    } catch (JSONException e) {
                        callback.onError("WeatherAPI günlük verisi işlenemedi.");
                    }
                },
                error -> callback.onError("WeatherAPI bağlantı hatası.")
        );

        requestQueue.add(request);
    }

    // 2. Saatlik Tahmin (24-48 Saatlik)
    public void getHourlyForecast(String cityName, final WeatherAPICallback callback) {
        String url = "https://api.weatherapi.com/v1/forecast.json?key=" + API_KEY +
                "&q=" + cityName + "&days=3&aqi=no&alerts=no&lang=tr";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        List<ForecastItem> list = new ArrayList<>();
                        JSONArray forecastDay = response.getJSONObject("forecast").getJSONArray("forecastday");

                        for (int i = 0; i < forecastDay.length(); i++) {
                            JSONObject dayObj = forecastDay.getJSONObject(i);
                            JSONArray hourArray = dayObj.getJSONArray("hour");

                            for (int j = 0; j < hourArray.length(); j++) {
                                JSONObject hour = hourArray.getJSONObject(j);
                                String time = hour.getString("time");
                                double temp = hour.getDouble("temp_c");
                                double humidity = hour.getDouble("humidity");
                                double wind = hour.getDouble("wind_kph");
                                double precip = hour.getDouble("precip_mm");
                                double rainProb = hour.optDouble("chance_of_rain", 0);

                                JSONObject condition = hour.getJSONObject("condition");
                                String desc = condition.getString("text");
                                String icon = condition.getString("icon");

                                ForecastItem item = new ForecastItem(
                                        time, temp, desc, icon, humidity, rainProb, precip, wind, temp, temp
                                );
                                list.add(item);
                            }
                        }
                        callback.onSuccess(list);
                    } catch (JSONException e) {
                        callback.onError("WeatherAPI saatlik verisi işlenemedi.");
                    }
                },
                error -> callback.onError("WeatherAPI bağlantı hatası.")
        );

        requestQueue.add(request);
    }
}