package com.example.weatherapp.generalFeatures;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.example.weatherapp.Activities.MainActivity;
import com.example.weatherapp.Helpers.ForecastItem;
import com.example.weatherapp.Helpers.UIUpdate;
import com.example.weatherapp.Helpers.WeatherReceiver.OpenMeteoService;
import com.example.weatherapp.Helpers.WeatherReceiver.VisualCrossingService;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherAPIService;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherJsonAPI;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherRepository;
import com.example.weatherapp.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeatherWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "WeatherWidgetProvider";
    public static final String ACTION_REFRESH_WIDGET = "com.example.weatherapp.ACTION_REFRESH_WIDGET";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAllWidgets(Context context) {
        if (context == null) return;
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context.getApplicationContext());
            ComponentName thisWidget = new ComponentName(context.getApplicationContext(), WeatherWidgetProvider.class);
            int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            if (allWidgetIds != null && allWidgetIds.length > 0) {
                for (int widgetId : allWidgetIds) {
                    updateAppWidget(context.getApplicationContext(), appWidgetManager, widgetId);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "updateAllWidgets hatası: " + e.getMessage());
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        PreferencesManager prefs = PreferencesManager.getInstance(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_weather);

        String cityName = prefs.getSavedCityName();
        boolean hasData = cityName != null && !cityName.trim().isEmpty();

        if (hasData) {
            String displayCity = cityName.contains("/") ? cityName.split("/")[0].trim() : cityName.trim();
            views.setTextViewText(R.id.widget_city_name, displayCity);
            views.setTextViewText(R.id.widget_temperature, String.format(Locale.getDefault(), "%.0f°", prefs.getTemp()));

            String desc = prefs.getDesc() != null ? prefs.getDesc().trim() : "";
            views.setTextViewText(R.id.widget_description, desc);

            String iconCode = prefs.getIcon() != null ? prefs.getIcon().trim() : "";

            String provider = prefs.getSelectedProvider();
            String providerTitle;
            if ("OWM".equals(provider)) {
                providerTitle = "OpenWeatherMap";
            } else if ("WEATHER_API".equals(provider)) {
                providerTitle = "WeatherAPI";
            } else if ("VISUAL_CROSSING".equals(provider)) {
                providerTitle = "Visual Crossing";
            } else {
                providerTitle = "Open-Meteo";
            }
            views.setTextViewText(R.id.widget_provider_name, providerTitle);

            int backgroundRes = UIUpdate.updateBackgroundByWeather(iconCode, desc);
            views.setImageViewResource(R.id.widget_background_image, backgroundRes);

            long lastUpdate = prefs.getLastUpdateTime();
            if (lastUpdate > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String timeStr = sdf.format(new Date(lastUpdate));
                views.setTextViewText(R.id.widget_last_update, desc.isEmpty() ? timeStr : "• " + timeStr);
            } else {
                views.setTextViewText(R.id.widget_last_update, "");
            }

            double feelsLike = prefs.getFeelsLike();
            if (feelsLike != 0) {
                views.setTextViewText(R.id.widget_feels_like, String.format(Locale.getDefault(), "His: %.0f°", feelsLike));
                views.setViewVisibility(R.id.widget_feels_like, View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.widget_feels_like, View.GONE);
            }

            int humidity = prefs.getHumidity();
            if (humidity > 0) {
                views.setTextViewText(R.id.widget_humidity_badge, "💧 %" + humidity);
                views.setViewVisibility(R.id.widget_humidity_badge, View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.widget_humidity_badge, View.GONE);
            }

            double wind = prefs.getWind();
            if (wind > 0) {
                views.setTextViewText(R.id.widget_wind_badge, String.format(Locale.getDefault(), "💨 %.0f km/s", wind));
                views.setViewVisibility(R.id.widget_wind_badge, View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.widget_wind_badge, View.GONE);
            }

            int pressure = prefs.getPressure();
            if (pressure > 0) {
                views.setTextViewText(R.id.widget_pressure_badge, "⏲️ " + pressure + " hPa");
                views.setViewVisibility(R.id.widget_pressure_badge, View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.widget_pressure_badge, View.GONE);
            }

            int iconRes = UIUpdate.setWeatherIcon(iconCode, desc);
            views.setImageViewResource(R.id.widget_weather_icon, iconRes);

            int descColor = getWidgetDescriptionColor();
            views.setTextColor(R.id.widget_description, descColor);

        } else {
            views.setTextViewText(R.id.widget_city_name, "Şehir Seçin");
            views.setTextViewText(R.id.widget_temperature, "--°");
            views.setTextViewText(R.id.widget_description, "Hava Durumu");
            views.setTextViewText(R.id.widget_provider_name, "");
            views.setTextViewText(R.id.widget_last_update, "");
            views.setViewVisibility(R.id.widget_feels_like, View.GONE);
            views.setViewVisibility(R.id.widget_humidity_badge, View.GONE);
            views.setViewVisibility(R.id.widget_wind_badge, View.GONE);
            views.setViewVisibility(R.id.widget_pressure_badge, View.GONE);
            views.setImageViewResource(R.id.widget_background_image, R.drawable.partlycloudy);
            views.setImageViewResource(R.id.widget_weather_icon, R.drawable.icon_partlycloudy);
            views.setTextColor(R.id.widget_description, 0xFF93C5FD);
        }

        bindHourlyForecast(views, prefs);

        if (hasData && (prefs.getWidgetHourlyItems() == null || prefs.getWidgetHourlyItems().isEmpty())) {
            fetchHourlyForecastForWidget(context, prefs);
        }

        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, flags);
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        Intent refreshIntent = new Intent(context, WeatherWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH_WIDGET);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 101, refreshIntent, flags);
        views.setOnClickPendingIntent(R.id.widget_button_refresh, refreshPendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (intent != null && ACTION_REFRESH_WIDGET.equals(intent.getAction())) {
            refreshWeatherData(context);
        }
    }

    private void refreshWeatherData(Context context) {
        PreferencesManager prefs = PreferencesManager.getInstance(context);
        String cityName = prefs.getSavedCityName();
        if (cityName == null || cityName.trim().isEmpty()) {
            Toast.makeText(context, "Kayıtlı şehir bulunamadı.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(context, "Hava durumu yenileniyor...", Toast.LENGTH_SHORT).show();
        String provider = prefs.getSelectedProvider();
        String vcModel = prefs.getVisualCrossingModel();

        String cleanCity = cityName.contains("/") ? cityName.split("/")[0].trim() : cityName.trim();

        WeatherRepository repo = new WeatherRepository(context);
        repo.getCurrentWeather(provider, cleanCity, vcModel, new com.example.weatherapp.Helpers.WeatherReceiver.WeatherCallback<com.example.weatherapp.Helpers.WeatherReceiver.WeatherData>() {
            @Override
            public void onSuccess(com.example.weatherapp.Helpers.WeatherReceiver.WeatherData data) {
                saveAndRefresh(context, prefs, provider, data.cityName, data.temp, data.description,
                        data.humidity, data.windSpeed, data.feelsLike, data.pressure, data.icon, data.windDirection, data.cloudiness);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(context, "Yenileme başarısız: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAndRefresh(Context context, PreferencesManager prefs, String provider, String city, double temp,
                                String desc, int humidity, double windSpeed, double feelsLike, int pressure,
                                String icon, float windDeg, double clouds) {
        prefs.saveWeatherData(
                provider,
                prefs.getAIAdvice(),
                city,
                temp,
                desc,
                humidity,
                windSpeed,
                feelsLike,
                pressure,
                icon,
                windDeg,
                clouds
        );
        prefs.updateLastUpdateTime();
        updateAllWidgets(context);
        fetchHourlyForecastForWidget(context, prefs);
        Toast.makeText(context, "Hava durumu güncellendi: " + String.format(Locale.getDefault(), "%.0f°", temp), Toast.LENGTH_SHORT).show();
    }

    private static void bindHourlyForecast(RemoteViews views, PreferencesManager prefs) {
        List<ForecastItem> list = prefs.getWidgetHourlyItems();
        if (list == null || list.isEmpty()) {
            views.setViewVisibility(R.id.widget_hourly_section, View.GONE);
            views.setViewVisibility(R.id.widget_hourly_divider, View.GONE);
            return;
        }

        views.setViewVisibility(R.id.widget_hourly_section, View.VISIBLE);
        views.setViewVisibility(R.id.widget_hourly_divider, View.VISIBLE);

        int count = list.size();
        int[] indices = new int[5];
        for (int i = 0; i < 5; i++) indices[i] = -1;

        long nowMillis = System.currentTimeMillis();
        long firstTime = 0;
        int startIndex = 0;

        for (int i = 0; i < count; i++) {
            long itemTime = parseItemTimestamp(list.get(i).getDate());
            if (itemTime > 0) {
                if (itemTime >= nowMillis - (45 * 60 * 1000L)) {
                    startIndex = i;
                    firstTime = itemTime;
                    break;
                }
            }
        }

        if (firstTime > 0) {
            indices[0] = startIndex;
            for (int slot = 1; slot < 5; slot++) {
                long targetTime = firstTime + (slot * 6L * 3600L * 1000L);
                int bestIdx = -1;
                long minDiff = Long.MAX_VALUE;
                int searchStart = indices[slot - 1] >= 0 ? indices[slot - 1] + 1 : startIndex + 1;
                for (int i = searchStart; i < count; i++) {
                    long t = parseItemTimestamp(list.get(i).getDate());
                    if (t > 0) {
                        long diff = Math.abs(t - targetTime);
                        if (diff < minDiff) {
                            minDiff = diff;
                            bestIdx = i;
                        }
                    }
                }
                if (bestIdx >= 0) {
                    indices[slot] = bestIdx;
                } else if (searchStart < count) {
                    indices[slot] = searchStart;
                } else {
                    indices[slot] = -1;
                }
            }
        } else {
            int step = count >= 25 ? 6 : Math.max(1, count / 5);
            int lastIdx = -1;
            for (int slot = 0; slot < 5; slot++) {
                int nextIdx = Math.max(lastIdx + 1, slot * step);
                if (nextIdx < count) {
                    indices[slot] = nextIdx;
                    lastIdx = nextIdx;
                } else {
                    indices[slot] = -1;
                }
            }
        }

        int[] timeViewIds = {
            R.id.widget_hourly_time_1, R.id.widget_hourly_time_2,
            R.id.widget_hourly_time_3, R.id.widget_hourly_time_4,
            R.id.widget_hourly_time_5
        };
        int[] iconViewIds = {
            R.id.widget_hourly_icon_1, R.id.widget_hourly_icon_2,
            R.id.widget_hourly_icon_3, R.id.widget_hourly_icon_4,
            R.id.widget_hourly_icon_5
        };
        int[] tempViewIds = {
            R.id.widget_hourly_temp_1, R.id.widget_hourly_temp_2,
            R.id.widget_hourly_temp_3, R.id.widget_hourly_temp_4,
            R.id.widget_hourly_temp_5
        };
        int[] slotViewIds = {
            R.id.widget_hourly_slot_1, R.id.widget_hourly_slot_2,
            R.id.widget_hourly_slot_3, R.id.widget_hourly_slot_4,
            R.id.widget_hourly_slot_5
        };

        for (int s = 0; s < 5; s++) {
            if (indices[s] >= 0 && indices[s] < list.size()) {
                ForecastItem item = list.get(indices[s]);
                views.setViewVisibility(slotViewIds[s], View.VISIBLE);

                String timeStr = formatHourlyTime(item.getDate());
                views.setTextViewText(timeViewIds[s], timeStr);

                int iconRes = UIUpdate.setWeatherIcon(item.getIcon(), item.getDescription());
                views.setImageViewResource(iconViewIds[s], iconRes);

                views.setTextViewText(tempViewIds[s], String.format(Locale.getDefault(), "%.0f°", item.getTemperature()));
            } else {
                views.setViewVisibility(slotViewIds[s], View.GONE);
            }
        }
    }

    private static long parseItemTimestamp(String rawDate) {
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

        // 3. Unix epoch timestamp
        try {
            long val = Long.parseLong(cleaned);
            return val < 10000000000L ? val * 1000L : val;
        } catch (Exception ignored) {}

        return 0;
    }

    private static String formatHourlyTime(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) return "--:--";

        try {
            long ts = parseItemTimestamp(rawDate);
            if (ts > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return sdf.format(new Date(ts));
            }

            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,2})[:.](\\d{2})").matcher(rawDate);
            if (m.find()) {
                int hour = Integer.parseInt(m.group(1));
                int minute = Integer.parseInt(m.group(2));
                return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
            }
        } catch (Exception e) {
            Log.e(TAG, "formatHourlyTime error: " + e.getMessage());
        }
        return rawDate;
    }

    public static void fetchHourlyForecastForWidget(Context context, PreferencesManager prefs) {
        fetchHourlyForecastForWidget(context, prefs, null);
    }

    public static void fetchHourlyForecastForWidget(Context context, PreferencesManager prefs, String targetCity) {
        if (context == null || prefs == null) return;
        String cityName = (targetCity != null && !targetCity.trim().isEmpty()) ? targetCity : prefs.getSavedCityName();
        if (cityName == null || cityName.trim().isEmpty()) return;

        String cleanCity = cityName.contains("/") ? cityName.split("/")[0].trim() : cityName.trim();
        String provider = prefs.getSelectedProvider();
        String vcModel = prefs.getVisualCrossingModel();

        try {
            WeatherRepository repository = new WeatherRepository(context.getApplicationContext());
            repository.getHourlyForecast(provider, vcModel, cleanCity, new WeatherRepository.ForecastDataCallback() {
                @Override
                public void onSuccess(List<ForecastItem> forecastList) {
                    if (forecastList != null && !forecastList.isEmpty()) {
                        prefs.saveWidgetHourlyItems(forecastList);
                        updateAllWidgets(context);
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "fetchHourlyForecastForWidget error: " + error);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "fetchHourlyForecastForWidget exception: " + e.getMessage());
        }
    }


    public static int getWidgetDescriptionColor() {
        return 0xFFFFFFFF;
    }
}
