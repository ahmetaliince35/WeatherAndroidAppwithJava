/*package com.example.weatherapp.data;

import androidx.room.Dao;
import androidx.room.Query;
import java.util.List;

@Dao
public interface CityDao {

    @Query("SELECT * FROM cities WHERE name LIKE :query || '%' ORDER BY name ASC LIMIT 10")
    List<CityEntity> searchCities(String query);
}*/