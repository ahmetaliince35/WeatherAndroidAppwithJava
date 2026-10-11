package com.example.weatherapp.Helpers.WeatherReceiver;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
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

public class OpenMeteoService {

    private static final String TAG = "OpenMeteoService";
    private final RequestQueue requestQueue;
    public OpenMeteoService(Context context) {
        this.requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static class GeoSearchResult {
        public String name;
        public String admin1;
        public String country;
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


    public void searchGlobalLocation(String query, final GeoSearchCallback callback) {
        String cleanQuery = query != null ? query.trim() : "";
        if (cleanQuery.contains("/")) {
            cleanQuery = cleanQuery.split("/")[0].trim();
        }
        if (cleanQuery.contains(",")) {
            cleanQuery = cleanQuery.split(",")[0].trim();
        }
        String encodedQuery = Uri.encode(cleanQuery);
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedQuery + "&count=5&language=tr&format=json";

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
                    } catch (Exception e) {
                        callback.onError("Geocoding verisi işlenemedi");
                    }
                },
                error -> callback.onError("Arama hatası")
        );

        request.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
        requestQueue.add(request);
    }


    public void getWeatherFromOpenMeteo(double lat, double lon, String cityName, boolean isAIactive, final WeatherCallback callback) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon +
                "&current=temperature_2m,relative_humidity_2m,pressure_msl,wind_speed_10m,wind_direction_10m,cloud_cover,weather_code&wind_speed_unit=ms"+
                "&models=ecmwf_ifs025" +
                "&timezone=auto";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> parseWeatherDataResponse(response, cityName, isAIactive, callback),
                error -> {
                    // ECMWF modeli cevap vermezse varsayılan Open-Meteo tahminiyle yeniden dene (Fallback)
                    String fallbackUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon +
                            "&current=temperature_2m,relative_humidity_2m,pressure_msl,wind_speed_10m,wind_direction_10m,cloud_cover,weather_code&wind_speed_unit=ms" +
                            "&timezone=auto";

                    JsonObjectRequest fbRequest = new JsonObjectRequest(
                            Request.Method.GET,
                            fallbackUrl,
                            null,
                            fbResponse -> parseWeatherDataResponse(fbResponse, cityName, isAIactive, callback),
                            fbError -> callback.onError("Open-Meteo bağlantı hatası")
                    );
                    fbRequest.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
                    requestQueue.add(fbRequest);
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
        requestQueue.add(request);
    }

    private void parseWeatherDataResponse(JSONObject response, String cityName, boolean isAIactive, final WeatherCallback callback) {
        try {
            WeatherData data = new WeatherData();
            data.cityName = cityName;

            JSONObject current = response.optJSONObject("current");
            if (current != null) {
                data.temp = current.optDouble("temperature_2m", 0.0);
                data.humidity = current.optInt("relative_humidity_2m", 0);
                data.pressure = (int) current.optDouble("pressure_msl", 1013.0);
                data.windSpeed = current.optDouble("wind_speed_10m", 0.0);
                data.windDirection = (float) current.optDouble("wind_direction_10m", 0.0);
                data.cloudiness = current.optDouble("cloud_cover", 0.0);

                int weatherCode = current.optInt("weather_code", 0);
                data.description = getWeatherDescriptionFromCode(weatherCode);
                data.icon = getWeatherIconFromCode(weatherCode);
            }

            data.AIAdvice = "";
            callback.onSuccess(data);
        } catch (Exception e) {
            callback.onError("Open-Meteo verisi işlenemedi.");
        }
    }



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


    public interface ForecastCallback {
        void onSuccess(List<ForecastItem> forecastList);
        void onError(String error);
    }


    public void getHourlyForecast(double lat, double lon, final ForecastCallback callback) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon +
                "&hourly=temperature_2m,relative_humidity_2m,pressure_msl,cloud_cover,precipitation_probability,precipitation,weather_code,wind_speed_10m&forecast_days=5&wind_speed_unit=ms"+
                "&models=ecmwf_ifs025" +
                "&timezone=auto";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> parseHourlyForecastResponse(response, callback),
                error -> {

                    String fallbackUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon +
                            "&hourly=temperature_2m,relative_humidity_2m,pressure_msl,cloud_cover,precipitation_probability,precipitation,weather_code,wind_speed_10m&forecast_days=5&wind_speed_unit=ms" +
                            "&timezone=auto";
                    JsonObjectRequest fbRequest = new JsonObjectRequest(
                            Request.Method.GET,
                            fallbackUrl,
                            null,
                            fbResponse -> parseHourlyForecastResponse(fbResponse, callback),
                            fbError -> callback.onError("Saatlik tahmin verisi alınamadı.")
                    );
                    fbRequest.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
                    requestQueue.add(fbRequest);
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
        requestQueue.add(request);
    }

    private void parseHourlyForecastResponse(JSONObject response, ForecastCallback callback) {
        try {
            List<ForecastItem> list = new ArrayList<>();
            JSONObject hourly = response.optJSONObject("hourly");
            if (hourly == null) {
                callback.onError("Open-Meteo saatlik veri bulunamadı.");
                return;
            }

            JSONArray timeArray = hourly.optJSONArray("time");
            JSONArray tempArray = hourly.optJSONArray("temperature_2m");
            JSONArray humidityArray = hourly.optJSONArray("relative_humidity_2m");
            JSONArray pressureArray = hourly.optJSONArray("pressure_msl");
            JSONArray cloudArray = hourly.optJSONArray("cloud_cover");
            JSONArray precipation_propailityArray = hourly.optJSONArray("precipitation_probability");
            JSONArray precipationArray = hourly.optJSONArray("precipitation");
            JSONArray windArray = hourly.optJSONArray("wind_speed_10m");
            JSONArray codeArray = hourly.optJSONArray("weather_code");

            if (timeArray == null) {
                callback.onError("Open-Meteo saatlik zaman dizisi bulunamadı.");
                return;
            }

            long nowThreshold = System.currentTimeMillis() - (50 * 60 * 1000L);
            SimpleDateFormat sdfParser = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

            for (int i = 0; i < timeArray.length(); i++) {
                String rawTime = timeArray.optString(i, "");
                String formattedTime = rawTime.replace("T", " ");

                // Geçmiş saatleri atla, o anki saatten itibaren başla
                try {
                    Date d = sdfParser.parse(formattedTime.length() >= 16 ? formattedTime.substring(0, 16) : formattedTime);
                    if (d != null && d.getTime() < nowThreshold) {
                        continue;
                    }
                } catch (Exception ignored) {}

                double temp = tempArray != null ? tempArray.optDouble(i, 0.0) : 0.0;
                int humidity = humidityArray != null ? humidityArray.optInt(i, 0) : 0;
                double pressure = pressureArray != null ? pressureArray.optDouble(i, 1013.0) : 1013.0;
                double cloudiness = cloudArray != null ? cloudArray.optDouble(i, 0.0) : 0.0;
                double precipation_probability = precipation_propailityArray != null ? precipation_propailityArray.optDouble(i, 0.0) : 0.0;
                double precipation = precipationArray != null ? precipationArray.optDouble(i, 0.0) : 0.0;
                double windSpeed = (windArray != null ? windArray.optDouble(i, 0.0) : 0.0) * 3.6; // m/s -> km/h
                int code = codeArray != null ? codeArray.optInt(i, 2) : 2;

                String desc = getWeatherDescriptionFromCode(code);
                String icon = getWeatherIconFromCode(code);

                ForecastItem item = new ForecastItem(
                        formattedTime, temp, desc, icon, humidity, precipation_probability, precipation, windSpeed, temp, temp, pressure, cloudiness
                );
                list.add(item);
                if (list.size() >= 72) break; // O anki saatten itibaren tam 72 saat
            }
            callback.onSuccess(list);
        } catch (Exception e) {
            callback.onError("Open-Meteo saatlik tahmin işlenemedi.");
        }
    }


    public void getDailyForecast(double lat, double lon, final ForecastCallback callback) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon +
                "&daily=weather_code,temperature_2m_max,precipitation_sum,precipitation_probability_max,temperature_2m_min,wind_speed_10m_max&forecast_days=5&wind_speed_unit=ms" +
                "&models=ecmwf_ifs025" +
                "&timezone=auto";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> parseDailyForecastResponse(response, callback),
                error -> {
                    // Fallback
                    String fallbackUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon +
                            "&daily=weather_code,temperature_2m_max,precipitation_sum,precipitation_probability_max,temperature_2m_min,wind_speed_10m_max&forecast_days=5&wind_speed_unit=ms" +
                            "&timezone=auto";
                    JsonObjectRequest fbRequest = new JsonObjectRequest(
                            Request.Method.GET,
                            fallbackUrl,
                            null,
                            fbResponse -> parseDailyForecastResponse(fbResponse, callback),
                            fbError -> callback.onError("Günlük tahmin verisi alınamadı.")
                    );
                    fbRequest.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
                    requestQueue.add(fbRequest);
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 1.0f));
        requestQueue.add(request);
    }

    private void parseDailyForecastResponse(JSONObject response, ForecastCallback callback) {
        try {
            List<ForecastItem> list = new ArrayList<>();
            JSONObject daily = response.optJSONObject("daily");
            if (daily == null) {
                callback.onError("Open-Meteo günlük veri bulunamadı.");
                return;
            }

            JSONArray timeArray = daily.optJSONArray("time");
            JSONArray maxTempArray = daily.optJSONArray("temperature_2m_max");
            JSONArray minTempArray = daily.optJSONArray("temperature_2m_min");
            JSONArray windArray = daily.optJSONArray("wind_speed_10m_max");
            JSONArray codeArray = daily.optJSONArray("weather_code");
            JSONArray precipitaion_sumArray = daily.optJSONArray("precipitation_sum");
            JSONArray precipitation_probability_maxArray = daily.optJSONArray("precipitation_probability_max");

            if (timeArray == null) {
                callback.onError("Open-Meteo günlük zaman dizisi bulunamadı.");
                return;
            }

            for (int i = 0; i < Math.min(timeArray.length(), 5); i++) {
                String date = timeArray.optString(i, "");
                double minTemp = minTempArray != null ? minTempArray.optDouble(i, 0.0) : 0.0;
                double maxTemp = maxTempArray != null ? maxTempArray.optDouble(i, 0.0) : 0.0;
                double precipitaion_sum = precipitaion_sumArray != null ? precipitaion_sumArray.optDouble(i, 0.0) : 0.0;
                double precipation_probability_max = precipitation_probability_maxArray != null ? precipitation_probability_maxArray.optDouble(i, 0.0) : 0.0;
                double avgTemp = (minTemp + maxTemp) / 2.0;
                double windSpeed = (windArray != null ? windArray.optDouble(i, 0.0) : 0.0) * 3.6;
                int code = codeArray != null ? codeArray.optInt(i, 2) : 2;

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
        } catch (Exception e) {
            callback.onError("Open-Meteo günlük tahmin işlenemedi.");
        }
    }
}