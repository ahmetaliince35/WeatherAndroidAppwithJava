package com.example.weatherapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
public class PreferencesManager {

    private static final String TAG = "PreferencesManager";
    public static final String PREF_NAME = "WeatherAppPrefs";
    private static final String KEY_CITY_NAME = "city_name";
    private static final String KEY_LAST_UPDATE = "last_update";

    private SharedPreferences preferences;
    private Context context;

    public PreferencesManager(Context context) {
        this.context = context.getApplicationContext(); // Application context kullan
        this.preferences = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "PreferencesManager initialized");
    }

    public void saveLocation(String cityName) {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_CITY_NAME, cityName);
            editor.putLong(KEY_LAST_UPDATE, System.currentTimeMillis());
            editor.commit();
            Log.d(TAG, "Location saved: " + cityName);
        } catch (Exception e) {
            Log.e(TAG, "saveLocation error: " + e.getMessage());
        }
    }

    public String getSavedCityName() {
        try {
            String cityName = preferences.getString(KEY_CITY_NAME, null);
            Log.d(TAG, "getSavedCityName: " + cityName);
            return cityName;
        } catch (Exception e) {
            Log.e(TAG, "getSavedCityName error: " + e.getMessage());
            return null;
        }
    }

    public long getLastUpdateTime() {
        try {
            long time = preferences.getLong(KEY_LAST_UPDATE, 0);
            Log.d(TAG, "getLastUpdateTime: " + time);
            return time;
        } catch (Exception e) {
            Log.e(TAG, "getLastUpdateTime error: " + e.getMessage());
            return 0;
        }
    }



    public void updateLastUpdateTime() {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            long currentTime = System.currentTimeMillis();
            editor.putLong(KEY_LAST_UPDATE, currentTime);
            editor.commit();
            Log.d(TAG, "Last update time updated: " + currentTime);
        } catch (Exception e) {
            Log.e(TAG, "updateLastUpdateTime error: " + e.getMessage());
        }
    }

    public boolean shouldUpdate() {
        try {
            long lastUpdate = getLastUpdateTime();
            if (lastUpdate == 0) {
                Log.d(TAG, "shouldUpdate: true (never updated)");
                return true;
            }
            long currentTime = System.currentTimeMillis();
            long twelveHours = 12*60* 60 * 1000;
            boolean shouldUpdate = (currentTime - lastUpdate) >= twelveHours;
            Log.d(TAG, "shouldUpdate: " + shouldUpdate);
            return shouldUpdate;
        } catch (Exception e) {
            Log.e(TAG, "shouldUpdate error: " + e.getMessage());
            return true;
        }
    }

    public void clearAll() {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();
            Log.d(TAG, "All preferences cleared");
        } catch (Exception e) {
            Log.e(TAG, "clearAll error: " + e.getMessage());
        }
    }

    public double getTemp() {
        try {
            double temp = preferences.getLong("temp", 0);
            Log.d(TAG, "getTemp: " + temp);
            return temp;
        } catch (Exception e) {
            Log.e(TAG, "getTemp error: " + e.getMessage());
            return 0;
        }
    }

    public String getDesc() {
        try {
            String desc = preferences.getString("desc", "AÇIK");
            Log.d(TAG, "getDesc: " + desc);
            return desc;
        } catch (Exception e) {
            Log.e(TAG, "getDesc error: " + e.getMessage());
            return null;
        }
    }

    public int getHumidity() {
        try {
            int humidity = preferences.getInt("humidity", 0);
            Log.d(TAG, "getHumidity: " + humidity);
            return humidity;
        } catch (Exception e) {
            Log.e(TAG, "getHumidity error: " + e.getMessage());
            return 0;
        }
    }

    public double getWind() {
        try {
            double wind = preferences.getLong("wind", 0);
            Log.d(TAG, "getWind: " + wind);
            return wind;
        } catch (Exception e) {
            Log.e(TAG, "getWind error: " + e.getMessage());
            return 0;
        }
    }

    public double getFeels() {
        try {
            double feels = preferences.getLong("feels", 0);
            Log.d(TAG, "getFeels: " + feels);
            return feels;
        } catch (Exception e) {
            Log.e(TAG, "getFeels error: " + e.getMessage());
            return 0;
        }
    }

    public int getPressure() {
        try {
            int press = preferences.getInt("pressure", 0);
            Log.d(TAG, "getPressure: " + press);
            return press;
        } catch (Exception e) {
            Log.e(TAG, "getPressure error: " + e.getMessage());
            return 0;
        }
    }

    public String getIcon() {
        try {
            String icon = preferences.getString("icon", "AÇIK");
            Log.d(TAG, "getIcon: " + icon);
            return icon;
        } catch (Exception e) {
            Log.e(TAG, "getIcon error: " + e.getMessage());
            return null;
        }
    }

    public void saveWeatherData(String city, double temp, String desc,
                                int humidity, double wind,
                                double feels, int pressure, String icon) {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("city", city);
            editor.putLong("temp", (long) temp);
            editor.putString("desc", desc);
            editor.putInt("humidity", humidity);
            editor.putLong("wind", (long) wind);
            editor.putLong("feels", (long) feels);
            editor.putInt("pressure", pressure);
            editor.putString("icon", icon);
            editor.commit();
            Log.d(TAG, "Weather data saved: " + city + ", temp=" + temp + ", desc=" + desc);
        } catch (Exception e) {
            Log.e(TAG, "saveWeatherData error: " + e.getMessage());
        }
    }
}