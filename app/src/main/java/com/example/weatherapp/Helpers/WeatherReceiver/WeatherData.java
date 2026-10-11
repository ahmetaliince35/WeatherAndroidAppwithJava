package com.example.weatherapp.Helpers.WeatherReceiver;

public class WeatherData {
    public String cityName;
    public double temp;
    public double feelsLike;
    public int humidity;
    public double windSpeed; // km/h cinsinden standart
    public float windDirection;
    public int pressure;
    public String description;
    public String icon;
    public double cloudiness;
    public String aiAdvice;
    public long timestamp;

    public WeatherData() {
        this.timestamp = System.currentTimeMillis();
        this.aiAdvice = "";
    }

    public WeatherData(String cityName, double temp, double feelsLike, int humidity,
                       double windSpeed, float windDirection, int pressure,
                       String description, String icon, double cloudiness) {
        this.cityName = cityName;
        this.temp = temp;
        this.feelsLike = feelsLike;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.pressure = pressure;
        this.description = description;
        this.icon = icon;
        this.cloudiness = cloudiness;
        this.aiAdvice = "";
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "WeatherData{" +
                "cityName='" + cityName + '\'' +
                ", temp=" + temp +
                ", feelsLike=" + feelsLike +
                ", humidity=" + humidity +
                ", windSpeed=" + windSpeed +
                ", windDirection=" + windDirection +
                ", pressure=" + pressure +
                ", description='" + description + '\'' +
                ", icon='" + icon + '\'' +
                ", cloudiness=" + cloudiness +
                ", aiAdvice='" + aiAdvice + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
