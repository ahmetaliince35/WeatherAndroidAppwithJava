package com.example.weatherapp.data.Daos;

import androidx.room.Dao;
import androidx.room.Query;

import com.example.weatherapp.data.Entities.CityEntity;

import java.util.List;

@Dao
public interface CityDao {

    @Query("SELECT * FROM cities WHERE name LIKE :prefix ||'%'  COLLATE NOCASE")
    List<CityEntity> getCitiesStartingWith(String prefix);

}