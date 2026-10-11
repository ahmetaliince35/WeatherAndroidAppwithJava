package com.example.weatherapp.Helpers.WeatherReceiver;

import com.example.weatherapp.Helpers.ForecastItem;
import java.util.List;

public interface WeatherProvider {
    String getProviderKey();
    String getDisplayName();
    void getCurrentWeather(String cityName, String model, WeatherCallback<WeatherData> callback);
    void getDailyForecast(String cityName, String model, WeatherCallback<List<ForecastItem>> callback);
    void getHourlyForecast(String cityName, String model, WeatherCallback<List<ForecastItem>> callback);
}
