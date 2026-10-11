package com.example.weatherapp.Helpers.WeatherReceiver;

import android.content.Context;

import com.example.weatherapp.Helpers.ForecastItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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


    public void getDailyForecast(String provider, String vcModel, String cityName, ForecastDataCallback callback) {
        if (provider == null || provider.isEmpty()) {
            provider = "OPEN_METEO";
        }

        ForecastDataCallback boundedCallback = new ForecastDataCallback() {
            @Override
            public void onSuccess(List<ForecastItem> forecastList) {
                if (forecastList != null && forecastList.size() > 5) {
                    forecastList = new java.util.ArrayList<>(forecastList.subList(0, 5));
                }
                callback.onSuccess(forecastList);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        };

        switch (provider.toUpperCase()) {
            case "OPEN_METEO":
                fetchOpenMeteoDaily(cityName, boundedCallback);
                break;

            case "WEATHER_API":
                fetchWeatherAPIDaily(cityName, boundedCallback);
                break;

            case "OWM":
                fetchOwmDaily(cityName, boundedCallback);
                break;
            case "VISUAL_CROSSING":
                fetchVisualCrossingDaily(cityName, vcModel, boundedCallback);
                break;

            default:
                fetchOpenMeteoDaily(cityName, boundedCallback);
                break;
        }
    }


    public void getHourlyForecast(String provider, String vcModel, String cityName, ForecastDataCallback callback) {
        if (provider == null || provider.isEmpty()) {
            provider = "OPEN_METEO";
        }

        ForecastDataCallback boundedCallback = new ForecastDataCallback() {
            @Override
            public void onSuccess(List<ForecastItem> forecastList) {
                if (forecastList != null) {
                    forecastList = filterFutureHours(forecastList, 72);
                }
                callback.onSuccess(forecastList);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        };

        switch (provider.toUpperCase()) {
            case "OPEN_METEO":
                fetchOpenMeteoHourly(cityName, boundedCallback);
                break;

            case "WEATHER_API":
                fetchWeatherAPIHourly(cityName, boundedCallback);
                break;
            case "VISUAL_CROSSING":
                fetchVisualCrossingHourly(cityName, vcModel, boundedCallback);
                break;

            case "OWM":
                fetchOwmHourly(cityName, boundedCallback);
                break;

            default:
                fetchOpenMeteoHourly(cityName, boundedCallback);
                break;
        }
    }


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
    private void fetchVisualCrossingDaily(String cityName, String model, ForecastDataCallback callback) {
        visualCrossingService.getDailyForecast(cityName, model, new VisualCrossingService.ForecastCallback() {
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

    private void fetchVisualCrossingHourly(String cityName, String model, ForecastDataCallback callback) {
        visualCrossingService.getHourlyForecast(cityName, model, new VisualCrossingService.ForecastCallback() {
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

    private List<ForecastItem> filterFutureHours(List<ForecastItem> list, int maxCount) {
        if (list == null || list.isEmpty()) return list;
        long nowThreshold = System.currentTimeMillis() - (50 * 60 * 1000L);
        List<ForecastItem> filtered = new ArrayList<>();
        boolean foundStart = false;

        for (ForecastItem item : list) {
            if (item == null) continue;
            long itemTime = parseTimestamp(item.getDate());
            if (!foundStart) {
                if (itemTime == 0 || itemTime >= nowThreshold) {
                    foundStart = true;
                    filtered.add(item);
                }
            } else {
                filtered.add(item);
            }
            if (filtered.size() >= maxCount) break;
        }

        return filtered.isEmpty() ? list : filtered;
    }

    private long parseTimestamp(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) return 0;
        String cleaned = rawDate.trim().replace("T", " ");

        if (cleaned.length() >= 16 && cleaned.charAt(4) == '-' && cleaned.charAt(7) == '-') {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                Date d = sdf.parse(cleaned.substring(0, 16));
                if (d != null) return d.getTime();
            } catch (Exception ignored) {}
        }

        if (cleaned.contains(".")) {
            try {
                String datePart = cleaned.contains("-") ? cleaned.split("-")[0].trim() : cleaned;
                String[] parts = datePart.split(" ");
                if (parts.length >= 3) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH.mm", new Locale("tr", "TR"));
                    Date d = sdf.parse(parts[0] + " " + parts[1] + " " + parts[2]);
                    if (d != null) {
                        Calendar cal = Calendar.getInstance();
                        Calendar parsedCal = Calendar.getInstance();
                        parsedCal.setTime(d);
                        cal.set(Calendar.MONTH, parsedCal.get(Calendar.MONTH));
                        cal.set(Calendar.DAY_OF_MONTH, parsedCal.get(Calendar.DAY_OF_MONTH));
                        cal.set(Calendar.HOUR_OF_DAY, parsedCal.get(Calendar.HOUR_OF_DAY));
                        cal.set(Calendar.MINUTE, parsedCal.get(Calendar.MINUTE));
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        return cal.getTimeInMillis();
                    }
                }
            } catch (Exception ignored) {}
        }

        try {
            long val = Long.parseLong(cleaned);
            return val < 10000000000L ? val * 1000L : val;
        } catch (Exception ignored) {}

        return 0;
    }
}