package com.example.weatherapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorite_weather")
public class WeatherEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String cityName;
    public double temperature;
    public String description;
    public int humidity;
    public double windSpeed;
    public String icon;
}
