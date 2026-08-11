package com.example.weatherapp.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {StationEntity.class},
        version = 2,
        exportSchema = false
)
public abstract class StationDatabase extends RoomDatabase {

    private static volatile StationDatabase INSTANCE;

    public abstract StationDao stationDao();

    public static StationDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (StationDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    StationDatabase.class,
                                    "stations.db"
                            )
                            .createFromAsset("stations.db")
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}