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
                switch(icon)
                {
                    case "Açık":
                        iconResource= R.drawable.icon_sunny;
                        break;
                    case "Az Bulutlu":
                    case "Parçalı Bulutlu":
                        iconResource=R.drawable.icon_partlycloudy;
                        break;
                    case "Hafif Yağmurlu":
                    case "Yağmurlu":
                    case "Kuvvetli Yağmurlu":
                    case "Hafif Sağanak Yağışlı":
                    case "Sağanak Yağışlı":
                    case "Kuvvetli Sağanak Yağışlı":
                    case "Yer Yer Sağanak Yağışlı":
                        iconResource=R.drawable.icon_rainy;
                        break;
                    case "Hafif Kar Yağışlı":
                    case "Kar Yağışlı":
                    case "Yoğun Kar Yağışlı":
                    case "Karla Karışık Yağmurlu":
                        iconResource=R.drawable.snowy;
                        break;
                    case "Sis":
                    case "Pus":
                    case "Duman":
                    case "Çok Bulutlu":
                        iconResource=R.drawable.icon_cloudy;
                        break;
                    case "Dolu":
                    case "Gökgürültülü Sağanak Yağışlı":
                    case "Kuvvetli Gökgürültülü Sağanak Yağışlı":
                        iconResource=R.drawable.icon_thunderstorm;
                        break;
                    default: iconResource=R.drawable.partlycloudy;
                        break;
                }

        }
        return iconResource;
    }
    public static int updateBackgroundByWeather(String iconCode, String description) {
        int backgroundRes = R.drawable.partlycloudy;

        if (iconCode.endsWith("n")) {
            backgroundRes = R.drawable.moony;
        }
        else if ((iconCode.startsWith("01") && iconCode.endsWith("d")) || "Açık".equals(iconCode)) {
            backgroundRes = R.drawable.sunny;
        }
        else if (iconCode.startsWith("02") || iconCode.startsWith("03")
                || "Az Bulutlu".equals(iconCode)
                || "Parçalı Bulutlu".equals(iconCode)) {
            backgroundRes = R.drawable.partlycloudy;
        }
        else if (iconCode.startsWith("04")|| "Çok Bulutlu".equals(iconCode)) {
            if (description.toLowerCase().contains("parçalı")) {
                backgroundRes = R.drawable.partlycloudy;
            }
            else  {
                backgroundRes = R.drawable.very_cloudy;
            }
        }
        else if (iconCode.startsWith("09")
                || iconCode.startsWith("10")
                || iconCode.startsWith("11")
                || "Hafif Yağmurlu".equals(iconCode)
                || "Yağmurlu".equals(iconCode)
                || "Kuvvetli Yağmurlu".equals(iconCode)
                || "Hafif Sağanak Yağışlı".equals(iconCode)
                || "Sağanak Yağışlı".equals(iconCode)
                || "Kuvvetli Sağanak Yağışlı".equals(iconCode)
                || "Yer Yer Sağanak Yağışlı".equals(iconCode)) {
            backgroundRes = R.drawable.rainy;
        }
        else if (iconCode.startsWith("13")
                || "Hafif Kar Yağışlı".equals(iconCode)
                || "Kar Yağışlı".equals(iconCode)
                || "Yoğun Kar Yağışlı".equals(iconCode)
                || "Karla Karışık Yağmurlu".equals(iconCode)) {
            backgroundRes = R.drawable.snowy;
        }

        return backgroundRes;
    }
}
