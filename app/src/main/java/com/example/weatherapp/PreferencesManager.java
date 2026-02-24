package com.example.weatherapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * SharedPreferences işlemleri için yönetici sınıf
 * Konum ve son güncelleme zamanını saklar
 *
 * FIXED: Daha iyi hata ayıklama ve loglama eklendi
 */
public class PreferencesManager {

    private static final String TAG = "PreferencesManager";
    private static final String PREF_NAME = "WeatherAppPrefs";
    private static final String KEY_CITY_NAME = "city_name";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final String KEY_AUTO_UPDATE_ENABLED = "auto_update_enabled";

    private SharedPreferences preferences;
    private Context context;

    public PreferencesManager(Context context) {
        this.context = context.getApplicationContext(); // Application context kullan
        this.preferences = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

    }

    /**
     * Konum bilgilerini kaydet
     */
    public void saveLocation(String cityName, double latitude, double longitude) {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_CITY_NAME, cityName);
            editor.putFloat(KEY_LATITUDE, (float) latitude);
            editor.putFloat(KEY_LONGITUDE, (float) longitude);
            editor.putLong(KEY_LAST_UPDATE, System.currentTimeMillis());

            // commit() kullan - apply() yerine (garantili kayıt)
            boolean success = editor.commit();

            if (success) {

                // Doğrulama - gerçekten kaydedildi mi?
                String savedCity = preferences.getString(KEY_CITY_NAME, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Kayıtlı şehir adını al
     */
    public String getSavedCityName() {
        try {
            String cityName = preferences.getString(KEY_CITY_NAME, null);
            return cityName;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Kayıtlı enlem al
     */
    public double getSavedLatitude() {
        try {
            float lat = preferences.getFloat(KEY_LATITUDE, 0f);
            return lat;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Kayıtlı boylam al
     */
    public double getSavedLongitude() {
        try {
            float lon = preferences.getFloat(KEY_LONGITUDE, 0f);
            return lon;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Son güncelleme zamanını al
     */
    public long getLastUpdateTime() {
        try {
            long time = preferences.getLong(KEY_LAST_UPDATE, 0);
            return time;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Otomatik güncelleme aktif mi?
     */
    public boolean isAutoUpdateEnabled() {
        try {
            boolean enabled = preferences.getBoolean(KEY_AUTO_UPDATE_ENABLED, true);
            return enabled;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Otomatik güncelleme ayarını değiştir
     */
    public void setAutoUpdateEnabled(boolean enabled) {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_AUTO_UPDATE_ENABLED, enabled);
            editor.commit();

        }
        catch (Exception e)
        {
            Log.e(TAG, "Otomatik güncelleme ayarlama hatası: " + e.getMessage());
        }
    }

    /**
     * Son güncelleme zamanını güncelle
     */
    public void updateLastUpdateTime() {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            long currentTime = System.currentTimeMillis();
            editor.putLong(KEY_LAST_UPDATE, currentTime);
            editor.commit();
        }
        catch (Exception e)
        {
            Log.e(TAG, "Güncelleme zamanı kaydetme hatası: " + e.getMessage());
        }
    }

    /**
     * Güncellenme gerekiyor mu? (12 saatte bir)
     */
    public boolean shouldUpdate()
    {
        try {
            long lastUpdate = getLastUpdateTime();

            if (lastUpdate == 0) {

                return true;
            }

            long currentTime = System.currentTimeMillis();
            long timeDifference = currentTime - lastUpdate;

            // 12 saat = 12 * 60 * 60 * 1000 ms
            long twelveHours = 12 * 60 * 60 * 1000;

            boolean shouldUpdate = timeDifference >= twelveHours;



            return shouldUpdate;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Tüm verileri sil
     */
    public void clearAll() {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();

        } catch (Exception e) {
            Log.e(TAG, "Veri silme hatası: " + e.getMessage());
        }
    }


}