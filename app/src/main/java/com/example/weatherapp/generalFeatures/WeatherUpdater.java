package com.example.weatherapp.generalFeatures;

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

import com.example.weatherapp.Activities.MainActivity;
import com.example.weatherapp.Helpers.ForecastItem;
import com.example.weatherapp.Helpers.GeminiHelperPackages.GeminiPrompter;
import com.example.weatherapp.Helpers.UIUpdate;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherCallback;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherData;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherRepository;
import com.example.weatherapp.R;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WeatherUpdater extends Worker {

    private static final String TAG = "WeatherUpdateWorker";
    private static final String CHANNEL_ID = "weather_updates";
    private static final int NOTIFICATION_ID = 1001;

    public WeatherUpdater(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "WeatherUpdater çalışıyor...");

        Context context = getApplicationContext();
        PreferencesManager prefsManager = PreferencesManager.getInstance(context);
        String cityName = prefsManager.getSavedCityName();
        if (cityName == null || cityName.trim().isEmpty()) {
            Log.e(TAG, "Kayıtlı şehir bulunamadı, worker sonlandırıldı.");
            return Result.failure();
        }

        String currentProvider = prefsManager.getSelectedProvider();
        String vcModel = prefsManager.getVisualCrossingModel();

        try {
            WeatherRepository repository = new WeatherRepository(context);

            // Seçili sağlayıcıya göre anlık hava durumu çekiliyor
            WeatherData data = repository.getCurrentWeatherSync(currentProvider, cityName, vcModel, 20);
            if (data == null) {
                Log.e(TAG, "Hava durumu verisi alınamadı (null).");
                return Result.retry();
            }

            // Sağlayıcıdan bağımsız optimize Gemini promptu ile tavsiye alınıyor
            GeminiPrompter aiProvider = new GeminiPrompter(context);
            String aiAdvice = aiProvider.getWeatherAdvice(data.cityName, data.temp, data.description, data.icon);

            // Tercihler güncelleniyor
            prefsManager.saveWeatherData(
                    currentProvider,
                    aiAdvice,
                    data.cityName,
                    data.temp,
                    data.description,
                    data.humidity,
                    data.windSpeed,
                    data.feelsLike,
                    data.pressure,
                    data.icon,
                    data.windDirection,
                    data.cloudiness
            );
            prefsManager.updateLastUpdateTime();

            // Saatlik ve günlük tahminleri de sağlayıcıdan güncelle
            updateForecastsSilently(repository, currentProvider, cityName, vcModel);

            // Widget'ları güncelle
            WeatherWidgetProvider.updateAllWidgets(context);
            Log.d(TAG, "Hava durumu ve widget'lar güncellendi.");

            // Bildirim gönder
            sendWeatherNotification(
                    data.cityName,
                    String.format(Locale.getDefault(), "%.1f°C", data.temp),
                    data.description,
                    data.icon
            );

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Hava durumu otomatik güncelleme hatası: " + e.getMessage());
            return Result.retry();
        }
    }

    private void updateForecastsSilently(WeatherRepository repository, String provider, String cityName, String model) {
        final CountDownLatch latch = new CountDownLatch(2);

        repository.getHourlyForecast(provider, cityName, model, new WeatherCallback<List<ForecastItem>>() {
            @Override
            public void onSuccess(List<ForecastItem> data) {
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                latch.countDown();
            }
        });

        repository.getDailyForecast(provider, cityName, model, new WeatherCallback<List<ForecastItem>>() {
            @Override
            public void onSuccess(List<ForecastItem> data) {
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
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

        int iconResource = UIUpdate.setWeatherIcon(iconCode, description);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Bildirim izni verilmemiş.");
            return;
        }

        RemoteViews notificationLayout = new RemoteViews(context.getPackageName(), R.layout.notification);
        String safeDesc = "Hava Durumu";
        if (description != null && !description.trim().isEmpty()) {
            safeDesc = (description.length() > 1)
                    ? description.substring(0, 1).toUpperCase(new Locale("tr", "TR")) + description.substring(1).toLowerCase(new Locale("tr", "TR"))
                    : description.toUpperCase(new Locale("tr", "TR"));
        }
        notificationLayout.setTextViewText(R.id.tvDescription, safeDesc);
        notificationLayout.setImageViewResource(R.id.ivWeatherIcon, iconResource);

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
            String description = "Otomatik hava durumu güncellemeleri";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}