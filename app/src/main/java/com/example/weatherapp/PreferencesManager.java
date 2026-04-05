package com.example.weatherapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
public class PreferencesManager {

    private static final String TAG = "PreferencesManager";
    public static final String PREF_NAME = "WeatherAppPrefs";
    private static final String KEY_CITY_NAME = "city_name";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final String KEY_HOURLY_FORECAST_JSON = "hourly_forecast_json";
    private static final String KEY_DAILY_FORECAST_JSON = "daily_forecast_json";
    private static PreferencesManager instance;
    private SharedPreferences preferences;

    private PreferencesManager(Context context) {
    preferences=context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
public static synchronized PreferencesManager getInstance(Context context)
{
    if(instance == null)
    {
        instance = new PreferencesManager(context);
    }
    return instance;
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




    public float getWindDegree() {
        try {
            float deg = preferences.getFloat("WindDegree", 0);
            Log.d(TAG, "getWindDegree: " + deg);
            return deg;
        } catch (Exception e) {
            Log.e(TAG, "getWindDegree error: " + e.getMessage());
            return 0;
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

    public double getHumidity() {
        try {
            double humidity = preferences.getInt("humidity", 0);
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
    public String getAIAdvice()
    {
        try {
            String advice = preferences.getString("AIAdvice", " ");
            return advice;
        } catch (Exception e) {
            return null;
        }
    }

    public int getCloudiness()
    {
        try {
            int cloudness = preferences.getInt("Cloud", 0);
            return cloudness;
        } catch (Exception e) {
            Log.e(TAG, "getIcon error: " + e.getMessage());
            return 0;
        }
    }

    public void saveWeatherData(String advice,String city, double temp, String desc,
                                double humidity, double wind,
                                double feels, int pressure, String icon,float windDegree,int cloudiness) {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("AIAdvice",advice);
            editor.putString("city", city);
            editor.putLong("temp", (long) temp);
            editor.putString("desc", desc);
            editor.putLong("humidity",(long) humidity);
            editor.putLong("wind", (long) wind);
            editor.putLong("feels", (long) feels);
            editor.putInt("pressure", pressure);
            editor.putString("icon", icon);
            editor.putFloat("WindDegree",windDegree);
            editor.putInt("Cloud",cloudiness);
            editor.commit();
            Log.d(TAG, "Weather data saved: " + city + ", temp=" + temp + ", desc=" + desc);
        } catch (Exception e) {
            Log.e(TAG, "saveWeatherData error: " + e.getMessage());
        }
    }
    public void clearAll()
    {
        SharedPreferences.Editor editor=preferences.edit();
        editor.clear();
        editor.commit();
    }
    public void saveHourlyForecastJson(String json) {
        try {
            preferences.edit().putString(KEY_HOURLY_FORECAST_JSON, json).commit();
            Log.d(TAG, "Hourly forecast saved");
        } catch (Exception e) {
            Log.e(TAG, "saveHourlyForecastJson error: " + e.getMessage());
        }
    }

    public void saveDailyForecastJson(String json) {
        try {
            preferences.edit().putString(KEY_DAILY_FORECAST_JSON, json).commit();
            Log.d(TAG, "Daily forecast saved");
        } catch (Exception e) {
            Log.e(TAG, "saveDailyForecastJson error: " + e.getMessage());
        }
    }

    public String getHourlyForecastJson() {
        try {
            return preferences.getString(KEY_HOURLY_FORECAST_JSON, null);
        } catch (Exception e) {
            Log.e(TAG, "getHourlyForecastJson error: " + e.getMessage());
            return null;
        }
    }

    public String getDailyForecastJson() {
        try {
            return preferences.getString(KEY_DAILY_FORECAST_JSON, null);
        } catch (Exception e) {
            Log.e(TAG, "getDailyForecastJson error: " + e.getMessage());
            return null;
        }
    }
}