package com.example.weatherapp.Helpers.WeatherReceiver;

import android.content.Context;
import android.util.Log;

import com.example.weatherapp.Helpers.ForecastItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class WeatherRepository {

    private static final String TAG = "WeatherRepository";
    private final Context context;
    private final Map<String, WeatherProvider> providers = new HashMap<>();

    public interface ForecastDataCallback {
        void onSuccess(List<ForecastItem> forecastList);
        void onError(String error);
    }

    public WeatherRepository(Context context) {
        this.context = context.getApplicationContext();
        OpenMeteoService openMeteo = new OpenMeteoService(context);
        WeatherJsonAPI owm = new WeatherJsonAPI(context);
        WeatherAPIService weatherApi = new WeatherAPIService(context);
        VisualCrossingService visualCrossing = new VisualCrossingService(context);

        providers.put("OPEN_METEO", openMeteo);
        providers.put("OWM", owm);
        providers.put("WEATHER_API", weatherApi);
        providers.put("VISUAL_CROSSING", visualCrossing);
    }

    public WeatherProvider getProvider(String providerKey) {
        if (providerKey == null || providerKey.trim().isEmpty()) {
            return providers.get("OPEN_METEO");
        }
        WeatherProvider provider = providers.get(providerKey.trim().toUpperCase(Locale.ROOT));
        return provider != null ? provider : providers.get("OPEN_METEO");
    }

    public void getCurrentWeather(String providerKey, String cityName, String model, WeatherCallback<WeatherData> callback) {
        WeatherProvider provider = getProvider(providerKey);
        provider.getCurrentWeather(cityName, model, callback);
    }

    public WeatherData getCurrentWeatherSync(String providerKey, String cityName, String model, long timeoutSeconds) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<WeatherData> resultRef = new AtomicReference<>();
        final AtomicReference<String> errorRef = new AtomicReference<>();

        getCurrentWeather(providerKey, cityName, model, new WeatherCallback<WeatherData>() {
            @Override
            public void onSuccess(WeatherData data) {
                resultRef.set(data);
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                errorRef.set(error);
                latch.countDown();
            }
        });

        boolean completed = latch.await(timeoutSeconds, TimeUnit.SECONDS);
        if (!completed) {
            throw new Exception("Hava durumu isteği zaman aşımına uğradı (" + timeoutSeconds + " saniye).");
        }
        if (errorRef.get() != null) {
            throw new Exception(errorRef.get());
        }
        return resultRef.get();
    }

    public void getDailyForecast(String providerKey, String model, String cityName, ForecastDataCallback callback) {
        getDailyForecast(providerKey, cityName, model, new WeatherCallback<List<ForecastItem>>() {
            @Override
            public void onSuccess(List<ForecastItem> data) {
                if (data != null && data.size() > 5) {
                    data = new ArrayList<>(data.subList(0, 5));
                }
                callback.onSuccess(data);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getDailyForecast(String providerKey, String cityName, String model, WeatherCallback<List<ForecastItem>> callback) {
        WeatherProvider provider = getProvider(providerKey);
        provider.getDailyForecast(cityName, model, new WeatherCallback<List<ForecastItem>>() {
            @Override
            public void onSuccess(List<ForecastItem> data) {
                if (data != null && data.size() > 5) {
                    data = new ArrayList<>(data.subList(0, 5));
                }
                callback.onSuccess(data);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getHourlyForecast(String providerKey, String model, String cityName, ForecastDataCallback callback) {
        getHourlyForecast(providerKey, cityName, model, new WeatherCallback<List<ForecastItem>>() {
            @Override
            public void onSuccess(List<ForecastItem> data) {
                if (data != null) {
                    data = filterFutureHours(data, 72);
                }
                callback.onSuccess(data);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getHourlyForecast(String providerKey, String cityName, String model, WeatherCallback<List<ForecastItem>> callback) {
        WeatherProvider provider = getProvider(providerKey);
        provider.getHourlyForecast(cityName, model, new WeatherCallback<List<ForecastItem>>() {
            @Override
            public void onSuccess(List<ForecastItem> data) {
                if (data != null) {
                    data = filterFutureHours(data, 72);
                }
                callback.onSuccess(data);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private List<ForecastItem> filterFutureHours(List<ForecastItem> list, int maxCount) {
        if (list == null || list.isEmpty()) return list;
        long nowThreshold = System.currentTimeMillis() - (50 * 60 * 1000L);
        List<ForecastItem> filtered = new ArrayList<>();
        boolean foundStart = false;

        for (ForecastItem item : list) {
            if (item == null) continue;
            long itemTime = parseTimestamp(item.getDate());
            if (!foundStart) {
                if (itemTime == 0 || itemTime >= nowThreshold) {
                    foundStart = true;
                    filtered.add(item);
                }
            } else {
                filtered.add(item);
            }
            if (filtered.size() >= maxCount) break;
        }

        return filtered.isEmpty() ? list : filtered;
    }

    private long parseTimestamp(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) return 0;
        String cleaned = rawDate.trim().replace("T", " ");

        if (cleaned.length() >= 16 && cleaned.charAt(4) == '-' && cleaned.charAt(7) == '-') {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                Date d = sdf.parse(cleaned.substring(0, 16));
                if (d != null) return d.getTime();
            } catch (Exception ignored) {}
        }

        if (cleaned.contains(".")) {
            try {
                String datePart = cleaned.contains("-") ? cleaned.split("-")[0].trim() : cleaned;
                String[] parts = datePart.split(" ");
                if (parts.length >= 3) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH.mm", new Locale("tr", "TR"));
                    Date d = sdf.parse(parts[0] + " " + parts[1] + " " + parts[2]);
                    if (d != null) {
                        Calendar cal = Calendar.getInstance();
                        Calendar parsedCal = Calendar.getInstance();
                        parsedCal.setTime(d);
                        cal.set(Calendar.MONTH, parsedCal.get(Calendar.MONTH));
                        cal.set(Calendar.DAY_OF_MONTH, parsedCal.get(Calendar.DAY_OF_MONTH));
                        cal.set(Calendar.HOUR_OF_DAY, parsedCal.get(Calendar.HOUR_OF_DAY));
                        cal.set(Calendar.MINUTE, parsedCal.get(Calendar.MINUTE));
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        return cal.getTimeInMillis();
                    }
                }
            } catch (Exception ignored) {}
        }

        try {
            long val = Long.parseLong(cleaned);
            return val < 10000000000L ? val * 1000L : val;
        } catch (Exception ignored) {}

        return 0;
    }
}