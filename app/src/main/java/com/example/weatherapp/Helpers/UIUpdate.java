package com.example.weatherapp.Helpers;

import com.example.weatherapp.R;
import java.util.Locale;

public class UIUpdate {

    public static int setWeatherIcon(String icon, String description) {
        if (description == null) description = "";
        if (icon == null) icon = "";

        String descLower = description.toLowerCase(Locale.forLanguageTag("tr"));
        String iconLower = icon.toLowerCase(Locale.US);

        // 1. VISUAL CROSSING ÖZEL İKON EŞLEŞTİRMESİ
        if (iconLower.contains("clear-day")) return R.drawable.icon_sunny;
        if (iconLower.contains("clear-night")) return R.drawable.icon_moon;
        if (iconLower.contains("partly-cloudy-day")) return R.drawable.icon_partlycloudy;
        if (iconLower.contains("partly-cloudy-night")) return R.drawable.icon_partlycloudy_night;
        if (iconLower.contains("rain") || iconLower.contains("showers")) return R.drawable.icon_rainy;
        if (iconLower.contains("snow") || iconLower.contains("sleet")) return R.drawable.icon_snowy;
        if (iconLower.contains("thunder")) return R.drawable.icon_thunderstorm;
        if (iconLower.contains("cloudy") || iconLower.contains("fog") || iconLower.contains("wind")) return R.drawable.icon_cloudy;

        // 2. TÜRKÇE AÇIKLAMA KELİME YAKALAMA (Open-Meteo & MGM İçin)
        if (descLower.contains("açık")) {
            return (icon.endsWith("n") || iconLower.contains("night")) ? R.drawable.icon_moon : R.drawable.icon_sunny;
        }
        if (descLower.contains("parçalı") || descLower.contains("az bulutlu")) {
            return (icon.endsWith("n") || iconLower.contains("night")) ? R.drawable.icon_partlycloudy_night : R.drawable.icon_partlycloudy;
        }
        if (descLower.contains("yağmur") || descLower.contains("sağanak") || descLower.contains("yağışlı")) {
            return R.drawable.icon_rainy;
        }
        if (descLower.contains("kar")) {
            return R.drawable.icon_snowy;
        }
        if (descLower.contains("sis") || descLower.contains("pus") || descLower.contains("bulutlu")) {
            return R.drawable.icon_cloudy;
        }
        if (descLower.contains("fırtına") || descLower.contains("gökgürültü") || descLower.contains("dolu")) {
            return R.drawable.icon_thunderstorm;
        }

        // 3. OWM İKON KODU EŞLEME
        switch (icon) {
            case "01d": return R.drawable.icon_sunny;
            case "01n": return R.drawable.icon_moon;
            case "02d": case "03d": return R.drawable.icon_partlycloudy;
            case "02n": case "03n": return R.drawable.icon_partlycloudy_night;
            case "04d": case "04n": case "50d": case "50n": return R.drawable.icon_cloudy;
            case "09d": case "09n": case "10d": case "10n": return R.drawable.icon_rainy;
            case "11d": case "11n": return R.drawable.icon_thunderstorm;
            case "13d": case "13n": return R.drawable.icon_snowy;
        }

        return R.drawable.icon_partlycloudy;
    }

    public static int updateBackgroundByWeather(String iconCode, String description) {
        if (description == null) description = "";
        if (iconCode == null) iconCode = "";

        String descLower = description.toLowerCase(Locale.forLanguageTag("tr"));
        String iconLower = iconCode.toLowerCase(Locale.US);

        // Gece Kontrolü
        if (iconCode.endsWith("n") || iconLower.contains("night")) {
            return R.drawable.moony;
        }

        // Visual Crossing & Genel İkon Kontrolü
        if (iconLower.contains("clear-day") || descLower.contains("açık")) return R.drawable.sunny;
        if (iconLower.contains("partly-cloudy") || descLower.contains("parçalı") || descLower.contains("az bulutlu")) return R.drawable.partlycloudy;
        if (iconLower.contains("rain") || iconLower.contains("showers") || descLower.contains("yağmur") || descLower.contains("sağanak")) return R.drawable.rainy;
        if (iconLower.contains("snow") || descLower.contains("kar")) return R.drawable.snowy;
        if (iconLower.contains("cloudy") || iconLower.contains("fog") || descLower.contains("sis") || descLower.contains("bulutlu")) return R.drawable.very_cloudy;

        return R.drawable.partlycloudy;
    }
}