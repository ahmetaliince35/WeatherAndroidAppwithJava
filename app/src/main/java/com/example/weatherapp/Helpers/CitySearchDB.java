package com.example.weatherapp.Helpers;
import com.example.weatherapp.data.AppDatabase;
import com.example.weatherapp.data.CityEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CitySearchDB
{
    public static List<String> GetCityList(String input, AppDatabase db) {

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

            cityNames.add(
                    toTitleCaseTr(c.name) +
                            "/" +
                            toTitleCaseTr(c.province) +
                            ", " +
                            countryName
            );
        }
        return cityNames;
    }
    private static String toTitleCaseTr(String text) {
        if(text==null)
            return "";

        Locale tr = new Locale("tr", "TR");

        text = text.toLowerCase(tr);

        return text.substring(0, 1).toUpperCase(tr) + text.substring(1);
    }
}



