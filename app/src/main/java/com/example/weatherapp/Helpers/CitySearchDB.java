package com.example.weatherapp.Helpers;
import com.example.weatherapp.data.AppDatabase;
import com.example.weatherapp.data.CityEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CitySearchDB
{
    public static List<String> GetCityList(String input, AppDatabase db)
    {

            List<CityEntity> cities;
            if (!input.isEmpty()) {
                cities = db.cityDao().getCitiesStartingWith(input);
            } else {
                cities = new ArrayList<>();
            }

            List<String> cityNames = new ArrayList<>();
            for (CityEntity c : cities) {
                Locale locale = new Locale("", c.country);
                String countryName = locale.getDisplayCountry();
                cityNames.add(c.name + " , " + countryName);
            }
            return cityNames;
    }
}



