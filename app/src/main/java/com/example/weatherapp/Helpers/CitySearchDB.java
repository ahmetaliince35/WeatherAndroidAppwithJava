package com.example.weatherapp.Helpers;
import com.example.weatherapp.data.Databases.AppDatabase;
import com.example.weatherapp.data.Entities.CityEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CitySearchDB
{
    public static List<String> GetCityList(String input, AppDatabase db) {
        if (input == null || db == null) return new ArrayList<>();

        List<CityEntity> cities;
        if (!input.trim().isEmpty()) {
            cities = db.cityDao().getCitiesStartingWith(input.trim());
        } else {
            cities = new ArrayList<>();
        }

        List<String> cityNames = new ArrayList<>();
        for (CityEntity c : cities) {
            if (c == null) continue;
            Locale locale = new Locale("", c.country != null ? c.country : "");
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
        if (text == null)
            return "";

        Locale tr = new Locale("tr", "TR");

        text = text.toLowerCase(tr).trim();
        if (text.isEmpty()) return "";

        return text.substring(0, 1).toUpperCase(tr) + text.substring(1);
    }
}



