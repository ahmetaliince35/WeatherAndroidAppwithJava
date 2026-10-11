package com.example.weatherapp.Helpers.WeatherReceiver;

import android.content.Context;
import android.net.Uri;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.weatherapp.BuildConfig;
import com.example.weatherapp.Helpers.ForecastItem;
import com.example.weatherapp.network.ApiClient;
import com.example.weatherapp.network.NetworkErrorHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeatherAPIService implements WeatherProvider {

    private static final String API_KEY = BuildConfig.weatherAPIKey;
    private final RequestQueue requestQueue;

    public WeatherAPIService(Context context) {
        this.requestQueue = ApiClient.getInstance(context).getRequestQueue();
    }

    @Override
    public String getProviderKey() {
        return "WEATHER_API";
    }

    @Override
    public String getDisplayName() {
        return "WeatherAPI";
    }

    public interface WeatherAPICallback {
        void onSuccess(List<ForecastItem> forecastList);
        void onError(String error);
    }

    public interface WeatherCallbackLegacy {
        void onSuccess(WeatherJsonAPI.WeatherData data);
        void onError(String error);
    }

    @Override
    public void getCurrentWeather(String cityName, String model, WeatherCallback<WeatherData> callback) {
        String cleanCity = cleanCityName(cityName);
        String url = "https://api.weatherapi.com/v1/current.json?key=" + API_KEY +
                "&q=" + Uri.encode(cleanCity) + "&aqi=no&lang=tr";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        WeatherData data = new WeatherData();
                        JSONObject location = response.getJSONObject("location");
                        JSONObject current = response.getJSONObject("current");

                        data.cityName = location.getString("name");
                        data.temp = current.getDouble("temp_c");
                        data.feelsLike = current.optDouble("feelslike_c", data.temp);
                        data.humidity = current.getInt("humidity");
                        data.pressure = (int) current.getDouble("pressure_mb");
                        data.windSpeed = current.getDouble("wind_kph"); // km/h
                        data.windDirection = (float) current.getDouble("wind_degree");
                        data.cloudiness = current.getDouble("cloud");

                        JSONObject condition = current.getJSONObject("condition");
                        data.description = condition.getString("text");
                        data.icon = condition.getString("icon");
                        data.aiAdvice = "";

                        callback.onSuccess(data);
                    } catch (JSONException e) {
                        callback.onError("WeatherAPI anlık veri işlenemedi.");
                    }
                },
                error -> callback.onError(NetworkErrorHandler.formatVolleyError(error))
        );

        request.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
        requestQueue.add(request);
    }

    @Override
    public void getDailyForecast(String cityName, String model, WeatherCallback<List<ForecastItem>> callback) {
        String cleanCity = cleanCityName(cityName);
        String url = "https://api.weatherapi.com/v1/forecast.json?key=" + API_KEY +
                "&q=" + Uri.encode(cleanCity) + "&days=5&aqi=no&alerts=no&lang=tr";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        List<ForecastItem> list = new ArrayList<>();
                        JSONArray forecastDay = response.getJSONObject("forecast").getJSONArray("forecastday");

                        for (int i = 0; i < Math.min(forecastDay.length(), 5); i++) {
                            JSONObject dayObj = forecastDay.getJSONObject(i);
                            String date = dayObj.getString("date");
                            JSONObject day = dayObj.getJSONObject("day");

                            double avgTemp = day.getDouble("avgtemp_c");
                            double minTemp = day.getDouble("mintemp_c");
                            double maxTemp = day.getDouble("maxtemp_c");
                            double maxWind = day.getDouble("maxwind_kph");
                            double totalPrecip = day.getDouble("totalprecip_mm");
                            double rainProb = day.optDouble("daily_chance_of_rain", 0);
                            double humidity = day.optDouble("avghumidity", 0);
                            double pressure = day.optDouble("pressure_mb", 1013.0);
                            double cloudiness = day.optDouble("cloud", 0);

                            JSONObject condition = day.getJSONObject("condition");
                            String desc = condition.getString("text");
                            String icon = condition.getString("icon");

                            ForecastItem item = new ForecastItem(
                                    date, avgTemp, desc, icon, humidity, rainProb,
                                    totalPrecip, maxWind, minTemp, maxTemp, pressure, cloudiness
                            );
                            list.add(item);
                        }
                        callback.onSuccess(list);
                    } catch (JSONException e) {
                        callback.onError("WeatherAPI günlük verisi işlenemedi.");
                    }
                },
                error -> callback.onError(NetworkErrorHandler.formatVolleyError(error))
        );

        request.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
        requestQueue.add(request);
    }

    @Override
    public void getHourlyForecast(String cityName, String model, WeatherCallback<List<ForecastItem>> callback) {
        String cleanCity = cleanCityName(cityName);
        String url = "https://api.weatherapi.com/v1/forecast.json?key=" + API_KEY +
                "&q=" + Uri.encode(cleanCity) + "&days=3&aqi=no&alerts=no&lang=tr";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        List<ForecastItem> list = new ArrayList<>();
                        JSONArray forecastDay = response.getJSONObject("forecast").getJSONArray("forecastday");

                        long nowThreshold = System.currentTimeMillis() - (50 * 60 * 1000L);
                        SimpleDateFormat sdfParser = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

                        for (int i = 0; i < forecastDay.length(); i++) {
                            JSONObject dayObj = forecastDay.getJSONObject(i);
                            JSONArray hourArray = dayObj.getJSONArray("hour");

                            for (int j = 0; j < hourArray.length(); j++) {
                                JSONObject hour = hourArray.getJSONObject(j);
                                String time = hour.getString("time");

                                try {
                                    Date d = sdfParser.parse(time);
                                    if (d != null && d.getTime() < nowThreshold) {
                                        continue;
                                    }
                                } catch (Exception ignored) {}

                                double temp = hour.getDouble("temp_c");
                                double humidity = hour.getDouble("humidity");
                                double wind = hour.getDouble("wind_kph");
                                double precip = hour.getDouble("precip_mm");
                                double rainProb = hour.optDouble("chance_of_rain", 0);
                                double pressure = hour.optDouble("pressure_mb", 1013.0);
                                double cloudiness = hour.optDouble("cloud", 0);

                                JSONObject condition = hour.getJSONObject("condition");
                                String desc = condition.getString("text");
                                String icon = condition.getString("icon");

                                ForecastItem item = new ForecastItem(
                                        time, temp, desc, icon, humidity, rainProb,
                                        precip, wind, temp, temp, pressure, cloudiness
                                );
                                list.add(item);
                                if (list.size() >= 72) break;
                            }
                            if (list.size() >= 72) break;
                        }
                        callback.onSuccess(list);
                    } catch (JSONException e) {
                        callback.onError("WeatherAPI saatlik verisi işlenemedi.");
                    }
                },
                error -> callback.onError(NetworkErrorHandler.formatVolleyError(error))
        );

        request.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
        requestQueue.add(request);
    }

    // Legacy method
    public void getCurrentWeather(String cityName, boolean isAIactive, final WeatherCallbackLegacy callback) {
        getCurrentWeather(cityName, "", new WeatherCallback<WeatherData>() {
            @Override
            public void onSuccess(WeatherData data) {
                WeatherJsonAPI.WeatherData legacy = new WeatherJsonAPI.WeatherData();
                legacy.cityName = data.cityName;
                legacy.temp = data.temp;
                legacy.feelsLike = data.feelsLike;
                legacy.humidity = data.humidity;
                legacy.pressure = data.pressure;
                legacy.windSpeed = data.windSpeed / 3.6;
                legacy.windDirection = data.windDirection;
                legacy.cloudiness = data.cloudiness;
                legacy.description = data.description;
                legacy.icon = data.icon;
                legacy.AIAdvice = data.aiAdvice;
                callback.onSuccess(legacy);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getDailyForecast(String cityName, final WeatherAPICallback callback) {
        getDailyForecast(cityName, "", new WeatherCallback<List<ForecastItem>>() {
            @Override
            public void onSuccess(List<ForecastItem> data) {
                callback.onSuccess(data);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getHourlyForecast(String cityName, final WeatherAPICallback callback) {
        getHourlyForecast(cityName, "", new WeatherCallback<List<ForecastItem>>() {
            @Override
            public void onSuccess(List<ForecastItem> data) {
                callback.onSuccess(data);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private String cleanCityName(String cityName) {
        if (cityName == null) return "";
        String clean = cityName.trim();
        if (clean.contains("/")) clean = clean.split("/")[0].trim();
        if (clean.contains(",")) clean = clean.split(",")[0].trim();
        return clean;
    }
}