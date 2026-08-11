package com.example.weatherapp.data;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

@Dao
public interface StationDao {
    @Query("UPDATE stations SET name = REPLACE(name, '?', 'ı') WHERE name LIKE '%?%'")
    void fixQuestionMarksToI();

    @Query("SELECT * FROM stations ORDER BY name ASC")
    List<StationEntity> getAllStations();

    @Query("SELECT * FROM stations WHERE name LIKE :query ORDER BY name ASC")
    List<StationEntity> searchStations(String query);
}