package com.example.weatherapp.Helpers.WeatherReceiver;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.weatherapp.Helpers.ForecastItem;
import com.example.weatherapp.Helpers.GeminiHelperPackages.GeminiPrompter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OpenMeteoService {

    private static final String TAG = "OpenMeteoService";
    private final RequestQueue requestQueue;

    // --- CONSTRUCTOR ---
    public OpenMeteoService(Context context) {
        this.requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    // --- MODEL SINIFLARI & INTERFACE'LER ---
    public static class GeoSearchResult {
        public String name;
        public String admin1; // Eyalet/Bölge
        public String country; // Ülke
        public double latitude;
        public double longitude;

        @NonNull
        @Override
        public String toString() {
            if (admin1 != null && !admin1.isEmpty()) {
                return name + ", " + admin1 + " (" + country + ")";
            }
            return name + " (" + country + ")";
        }
    }

    public interface GeoSearchCallback {
        void onSuccess(List<GeoSearchResult> results);
        void onError(String error);
    }

    public interface WeatherCallback {
        void onSuccess(WeatherData data);
        void onError(String error);
    }

    public static class WeatherData {
        public String AIAdvice;
        public String cityName;
        public double temp;
        public int humidity;
        public int pressure;
        public String description;
        public String icon;
        public double windSpeed;
        public float windDirection;
        public double cloudiness;
    }

    // --- METOTLAR ---

    // 1. Open-Meteo Geocoding API ile konum araması
    public void searchGlobalLocation(String query, final GeoSearchCallback callback) {
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + query + "&count=5&language=tr&format=json";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        List<GeoSearchResult> resultsList = new ArrayList<>();
                        if (response.has("results")) {
                            JSONArray results = response.getJSONArray("results");
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject obj = results.getJSONObject(i);
                                GeoSearchResult item = new GeoSearchResult();
                                item.name = obj.optString("name");
                                item.admin1 = obj.optString("admin1", "");
                                item.country = obj.optString("country", "");
                                item.latitude = obj.optDouble("latitude");
                                item.longitude = obj.optDouble("longitude");
                                resultsList.add(item);
                            }
                        }
                        callback.onSuccess(resultsList);
                    } catch (JSONException e) {
                        callback.onError("Geocoding verisi işlenemedi");
                    }
                },
                error -> callback.onError("Arama hatası")
        );

        requestQueue.add(request);
    }

    // 2. Open-Meteo üzerinden hava durumu çekme
    public void getWeatherFromOpenMeteo(double lat, double lon, String cityName, boolean isAIactive, final WeatherCallback callback) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon +
                "&current=temperature_2m,relative_humidity_2m,pressure_msl,wind_speed_10m,wind_direction_10m,cloud_cover,weather_code&wind_speed_unit=ms"+
                "&models=ecmwf_ifs025" + // 👈 ECMWF IFS 0.25° Resmi Modelini Zorlar
                "&timezone=auto";;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        WeatherData data = new WeatherData();
                        data.cityName = cityName;

                        JSONObject current = response.getJSONObject("current");
                        data.temp = current.getDouble("temperature_2m");
                        data.humidity = current.getInt("relative_humidity_2m");
                        data.pressure = (int) current.getDouble("pressure_msl");
                        data.windSpeed = current.getDouble("wind_speed_10m");
                        data.windDirection = (float) current.getDouble("wind_direction_10m");
                        data.cloudiness = current.getDouble("cloud_cover");

                        int weatherCode = current.getInt("weather_code");
                        data.description = getWeatherDescriptionFromCode(weatherCode);
                        data.icon = getWeatherIconFromCode(weatherCode);

                        if (isAIactive) {
                            new Thread(() -> {
                                try {
                                    GeminiPrompter aiProvider = new GeminiPrompter();
                                    data.AIAdvice = aiProvider.getWeatherAdvice(data.cityName, data.temp, data.description, data.icon);
                                } catch (Exception e) {
                                    Log.e(TAG, "AI Hatası: " + e);
                                } finally {
                                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(data));
                                }
                            }).start();
                        } else {
                            callback.onSuccess(data);
                        }
                    } catch (JSONException e) {
                        callback.onError("Open-Meteo verisi işlenemedi.");
                    }
                },
                error -> callback.onError("Open-Meteo bağlantı hatası")
        );

        requestQueue.add(request);
    }

    // WMO Hava Durumu Kod Dönüştürücüleri
    // OpenMeteoService.java içine ekleyin/güncelleyin:

    private String getWeatherDescriptionFromCode(int code) {
        if (code == 0) return "Açık";
        if (code == 1) return "Açık";
        if (code == 2) return "Parçalı Bulutlu";
        if (code == 3) return "Çok Bulutlu";
        if (code >= 45 && code <= 48) return "Sisli";
        if (code >= 51 && code <= 67) return "Yağmurlu";
        if (code >= 71 && code <= 77) return "Kar Yağışlı";
        if (code >= 80 && code <= 82) return "Sağanak Yağışlı";
        if (code >= 95) return "Gökgürültülü Fırtına";
        return "Parçalı Bulutlu";
    }

    private String getWeatherIconFromCode(int code) {
        if (code == 0 || code == 1) return "01d";
        if (code == 2) return "02d";
        if (code == 3) return "04d";
        if (code >= 45 && code <= 48) return "50d";
        if (code >= 51 && code <= 67) return "10d";
        if (code >= 71 && code <= 77) return "13d";
        if (code >= 80 && code <= 82) return "09d";
        if (code >= 95) return "11d";
        return "02d";
    }
    // --- SAATLİK VE GÜNLÜK TAHMİN MODELLERİ & INTERFACE'LERİ ---

    public interface ForecastCallback {
        void onSuccess(List<ForecastItem> forecastList);
        void onError(String error);
    }

    // 1. Open-Meteo Saatlik Tahmin (72 Saatlik)
    public void getHourlyForecast(double lat, double lon, final ForecastCallback callback) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon +
                "&hourly=temperature_2m,relative_humidity_2m,precipitation_probability,precipitation,weather_code,wind_speed_10m&forecast_days=3&wind_speed_unit=ms"+
        "&models=ecmwf_ifs025" + // 👈 ECMWF IFS 0.25° Resmi Modelini Zorlar
                "&timezone=auto";;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        List<ForecastItem> list = new ArrayList<>();
                        JSONObject hourly = response.getJSONObject("hourly");

                        JSONArray timeArray = hourly.getJSONArray("time");
                        JSONArray tempArray = hourly.getJSONArray("temperature_2m");
                        JSONArray humidityArray = hourly.getJSONArray("relative_humidity_2m");
                        JSONArray precipation_propailityArray=hourly.getJSONArray("precipitation_probability");
                        JSONArray precipationArray=hourly.getJSONArray("precipitation");
                        JSONArray windArray = hourly.getJSONArray("wind_speed_10m");
                        JSONArray codeArray = hourly.getJSONArray("weather_code");

                        for (int i = 0; i < timeArray.length(); i++) {
                            String rawTime = timeArray.getString(i); // Örn: 2026-08-11T15:00
                            String formattedTime = rawTime.replace("T", " ");

                            double temp = tempArray.getDouble(i);
                            int humidity = humidityArray.getInt(i);
                            double precipation_probability=precipation_propailityArray.getDouble(i);
                            double precipation=precipationArray.getDouble(i);
                            double windSpeed = windArray.getDouble(i) * 3.6; // m/s -> km/h
                            int code = codeArray.getInt(i);

                            String desc = getWeatherDescriptionFromCode(code);
                            String icon = getWeatherIconFromCode(code);

                            ForecastItem item = new ForecastItem(
                                    formattedTime, temp, desc, icon, humidity, precipation_probability, precipation, windSpeed, temp, temp
                            );
                            list.add(item);
                        }
                        callback.onSuccess(list);
                    } catch (JSONException e) {
                        callback.onError("Open-Meteo saatlik tahmin işlenemedi.");
                    }
                },
                error -> callback.onError("Saatlik tahmin verisi alınamadı.")
        );

        requestQueue.add(request);
    }

    // 2. Open-Meteo Günlük Tahmin (7 Günlük)
    public void getDailyForecast(double lat, double lon, final ForecastCallback callback) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon +
                "&daily=weather_code,temperature_2m_max,precipitation_sum,precipitation_probability_max,temperature_2m_min,wind_speed_10m_max&forecast_days=7&wind_speed_unit=ms";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        List<ForecastItem> list = new ArrayList<>();
                        JSONObject daily = response.getJSONObject("daily");

                        JSONArray timeArray = daily.getJSONArray("time");
                        JSONArray maxTempArray = daily.getJSONArray("temperature_2m_max");
                        JSONArray minTempArray = daily.getJSONArray("temperature_2m_min");
                        JSONArray windArray = daily.getJSONArray("wind_speed_10m_max");
                        JSONArray codeArray = daily.getJSONArray("weather_code");
                        JSONArray precipitaion_sumArray= daily.getJSONArray("precipitation_sum");
                        JSONArray precipitation_probability_maxArray=daily.getJSONArray("precipitation_probability_max");

                        for (int i = 0; i < timeArray.length(); i++) {
                            String date = timeArray.getString(i); // Örn: 2026-08-11
                            double minTemp = minTempArray.getDouble(i);
                            double maxTemp = maxTempArray.getDouble(i);
                            double precipitaion_sum=precipitaion_sumArray.getDouble(i);
                            double precipation_probability_max=precipitation_probability_maxArray.getDouble(i);
                            double avgTemp = (minTemp + maxTemp) / 2;
                            double windSpeed = windArray.getDouble(i) * 3.6;
                            int code = codeArray.getInt(i);

                            String desc = getWeatherDescriptionFromCode(code);
                            String icon = getWeatherIconFromCode(code);

                            ForecastItem item = new ForecastItem(
                                    date,
                                    avgTemp,
                                    desc,
                                    icon,
                                    0,
                                    precipation_probability_max,
                                    precipitaion_sum,
                                    windSpeed,
                                    minTemp,
                                    maxTemp
                            );
                            list.add(item);
                        }
                        callback.onSuccess(list);
                    } catch (JSONException e) {
                        callback.onError("Open-Meteo günlük tahmin işlenemedi.");
                    }
                },
                error -> callback.onError("Günlük tahmin verisi alınamadı.")
        );

        requestQueue.add(request);
    }
}