package com.example.weatherapp.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WeatherEntity city);

    @Query("SELECT * FROM favorite_weather")
    LiveData<List<WeatherEntity>> getAllFavorites();

    @Query("SELECT * FROM favorite_weather WHERE cityName = :city LIMIT 1")
    WeatherEntity findByCity(String city);

    @Query("DELETE FROM favorite_weather WHERE cityName = :city")
    void deleteByCity(String city);

    @Query("DELETE FROM favorite_weather")
    void deleteallcity();
    @Query("SELECT * FROM favorite_weather")
    List<WeatherEntity> getAllFavoritesSync();  // Worker içinde kullanacağız

    @Query("UPDATE favorite_weather SET temperature=:temp, description=:desc, icon=:icon WHERE cityName=:city")
    void updateFavoriteWeather(String city, double temp, String desc, String icon);
}
