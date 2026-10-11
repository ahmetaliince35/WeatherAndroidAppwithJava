package com.example.weatherapp.Helpers.WeatherReceiver;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.weatherapp.Helpers.ForecastItem;
import com.example.weatherapp.data.URL_API;
import com.example.weatherapp.generalFeatures.PreferencesManager;
import com.example.weatherapp.network.ApiClient;
import com.example.weatherapp.network.NetworkErrorHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class WeatherJsonAPI implements WeatherProvider {

    private static final String TAG = "WeatherJsonAPI";
    private static final String API_KEY = URL_API.API_KEY;
    private final RequestQueue requestQueue;
    private final PreferencesManager preferencesManager;

    public WeatherJsonAPI(Context context) {
        this.requestQueue = ApiClient.getInstance(context).getRequestQueue();
        this.preferencesManager = PreferencesManager.getInstance(context.getApplicationContext());
    }

    @Override
    public String getProviderKey() {
        return "OWM";
    }

    @Override
    public String getDisplayName() {
        return "OpenWeatherMap";
    }

    public static class WeatherDataLegacy {
        public String AIAdvice;
        public String cityName;
        public double temp;
        public int humidity;
        public double feelsLike;
        public int pressure;
        public String description;
        public String icon;
        public double windSpeed;
        public float windDirection;
        public double cloudiness;
    }

    // For backwards compatibility
    public static class WeatherData extends WeatherDataLegacy {}

    public interface WeatherCallbackLegacy {
        void onSuccess(WeatherData data);
        void onError(String error);
    }

    public interface HourlyCallback {
        void onSuccess(List<ForecastItem> hourlyList, String json);
        void onError(String error);
    }

    public interface ForecastCallback {
        void onSuccess(List<ForecastItem> forecastList, String json);
        void onError(String error);
    }

    @Override
    public void getCurrentWeather(String cityName, String model, WeatherCallback<com.example.weatherapp.Helpers.WeatherReceiver.WeatherData> callback) {
        String cleanCity = cleanCityName(cityName);
        String url = URL_API.CurrentURL + cleanCity + "&appid=" + API_KEY + "&units=metric&lang=tr";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        com.example.weatherapp.Helpers.WeatherReceiver.WeatherData data =
                                new com.example.weatherapp.Helpers.WeatherReceiver.WeatherData();
                        data.cityName = response.getString("name");

                        JSONObject main = response.getJSONObject("main");
                        data.temp = main.getDouble("temp");
                        data.feelsLike = main.getDouble("feels_like");
                        data.humidity = main.getInt("humidity");
                        data.pressure = main.getInt("pressure");

                        JSONArray weatherArray = response.getJSONArray("weather");
                        JSONObject weather = weatherArray.getJSONObject(0);
                        data.description = weather.getString("description");
                        data.icon = weather.getString("icon");

                        JSONObject wind = response.getJSONObject("wind");
                        data.windSpeed = wind.getDouble("speed") * 3.6; // m/s -> km/h
                        data.windDirection = (float) wind.optDouble("deg", 0.0);

                        JSONObject cloud = response.optJSONObject("clouds");
                        data.cloudiness = cloud != null ? cloud.optDouble("all", 0.0) : 0.0;
                        data.aiAdvice = "";

                        callback.onSuccess(data);
                    } catch (JSONException e) {
                        callback.onError("OpenWeatherMap verisi ayrıştırılamadı: " + e.getMessage());
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
        String url = URL_API.ForecastURL + cleanCity + "&appid=" + API_KEY + "&units=metric&lang=tr&cnt=40";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        List<ForecastItem> forecastList = parseDailyForecast(response);
                        preferencesManager.saveDailyForecastJson(response.toString());
                        callback.onSuccess(forecastList);
                    } catch (JSONException e) {
                        callback.onError("Tahmin verisi işlenirken hata oluştu.");
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
        String url = URL_API.ForecastURL + cleanCity + "&appid=" + API_KEY + "&units=metric&lang=tr&cnt=24";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        List<ForecastItem> hourlyList = parseHourlyForecast(response);
                        preferencesManager.saveHourlyForecastJson(response.toString());
                        callback.onSuccess(hourlyList);
                    } catch (JSONException e) {
                        callback.onError("Saatlik tahmin işlenirken hata oluştu.");
                    }
                },
                error -> callback.onError(NetworkErrorHandler.formatVolleyError(error))
        );

        request.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
        requestQueue.add(request);
    }

    // Legacy method for existing callers
    public void getWeatherfromOpenWEatherMAp(String cityName, boolean isAIactive, final WeatherCallbackLegacy callback) {
        getCurrentWeather(cityName, "", new WeatherCallback<com.example.weatherapp.Helpers.WeatherReceiver.WeatherData>() {
            @Override
            public void onSuccess(com.example.weatherapp.Helpers.WeatherReceiver.WeatherData data) {
                WeatherData legacy = new WeatherData();
                legacy.cityName = data.cityName;
                legacy.temp = data.temp;
                legacy.feelsLike = data.feelsLike;
                legacy.humidity = data.humidity;
                legacy.pressure = data.pressure;
                legacy.description = data.description;
                legacy.icon = data.icon;
                legacy.windSpeed = data.windSpeed / 3.6; // keep legacy in m/s if expected
                legacy.windDirection = data.windDirection;
                legacy.cloudiness = data.cloudiness;
                legacy.AIAdvice = data.aiAdvice;
                callback.onSuccess(legacy);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public List<ForecastItem> getCachedHourlyForecast() {
        try {
            String cached = preferencesManager.getHourlyForecastJson();
            if (cached == null || cached.isEmpty()) return null;
            return parseHourlyForecast(new JSONObject(cached));
        } catch (Exception e) {
            Log.e(TAG, "getCachedHourlyForecast error: " + e.getMessage());
            return null;
        }
    }

    public List<ForecastItem> getCachedDailyForecast() {
        try {
            String cached = preferencesManager.getDailyForecastJson();
            if (cached == null || cached.isEmpty()) return null;
            return parseDailyForecast(new JSONObject(cached));
        } catch (Exception e) {
            Log.e(TAG, "getCachedDailyForecast error: " + e.getMessage());
            return null;
        }
    }

    private List<ForecastItem> parseHourlyForecast(JSONObject response) throws JSONException {
        JSONArray list = response.getJSONArray("list");
        List<ForecastItem> hourlyList = new ArrayList<>();

        for (int i = 0; i < list.length(); i++) {
            JSONObject hourData = list.getJSONObject(i);

            long timestamp = hourData.getLong("dt");
            String dateHour = formatDate(timestamp);

            JSONObject main = hourData.getJSONObject("main");
            double temp = main.getDouble("temp");
            int humidity = main.getInt("humidity");
            double tempMin = main.getDouble("temp_min");
            double tempMax = main.getDouble("temp_max");

            JSONObject weather = hourData.getJSONArray("weather").getJSONObject(0);
            String description = weather.getString("description");
            String icon = weather.getString("icon");

            JSONObject wind = hourData.getJSONObject("wind");
            double windSpeed = wind.getDouble("speed");

            double rain = 0;
            if (hourData.has("rain")) {
                rain = hourData.getJSONObject("rain").optDouble("3h", 0);
            }
            if (hourData.has("snow")) {
                rain = hourData.getJSONObject("snow").optDouble("3h", 0);
            }

            double rainProb = hourData.optDouble("pop", 0) * 100;
            double pressure = main.optDouble("pressure", 1013.0);
            double cloudiness = 0;
            if (hourData.has("clouds")) {
                cloudiness = hourData.getJSONObject("clouds").optDouble("all", 0);
            }

            ForecastItem forecastItem = new ForecastItem(
                    dateHour, temp, description, icon, humidity,
                    rainProb, rain, windSpeed * 3.6, tempMin, tempMax, pressure, cloudiness
            );
            hourlyList.add(forecastItem);
        }
        return hourlyList;
    }

    private List<ForecastItem> parseDailyForecast(JSONObject response) throws JSONException {
        JSONArray list = response.getJSONArray("list");
        List<ForecastItem> forecastList = new ArrayList<>();

        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        String todayKey = formatDateKey(today.getTimeInMillis() / 1000);

        Map<String, DailyData> dailyDataMap = new HashMap<>();

        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.getJSONObject(i);
            long timestamp = item.getLong("dt");
            String dateTime = item.getString("dt_txt");
            String dayKey = formatDateKey(timestamp);
            String displayDate = formatDate(timestamp);

            JSONObject main = item.getJSONObject("main");
            double temp = main.getDouble("temp");
            double tempMin = main.getDouble("temp_min");
            double tempMax = main.getDouble("temp_max");
            int humidity = main.getInt("humidity");

            JSONObject weather = item.getJSONArray("weather").getJSONObject(0);
            String description = weather.getString("description");
            String icon = weather.getString("icon");

            JSONObject wind = item.getJSONObject("wind");
            double windSpeed = wind.getDouble("speed");

            double pop = item.has("pop") ? item.getDouble("pop") * 100 : 0;
            double rainVolume = 0;
            if (item.has("rain")) rainVolume = item.getJSONObject("rain").optDouble("3h", 0);
            if (item.has("snow")) rainVolume += item.getJSONObject("snow").optDouble("3h", 0);

            if (!dailyDataMap.containsKey(dayKey)) {
                dailyDataMap.put(dayKey, new DailyData(displayDate, dayKey));
            }

            DailyData dailyData = dailyDataMap.get(dayKey);
            if (dailyData != null) {
                if (dateTime.contains("12:00:00")) {
                    dailyData.noonTemp = temp;
                    dailyData.noonIcon = icon;
                    dailyData.noonDescription = description;
                }

                dailyData.minTemp = Math.min(dailyData.minTemp, tempMin);
                dailyData.maxTemp = Math.max(dailyData.maxTemp, tempMax);
                dailyData.totalHumidity += humidity;
                dailyData.totalWindSpeed += windSpeed;
                dailyData.totalRainVolume += rainVolume;
                dailyData.maxRainProbability = Math.max(dailyData.maxRainProbability, pop);
                dailyData.totalPressure += main.optDouble("pressure", 1013.0);
                if (item.has("clouds")) {
                    dailyData.totalCloudiness += item.getJSONObject("clouds").optDouble("all", 0);
                }
                dailyData.count++;
            }
        }

        List<DailyData> sortedDailyData = new ArrayList<>(dailyDataMap.values());
        sortedDailyData.sort((a, b) -> a.dayKey.compareTo(b.dayKey));

        for (DailyData dailyData : sortedDailyData) {
            if (dailyData.dayKey.equals(todayKey)) continue;
            if (dailyData.noonIcon == null || dailyData.noonDescription == null) continue;

            ForecastItem forecastItem = new ForecastItem(
                    dailyData.displayDate, dailyData.noonTemp, dailyData.noonDescription,
                    dailyData.noonIcon, dailyData.getAverageHumidity(), dailyData.maxRainProbability,
                    dailyData.totalRainVolume, dailyData.getAverageWindSpeed() * 3.6, dailyData.minTemp,
                    dailyData.maxTemp, dailyData.getAveragePressure(), dailyData.getAverageCloudiness()
            );
            forecastList.add(forecastItem);
            if (forecastList.size() >= 5) break;
        }

        return forecastList;
    }

    private String formatDate(long timestamp) {
        Date date = new Date(timestamp * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH.mm - EE", new Locale("tr", "TR"));
        return sdf.format(date);
    }

    private String formatDateKey(long timestamp) {
        Date date = new Date(timestamp * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    private String cleanCityName(String cityName) {
        if (cityName == null) return "";
        String clean = cityName.trim();
        if (clean.contains("/")) clean = clean.split("/")[0].trim();
        if (clean.contains(",")) clean = clean.split(",")[0].trim();
        return clean;
    }

    private static class DailyData {
        String displayDate;
        String dayKey;
        String noonIcon = null;
        String noonDescription = null;
        double noonTemp = 0;
        double minTemp = Double.POSITIVE_INFINITY;
        double maxTemp = Double.NEGATIVE_INFINITY;
        int totalHumidity = 0;
        double totalWindSpeed = 0;
        double totalRainVolume = 0;
        double maxRainProbability = 0;
        double totalPressure = 0;
        double totalCloudiness = 0;
        int count = 0;

        DailyData(String displayDate, String dayKey) {
            this.displayDate = displayDate;
            this.dayKey = dayKey;
        }

        int getAverageHumidity() { return count > 0 ? totalHumidity / count : 0; }
        double getAverageWindSpeed() { return count > 0 ? totalWindSpeed / count : 0; }
        double getAveragePressure() { return count > 0 ? totalPressure / count : 1013.0; }
        double getAverageCloudiness() { return count > 0 ? totalCloudiness / count : 0; }
    }
}