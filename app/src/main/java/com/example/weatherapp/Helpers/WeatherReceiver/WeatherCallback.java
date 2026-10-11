package com.example.weatherapp.Helpers.WeatherReceiver;

public interface WeatherCallback<T> {
    void onSuccess(T data);
    void onError(String error);
}
