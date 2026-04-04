package com.example.weatherapp.Helpers;


import com.example.weatherapp.BuildConfig;

public  class URL_API {
    private URL_API(){}
    public static final String API_KEY= BuildConfig.API_KEY;
    public static final String CurrentURL="https://api.openweathermap.org/data/2.5/weather?q=";
    public static final String ForecastURL="https://api.openweathermap.org/data/2.5/forecast?q=";
}
