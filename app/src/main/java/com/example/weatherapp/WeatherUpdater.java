package com.example.weatherapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class WeatherUpdater extends Worker {

    private static final String TAG = "WeatherUpdateWorker";
    private static final String API_KEY = URL_API.API_KEY;
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String CHANNEL_ID = "weather_updates";
    private static final int NOTIFICATION_ID = 1001;

    public WeatherUpdater(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Worker başlatıldı");

        PreferencesManager prefsManager = PreferencesManager.getInstance(getApplicationContext());
        String cityName = prefsManager.getSavedCityName();
        if (cityName == null || cityName.isEmpty()) {
            Log.e(TAG, "Kayıtlı şehir yok, worker başarısız");
            return Result.failure();
        }
        try {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            WeatherService service = retrofit.create(WeatherService.class);

            Call<WeatherResponse> call = service.getCurrentWeather(cityName, API_KEY, "metric", "tr");
            Response<WeatherResponse> response = call.execute();

            if (!response.isSuccessful() || response.body() == null) {
                Log.e(TAG, "API hatası: " + response.code());
                return Result.retry();
            }

            WeatherResponse data = response.body();

            // Preferences güncelle
            prefsManager.saveWeatherData(
                    data.name,
                    data.main.temp,
                    data.weather.get(0).description,
                    data.main.humidity,
                    data.wind.speed * 3.6,
                    data.main.feels_like,
                    data.main.pressure,
                    data.weather.get(0).icon,
                    data.wind.deg
            );
            Response<ResponseBody> hourlyResponse = service.getForecast(cityName, API_KEY, "metric", "tr", 24).execute();
            if (hourlyResponse.isSuccessful() && hourlyResponse.body() != null) {
                prefsManager.saveHourlyForecastJson(hourlyResponse.body().string());
            }

            Response<ResponseBody> dailyResponse = service.getForecast(cityName, API_KEY, "metric", "tr", 40).execute();
            if (dailyResponse.isSuccessful() && dailyResponse.body() != null) {
                prefsManager.saveDailyForecastJson(dailyResponse.body().string());
            }
            prefsManager.updateLastUpdateTime();
            Log.d(TAG, "Preferences güncellendi");

            // Bildirim gönder
            sendWeatherNotification(
                    data.name,
                    String.format("%.1f°C", data.main.temp),
                    data.weather.get(0).description,
                    data.weather.get(0).icon
            );

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Hava durumu güncelleme hatası: " + e.getMessage());
            return Result.retry();
        }
    }

    // Retrofit arayüzü
    public interface WeatherService {
        @GET("weather")
        Call<WeatherResponse> getCurrentWeather(
                @Query("q") String city,
                @Query("appid") String apiKey,
                @Query("units") String units,
                @Query("lang") String lang
        );
        @GET("forecast")
        Call<ResponseBody> getForecast(
                @Query("q") String city,
                @Query("appid") String apiKey,
                @Query("units") String units,
                @Query("lang") String lang,
                @Query("cnt") int count
        );
    }

    public static class WeatherResponse {
        public Main main;
        public List<Weather> weather;
        public Wind wind;
        public String name;

        public static class Main {
            public double temp;
            public int humidity;
            public double feels_like;
            public int pressure;
        }

        public static class Weather {
            public String description;
            public String icon;
        }

        public static class Wind {
            public double speed;
            public float deg;
        }
    }

    private void sendWeatherNotification(String cityName, String temperature, String description, String iconCode) {
        Context context = getApplicationContext();

        createNotificationChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        int iconResource = getWeatherIcon(iconCode);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

            // Eğer izin yoksa burada izin isteyebilir veya metoddan çıkabilirsin.
            Log.e("Hata", "Bildirim izni verilmemiş!");
            return;
        }
        RemoteViews notificationLayout = new RemoteViews(getApplicationContext().getPackageName(), R.layout.notification);
        notificationLayout.setTextViewText(R.id.tvTitle, cityName + " - " + temperature);
        notificationLayout.setTextViewText(R.id.tvDescription, description.substring(0,1).toUpperCase()+description.substring(1));
        notificationLayout.setImageViewResource(R.id.ivWeatherIcon, getWeatherIcon(iconCode));

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(iconResource)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notificationLayout)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Hava Durumu Güncellemeleri";
            String description = "12 saatte bir otomatik hava durumu güncellemeleri";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Bildirim kanalı oluşturuldu");
            }
        }
    }

    private int getWeatherIcon(String iconCode) {
        switch (iconCode) {
            case "01d":
            case "01n":
                return R.drawable.icon_sunny;
            case "02d":
            case "02n":
                return R.drawable.icon_partlycloudy;
            case "03d":
            case "03n":
            case "04d":
            case "04n":
                return R.drawable.icon_cloudy;
            case "09d":
            case "09n":
            case "10d":
            case "10n":
                return R.drawable.icon_rainy;
            case "11d":
            case "11n":
                return R.drawable.icon_thunderstorm;
            case "13d":
            case "13n":
                return R.drawable.icon_snowy;
            default:
                return R.drawable.icon_sunny;
        }
    }
}