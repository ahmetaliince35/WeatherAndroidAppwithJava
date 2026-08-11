package com.example.weatherapp.data.Databases;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.weatherapp.data.Daos.CityDao;
import com.example.weatherapp.data.Entities.CityEntity;
import com.example.weatherapp.data.Daos.WeatherDao;
import com.example.weatherapp.data.Entities.WeatherEntity;

@Database(entities = {CityEntity.class, WeatherEntity.class}, version = 6, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    public abstract CityDao cityDao();

    public abstract WeatherDao weatherDao();
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) { // thread-safe
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "weather_db"  // DB adı
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }

}