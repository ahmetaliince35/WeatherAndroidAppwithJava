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
    public void saveLocation(String cityName) {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_CITY_NAME, cityName);
            editor.putLong(KEY_LAST_UPDATE, System.currentTimeMillis());

            // commit() kullan - apply() yerine (garantili kayıt)
            editor.commit();

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
            long twelveHours = 60*1000 ;

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
    public double getTemp() {
        try {
            double temp = preferences.getLong("temp",0 );
            return temp;
        } catch (Exception e) {
            return 0;
        }
    }
    public String getDesc() {
        try {
            String desc = preferences.getString("desc", "AÇIK");
            return desc;
        } catch (Exception e) {
            return null;
        }
    }
    public int getHumidity() {
        try {
            int humidity = preferences.getInt("humidity", 0);
            return humidity;
        } catch (Exception e) {
            return 0;
        }
    }
    public double getWind() {
        try {
            double wind = preferences.getLong("wind", 0);
            return wind;
        } catch (Exception e) {
            return 0;
        }
    }
    public double getFeels() {
        try {
            double feels = preferences.getLong("feels", 0);
            return feels;
        } catch (Exception e) {
            return 0;
        }
    }
    public int getPressure() {
        try {
            int press= preferences.getInt("pressure", 0);
            return press;
        } catch (Exception e) {
            return 0;
        }
    }
    public String getIcon() {
        try {
            String icon = preferences.getString("icon", "AÇIK");
            return icon;
        } catch (Exception e) {
            return null;
        }
    }
    public void saveWeatherData(String city, double temp, String desc,
                                int humidity, double wind,
                                double feels, int pressure, String icon) {

        SharedPreferences.Editor editor = preferences.edit();

        editor.putString("city", city);
        editor.putLong("temp", (long) temp);
        editor.putString("desc", desc);
        editor.putInt("humidity", humidity);
        editor.putLong("wind", (long) wind);
        editor.putLong("feels", (long) feels);
        editor.putInt("pressure", pressure);
        editor.putString("icon", icon);

        editor.apply();
    }

}