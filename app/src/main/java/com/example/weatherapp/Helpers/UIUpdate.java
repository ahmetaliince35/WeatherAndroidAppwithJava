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
                iconResource = R.drawable.icon_rainy;
                break;
            case "11d":
            case "11n":
                iconResource = R.drawable.icon_thunderstorm;
                break;
            case "13d":
            case "13n":
                iconResource = R.drawable.icon_snowy;
                break;
            default:
                iconResource = R.drawable.icon_sunny;
                break;
        }
        return iconResource;
    }
    public static int updateBackgroundByWeather(String iconCode) {
        int backgroundRes;

        if (iconCode.startsWith("01")) {
            backgroundRes = iconCode.endsWith("d")
                    ? R.drawable.sun
                    : R.drawable.moon;

        }
        else if (iconCode.startsWith("02") || iconCode.startsWith("03"))
        {
            backgroundRes=R.drawable.partlycloud;
        }
        else if (iconCode.startsWith("04"))
        {
            backgroundRes = R.drawable.very_cloud;

        }
        else if (iconCode.startsWith("09") || iconCode.startsWith("10"))
        {
            backgroundRes = R.drawable.rain;

        }
        else if(iconCode.startsWith("11"))
        {
            backgroundRes= R.drawable.thunder;

        }
        else if (iconCode.startsWith("13"))
        {
            backgroundRes = R.drawable.snow;

        }
        else
        {
            backgroundRes = R.drawable.sun;
        }
        return backgroundRes;
    }
}
