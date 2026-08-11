package com.example.weatherapp.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "stations",
        indices = {@Index(value = {"name"})}
)
public class StationEntity {

    @PrimaryKey
    @NonNull
    public String id;

    @NonNull
    public String name;

    public String country;

    public StationEntity(@NonNull String id, @NonNull String name, String country) {
        this.id = id;
        this.name = name;
        this.country = country;
    }
}