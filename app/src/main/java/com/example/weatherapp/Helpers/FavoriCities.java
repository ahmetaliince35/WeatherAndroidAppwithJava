package com.example.weatherapp.Helpers;

import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherapp.FavoriCityAdapter;
import com.example.weatherapp.data.AppDatabase;
import com.example.weatherapp.data.WeatherEntity;

import java.util.List;

public class FavoriCities {
    AppDatabase db;
    private RecyclerView recyclerFavorites;
    private List<WeatherEntity> favoriteList;
    FavoriCityAdapter favoriCityAdapter;
    public FavoriCities(AppDatabase db)
    {
    this.db=db;
    }
    public boolean saveToFavorites(String cityName, double temp, String desc, int humidity, double wind, String icon) {
        if (db.weatherDao().findByCity(cityName) == null) {
            WeatherEntity entity = new WeatherEntity();

            // İlk harfi büyütme mantığı
            if (cityName != null && !cityName.isEmpty()) {
                entity.cityName = cityName.substring(0, 1).toUpperCase() + cityName.substring(1).toLowerCase();
            } else {
                entity.cityName = cityName;
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


