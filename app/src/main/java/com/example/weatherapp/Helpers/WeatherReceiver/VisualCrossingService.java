package com.example.weatherapp.Helpers.WeatherReceiver;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    private String buildUrl(String cityName, String model) {
        String encodedCity = Uri.encode(cityName.trim());
        String url = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/"
                + encodedCity + "?unitGroup=metric&key=" + API_KEY + "&contentType=json";
        if (model != null && !model.trim().isEmpty() && !model.equalsIgnoreCase("auto") && !model.equalsIgnoreCase("blended")) {
            String dataset = model.trim().toLowerCase();

            if (dataset.equals("ukmo")) {
                dataset = "ukmo_global";
            }
            url += "&forecastDataset=" + dataset;
        }
        return url;
    }


    public void getCurrentWeather(String cityName, String model, boolean isAIactive, final WeatherCallback callback) {
        String url = buildUrl(cityName, model);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        WeatherJsonAPI.WeatherData data = new WeatherJsonAPI.WeatherData();
                        data.cityName = response.optString("address", cityName);

                        JSONObject current = response.optJSONObject("currentConditions");
                        if (current == null) {
                            JSONArray days = response.optJSONArray("days");
                            if (days != null && days.length() > 0) {
                                JSONObject firstDay = days.optJSONObject(0);
                                JSONArray hours = firstDay != null ? firstDay.optJSONArray("hours") : null;
                                if (hours != null && hours.length() > 0) {
                                    current = hours.optJSONObject(0);
                                } else {
                                    current = firstDay;
                                }
                            }
                        }

                        if (current != null) {
                            data.temp = current.optDouble("temp", 0.0);
                            data.humidity = current.optInt("humidity", 0);
                            data.pressure = (int) current.optDouble("pressure", 1013.0);

                            double windKph = current.optDouble("windspeed", 0.0);
                            data.windSpeed = windKph / 3.6; // km/h -> m/s

                            data.windDirection = (float) current.optDouble("winddir", 0.0);
                            data.cloudiness = current.optDouble("cloudcover", 0.0);
                            data.description = current.optString("conditions", "Parçalı Bulutlu");
                            data.icon = current.optString("icon", "02d");
                        }

                        data.AIAdvice = "";
                        callback.onSuccess(data);

                    } catch (Exception e) {
                        Log.e(TAG, "JSON Parsing Hatası: " + e.getMessage());
                        callback.onError("Visual Crossing verisi işlenemedi.");
                    }
                },
                error -> {
                    Log.e(TAG, "Volley Hatası: " + error.toString());
                    callback.onError("Visual Crossing bağlantı hatası. API Key kontrol edin.");
                }
        );
        request.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
        requestQueue.add(request);
    }


    public void getDailyForecast(String cityName, String model, final ForecastCallback callback) {
        String url = buildUrl(cityName, model);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        List<ForecastItem> list = new ArrayList<>();
                        JSONArray days = response.optJSONArray("days");
                        if (days == null) {
                            callback.onError("Visual Crossing günlük verisi bulunamadı.");
                            return;
                        }

                        for (int i = 0; i < Math.min(days.length(), 5); i++) {
                            JSONObject dayObj = days.optJSONObject(i);
                            if (dayObj == null) continue;
                            String date = dayObj.optString("datetime", "");
                            double temp = dayObj.optDouble("temp", 0.0);
                            double minTemp = dayObj.optDouble("tempmin", 0.0);
                            double maxTemp = dayObj.optDouble("tempmax", 0.0);
                            double windSpeed = dayObj.optDouble("windspeed", 0.0);
                            double precip = dayObj.optDouble("precip", 0.0);
                            double rainProb = dayObj.optDouble("precipprob", 0.0);
                            double pressure = dayObj.optDouble("pressure", 1013.0);
                            double cloudCover = dayObj.optDouble("cloudcover", 0.0);
                            double humidity = dayObj.optDouble("humidity", 0.0);
                            String desc = dayObj.optString("conditions", "Parçalı Bulutlu");
                            String icon = dayObj.optString("icon", "02d");

                            ForecastItem item = new ForecastItem(
                                    date, temp, desc, icon, humidity, rainProb, precip, windSpeed, minTemp, maxTemp, pressure, cloudCover
                            );
                            list.add(item);
                        }
                        callback.onSuccess(list);
                    } catch (Exception e) {
                        callback.onError("Visual Crossing günlük verisi işlenemedi.");
                    }
                },
                error -> callback.onError("Visual Crossing bağlantı hatası.")
        );
        request.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
        requestQueue.add(request);
    }


    private String buildHourlyUrl(String cityName, String model) {
        String encodedCity = Uri.encode(cityName.trim());
        String url = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/"
                + encodedCity + "/next72hours?unitGroup=metric&key=" + API_KEY + "&contentType=json&include=hours";
        if (model != null && !model.trim().isEmpty() && !model.equalsIgnoreCase("auto") && !model.equalsIgnoreCase("blended")) {
            String dataset = model.trim().toLowerCase();

            if (dataset.equals("ukmo")) {
                dataset = "ukmo_global";
            }
            url += "&forecastDataset=" + dataset;
        }
        return url;
    }

    public void getHourlyForecast(String cityName, String model, final ForecastCallback callback) {
        String url = buildHourlyUrl(cityName, model);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        List<ForecastItem> list = new ArrayList<>();
                        JSONArray days = response.optJSONArray("days");
                        if (days == null) {
                            callback.onError("Visual Crossing saatlik verisi bulunamadı.");
                            return;
                        }

                        long nowThreshold = System.currentTimeMillis() - (50 * 60 * 1000L);
                        SimpleDateFormat sdfParser = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

                        for (int i = 0; i < days.length(); i++) {
                            JSONObject dayObj = days.optJSONObject(i);
                            if (dayObj == null) continue;
                            String dateStr = dayObj.optString("datetime", "");
                            JSONArray hours = dayObj.optJSONArray("hours");
                            if (hours == null) continue;

                            for (int j = 0; j < hours.length(); j++) {
                                JSONObject hourObj = hours.optJSONObject(j);
                                if (hourObj == null) continue;
                                String rawTime = hourObj.optString("datetime", "00:00:00");
                                String timeStr = dateStr + " " + (rawTime.length() >= 5 ? rawTime.substring(0, 5) : rawTime);

                                // Geçmiş saatleri atla, o anki saatten itibaren başla
                                try {
                                    Date d = sdfParser.parse(timeStr);
                                    if (d != null && d.getTime() < nowThreshold) {
                                        continue;
                                    }
                                } catch (Exception ignored) {}

                                double temp = hourObj.optDouble("temp", 0.0);
                                double humidity = hourObj.optDouble("humidity", 0.0);
                                double wind = hourObj.optDouble("windspeed", 0.0);
                                double precip = hourObj.optDouble("precip", 0.0);
                                double rainProb = hourObj.optDouble("precipprob", 0.0);
                                double pressure = hourObj.optDouble("pressure", 1013.0);
                                double cloudCover = hourObj.optDouble("cloudcover", 0.0);
                                String desc = hourObj.optString("conditions", "Parçalı Bulutlu");
                                String icon = hourObj.optString("icon", "02d");

                                ForecastItem item = new ForecastItem(
                                        timeStr, temp, desc, icon, humidity, rainProb, precip, wind, temp, temp, pressure, cloudCover
                                );
                                list.add(item);
                                if (list.size() >= 72) break; // O anki saatten itibaren tam 72 saat
                            }
                            if (list.size() >= 72) break;
                        }
                        callback.onSuccess(list);
                    } catch (Exception e) {
                        callback.onError("Visual Crossing saatlik verisi işlenemedi.");
                    }
                },
                error -> callback.onError("Visual Crossing bağlantı hatası.")
        );
        request.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
        requestQueue.add(request);
    }
}