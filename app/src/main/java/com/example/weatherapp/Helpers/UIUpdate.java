package com.example.weatherapp.Helpers;

import com.example.weatherapp.R;

public class UIUpdate {

    public static int setWeatherIcon(String icon,String description) {
        int iconResource;
        switch (icon) {
            case "01d":
                iconResource = R.drawable.icon_sunny;
                break;
            case "01n":
                iconResource=R.drawable.icon_moon;
                break;
            case "02d":
            case "03d":
                iconResource = R.drawable.icon_partlycloudy;
                break;
            case "02n":
            case "03n":
                iconResource=R.drawable.icon_partlycloudy_night;
                break;
            case "04d":
                if(description.contains("parçalı"))
                {
                    iconResource=R.drawable.icon_partlycloudy;
                }
                else {
                    iconResource = R.drawable.icon_cloudy;
                }
                break;
            case "04n":
                if(description.contains("parçalı"))
                {
                    iconResource=R.drawable.icon_partlycloudy_night;
                }
                else {
                    iconResource = R.drawable.icon_cloudy;
                }
                break;
            case "09d":
            case "09n":
            case "10d":
            case "10n":
            case "11d":
            case "11n":
                iconResource = R.drawable.icon_rainy;
                break;
            case "13d":
            case "13n":
                iconResource = R.drawable.icon_snowy;
                break;
            default:
                iconResource = R.drawable.icon_partlycloudy;
                break;
        }
        return iconResource;
    }
    public static int updateBackgroundByWeather(String iconCode,String description) {
        int backgroundRes;

        if(iconCode.endsWith("n"))
        {
            backgroundRes=R.drawable.moony;
        }
        else if (iconCode.startsWith("01") && iconCode.endsWith("d")) {
            backgroundRes= R.drawable.sunny;

        }
        else if (iconCode.startsWith("02") || iconCode.startsWith("03"))
        {
            backgroundRes=R.drawable.partlycloudy;
        }
        else if (iconCode.startsWith("04"))
        {
            if(description.contains("parçalı"))
            {
                backgroundRes=R.drawable.partlycloudy;
            }
            else {
                backgroundRes=R.drawable.very_cloudy;
            }
        }
        else if (iconCode.startsWith("09") || iconCode.startsWith("10") || iconCode.startsWith("11"))
        {
            backgroundRes=R.drawable.rainy;
        }
        else if (iconCode.startsWith("13"))
        {
            backgroundRes=R.drawable.snowy;
        }
        else
        {
            backgroundRes = R.drawable.partlycloudy;
        }
        return backgroundRes;
    }
}
