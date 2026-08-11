package com.example.weatherapp.Helpers.WeatherReceiver;

import android.content.Context;

import com.example.weatherapp.Helpers.ForecastItem;

import java.util.List;

public class WeatherRepository {

    private final Context context;
    private final OpenMeteoService openMeteoService;
    private final WeatherJsonAPI weatherJsonAPI;
    private final WeatherAPIService weatherAPIService;
    private final VisualCrossingService visualCrossingService;

    public interface ForecastDataCallback {
        void onSuccess(List<ForecastItem> forecastList);
        void onError(String error);
    }

    public WeatherRepository(Context context) {
        this.context = context;
        this.openMeteoService = new OpenMeteoService(context);
        this.weatherJsonAPI = new WeatherJsonAPI(context);
        this.weatherAPIService=new WeatherAPIService(context);
        this.visualCrossingService= new VisualCrossingService(context);
    }

    // --- GÜNLÜK TAHMİN ÇEKİCİ ---
    public void getDailyForecast(String provider, String cityName, ForecastDataCallback callback) {
        if (provider == null || provider.isEmpty()) {
            provider = "OPEN_METEO";
        }

        switch (provider.toUpperCase()) {
            case "OPEN_METEO":
                fetchOpenMeteoDaily(cityName, callback);
                break;

            case "WEATHER_API":
                fetchWeatherAPIDaily(cityName, callback);
                break;

            case "OWM":
                fetchOwmDaily(cityName, callback);
                break;
            case "VISUAL_CROSSING": // YENİ
                fetchVisualCrossingDaily(cityName, callback);
                break;

            default:
                // Bilinmeyen veya boş bir kaynak gelirse varsayılan olarak Open-Meteo çalışır
                fetchOpenMeteoDaily(cityName, callback);
                break;
        }
    }

    // --- SAATLİK TAHMİN ÇEKİCİ ---
    public void getHourlyForecast(String provider, String cityName, ForecastDataCallback callback) {
        if (provider == null || provider.isEmpty()) {
            provider = "OPEN_METEO";
        }

        switch (provider.toUpperCase()) {
            case "OPEN_METEO":
                fetchOpenMeteoHourly(cityName, callback);
                break;

            case "WEATHER_API":
                fetchWeatherAPIHourly(cityName, callback);
                break;
            case "VISUAL_CROSSING": // YENİ
                fetchVisualCrossingHourly(cityName, callback);
                break;

            case "OWM":
                fetchOwmHourly(cityName, callback);
                break;

            default:
                fetchOpenMeteoHourly(cityName, callback);
                break;
        }
    }

    // --- PRIVATE YARDIMCI METOTLAR (Sadece burada yönetilir) ---
    // YENİ HELPER METOTLAR:
    private void fetchWeatherAPIDaily(String cityName, ForecastDataCallback callback) {
        weatherAPIService.getDailyForecast(cityName, new WeatherAPIService.WeatherAPICallback() {
            @Override
            public void onSuccess(List<ForecastItem> forecastList) {
                callback.onSuccess(forecastList);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private void fetchWeatherAPIHourly(String cityName, ForecastDataCallback callback) {
        weatherAPIService.getHourlyForecast(cityName, new WeatherAPIService.WeatherAPICallback() {
            @Override
            public void onSuccess(List<ForecastItem> forecastList) {
                callback.onSuccess(forecastList);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private void fetchOpenMeteoDaily(String cityName, ForecastDataCallback callback) {
        openMeteoService.searchGlobalLocation(cityName, new OpenMeteoService.GeoSearchCallback() {
            @Override
            public void onSuccess(List<OpenMeteoService.GeoSearchResult> results) {
                if (!results.isEmpty()) {
                    OpenMeteoService.GeoSearchResult loc = results.get(0);
                    openMeteoService.getDailyForecast(loc.latitude, loc.longitude, new OpenMeteoService.ForecastCallback() {
                        @Override
                        public void onSuccess(List<ForecastItem> forecastList) {
                            callback.onSuccess(forecastList);
                        }

                        @Override
                        public void onError(String error) {
                            callback.onError(error);
                        }
                    });
                } else {
                    callback.onError("Konum bulunamadı");
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private void fetchOwmDaily(String cityName, ForecastDataCallback callback) {
        weatherJsonAPI.getDailyForecast(cityName, new WeatherJsonAPI.ForecastCallback() {
            @Override
            public void onSuccess(List<ForecastItem> forecastList, String json) {
                callback.onSuccess(forecastList);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private void fetchOpenMeteoHourly(String cityName, ForecastDataCallback callback) {
        openMeteoService.searchGlobalLocation(cityName, new OpenMeteoService.GeoSearchCallback() {
            @Override
            public void onSuccess(List<OpenMeteoService.GeoSearchResult> results) {
                if (!results.isEmpty()) {
                    OpenMeteoService.GeoSearchResult loc = results.get(0);
                    openMeteoService.getHourlyForecast(loc.latitude, loc.longitude, new OpenMeteoService.ForecastCallback() {
                        @Override
                        public void onSuccess(List<ForecastItem> forecastList) {
                            callback.onSuccess(forecastList);
                        }

                        @Override
                        public void onError(String error) {
                            callback.onError(error);
                        }
                    });
                } else {
                    callback.onError("Konum bulunamadı");
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private void fetchOwmHourly(String cityName, ForecastDataCallback callback) {
        weatherJsonAPI.getHourlyForecast(cityName, new WeatherJsonAPI.HourlyCallback() {
            @Override
            public void onSuccess(List<ForecastItem> hourlyList, String json) {
                callback.onSuccess(hourlyList);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    private void fetchVisualCrossingDaily(String cityName, ForecastDataCallback callback) {
        visualCrossingService.getDailyForecast(cityName, new VisualCrossingService.ForecastCallback() {
            @Override
            public void onSuccess(List<ForecastItem> forecastList) {
                callback.onSuccess(forecastList);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private void fetchVisualCrossingHourly(String cityName, ForecastDataCallback callback) {
        visualCrossingService.getHourlyForecast(cityName, new VisualCrossingService.ForecastCallback() {
            @Override
            public void onSuccess(List<ForecastItem> forecastList) {
                callback.onSuccess(forecastList);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}