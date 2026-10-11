package com.example.weatherapp.Helpers;

import com.example.weatherapp.data.Databases.AppDatabase;
import com.example.weatherapp.data.Entities.WeatherEntity;

public class FavoriCities {
    AppDatabase db;
    public FavoriCities(AppDatabase db)
    {
    this.db=db;
    }
    public boolean saveToFavorites(String cityName, double temp, String desc, int humidity, double wind, String icon) {
        if (cityName == null || cityName.trim().isEmpty()) return false;
        if (db.weatherDao().findByCity(cityName) == null) {
            WeatherEntity entity = new WeatherEntity();

            if (cityName.length() > 1) {
                entity.cityName = cityName.substring(0, 1).toUpperCase(new java.util.Locale("tr", "TR")) +
                        cityName.substring(1).toLowerCase(new java.util.Locale("tr", "TR"));
            } else {
                entity.cityName = cityName.toUpperCase(new java.util.Locale("tr", "TR"));
            }

            entity.temperature = temp;
            entity.description = desc;
            entity.humidity = humidity;
            entity.windSpeed = wind;
            entity.icon = icon;

            db.weatherDao().insert(entity);
            return true; // Başarıyla eklendi
        }
        return false; // Zaten var
    }

}


