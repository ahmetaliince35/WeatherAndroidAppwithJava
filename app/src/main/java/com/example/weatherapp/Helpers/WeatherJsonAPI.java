package com.example.weatherapp.Helpers;

import android.content.Context;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.weatherapp.ForecastItem;
import com.example.weatherapp.PreferencesManager;

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

public class WeatherJsonAPI {

    private static final String API_KEY = URL_API.API_KEY;
    private static final String TAG="WeatherJsonAPI";
    private  final String BASE_URL;
    private final  RequestQueue requestQueue;
    private Context context;
    PreferencesManager preferencesManager;
    public WeatherJsonAPI(Context context,String BASE_URL) {
        this.context=context.getApplicationContext();
        requestQueue = Volley.newRequestQueue(context);
        this.BASE_URL=BASE_URL;
        preferencesManager=PreferencesManager.getInstance(context.getApplicationContext());
    }

    // Basit veri sınıfı: sadece çekilen değerleri tutacak
    public static class WeatherData {
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
        public int cloudiness;
    }

    // Callback arayüzü
    public interface WeatherCallback {
        void onSuccess(WeatherData data);
        void onError(String error);
    }

    // Saatlik veri callback arayüzü
    public interface HourlyCallback {
        void onSuccess(List<ForecastItem> hourlyList, String json);
        void onError(String error);
    }

    public interface ForecastCallback {
        void onSuccess(List<ForecastItem> forecastList,String json);
        void onError(String error);
    }

    // Hava durumu verisini çek
    public void getWeather(String cityName,boolean isAIactive, final WeatherCallback callback) {
        String url = BASE_URL + cityName + "&appid=" + API_KEY + "&units=metric&lang=tr";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            WeatherData data = new WeatherData();

                            // JSON parse
                            data.cityName = response.getString("name");

                            JSONObject main = response.getJSONObject("main");
                            data.temp = main.getDouble("temp");
                            data.humidity = main.getInt("humidity");
                            data.feelsLike = main.getDouble("feels_like");
                            data.pressure = main.getInt("pressure");

                            JSONArray weatherArray = response.getJSONArray("weather");
                            JSONObject weather = weatherArray.getJSONObject(0);
                            data.description = weather.getString("description");
                            data.icon = weather.getString("icon");

                            JSONObject wind = response.getJSONObject("wind");
                            data.windSpeed = wind.getDouble("speed");
                            data.windDirection = (float) wind.getDouble("deg");

                            JSONObject cloud = response.getJSONObject("clouds");
                            data.cloudiness = cloud.getInt("all");
                            if(isAIactive==true)
                            {
                            new Thread(() -> {
                                try {
                                GeminiPrompter aiProvider = new GeminiPrompter();
                                String advice = aiProvider.getWeatherAdvice(data.cityName, data.temp, data.description);
                                data.AIAdvice=advice;
                            } catch (Exception e) {
                                Log.d(TAG,"Hata: " +e);
                            } finally {
                                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                    callback.onSuccess(data);
                                });
                            }
                        }).start();

                            }
                            else
                            {
                                callback.onSuccess(data);
                            }
                        } catch (JSONException e) {
                            Log.d(TAG,"Bir şeyler ters gitti."+e);
                            callback.onError(e.toString());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        callback.onError("Şehir bulunamadı veya API hatası");
                    }
                }
        );

        requestQueue.add(request);
    }
    public void getHourlyForecast(String cityName, final HourlyCallback callback) {
        // 24 veri noktası için cnt=24
        String url = BASE_URL + cityName + "&appid=" + API_KEY + "&units=metric&lang=tr&cnt=24";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            List<ForecastItem> hourlyList = parseHourlyForecast(response);
                            //preferencesManager.saveHourlyForecastJson(response.toString());


                            callback.onSuccess(hourlyList,response.toString());

                        } catch (JSONException e) {
                                callback.onError("Saatlik veri işlenirken hata oluştu");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                            callback.onError("Saatlik veri alınamadı");
                    }
                }
        );
        requestQueue.add(request);
    }



    public void getDailyForecast(String cityName, final ForecastCallback callback) {
        String url = BASE_URL + cityName + "&appid=" + API_KEY + "&units=metric&lang=tr&cnt=40";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            List<ForecastItem> forecastList = parseDailyForecast(response);
                            callback.onSuccess(forecastList,response.toString());


                        } catch (JSONException e) {
                                callback.onError("Veri işlenirken hata oluştu");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                            callback.onError("Tahmin verisi alınamadı");
                    }
                }
        );

        requestQueue.add(request);
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
                JSONObject rainObj = hourData.getJSONObject("rain");
                rain = rainObj.optDouble("3h", 0);
            }
            if (hourData.has("snow")) {
                JSONObject snowObj = hourData.getJSONObject("snow");
                rain = snowObj.optDouble("3h", 0);
            }

            double rainProb = hourData.optDouble("pop", 0) * 100;

            ForecastItem forecastItem = new ForecastItem(
                    dateHour,
                    temp,
                    description,
                    icon,
                    humidity,
                    rainProb,
                    rain,
                    windSpeed * 3.6,
                    tempMin,
                    tempMax
            );

            hourlyList.add(forecastItem);
        }
        return hourlyList;
    }

    private List<ForecastItem> parseDailyForecast(JSONObject response) throws JSONException {
        JSONArray list = response.getJSONArray("list");
        List<ForecastItem> forecastList = new ArrayList<>();
                            // Bugünün timestamp'i (UTC)
                            Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                            today.set(Calendar.HOUR_OF_DAY, 0);
                            today.set(Calendar.MINUTE, 0);
                            today.set(Calendar.SECOND, 0);
                            today.set(Calendar.MILLISECOND, 0);
                            String todayKey = formatDateKey(today.getTimeInMillis() / 1000);

                            // Günlük verileri toplamak için map
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

                                // 12:00 verisi
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
                                dailyData.count++;
                            }

                            // Map → listeye dönüştür ve sırala
                            List<DailyData> sortedDailyData = new ArrayList<>(dailyDataMap.values());
                            sortedDailyData.sort((a, b) -> a.dayKey.compareTo(b.dayKey));

                            for (DailyData dailyData : sortedDailyData) {
                                if (dailyData.dayKey.equals(todayKey)) continue;
                                if (dailyData.noonIcon == null || dailyData.noonDescription == null) continue;

                                ForecastItem forecastItem = new ForecastItem(
                                        dailyData.displayDate,
                                        dailyData.noonTemp,
                                        dailyData.noonDescription,
                                        dailyData.noonIcon,
                                        dailyData.getAverageHumidity(),
                                        dailyData.maxRainProbability,
                                        dailyData.totalRainVolume,
                                        dailyData.getAverageWindSpeed() * 3.6,
                                        dailyData.minTemp,
                                        dailyData.maxTemp
                                );
                                forecastList.add(forecastItem);

                                if (forecastList.size() >= 5) break; // sadece 5 gün
                            }

return forecastList;
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
        int count = 0;

        DailyData(String displayDate, String dayKey) {
            this.displayDate = displayDate;
            this.dayKey = dayKey;
        }

        int getAverageHumidity() { return count > 0 ? totalHumidity / count : 0; }
        double getAverageWindSpeed() { return count > 0 ? totalWindSpeed / count : 0; }
    }
}