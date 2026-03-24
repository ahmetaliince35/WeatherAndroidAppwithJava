package com.example.weatherapp.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {CityEntity.class,WeatherEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract CityDao cityDao();

    public abstract WeatherDao weatherDao();
}