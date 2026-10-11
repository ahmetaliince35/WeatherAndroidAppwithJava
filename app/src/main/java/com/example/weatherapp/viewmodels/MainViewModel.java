package com.example.weatherapp.viewmodels;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.weatherapp.Helpers.FavoriCities;
import com.example.weatherapp.Helpers.FavoriteCitiesUpdater;
import com.example.weatherapp.Helpers.GeminiHelperPackages.GeminiPrompter;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherCallback;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherData;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherRepository;
import com.example.weatherapp.data.Databases.AppDatabase;
import com.example.weatherapp.data.Entities.WeatherEntity;
import com.example.weatherapp.generalFeatures.PreferencesManager;
import com.example.weatherapp.generalFeatures.WeatherWidgetProvider;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainViewModel extends AndroidViewModel {

    private final WeatherRepository weatherRepository;
    private final PreferencesManager preferencesManager;
    private final AppDatabase favoritesDb;
    private final FavoriCities favoriCities;
    private final GeminiPrompter geminiPrompter;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private final MutableLiveData<WeatherData> currentWeather = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> aiAdvice = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        this.weatherRepository = new WeatherRepository(application);
        this.preferencesManager = PreferencesManager.getInstance(application);
        this.favoritesDb = AppDatabase.getInstance(application);
        this.favoriCities = new FavoriCities(favoritesDb);
        this.geminiPrompter = new GeminiPrompter(application);
        this.executorService = Executors.newFixedThreadPool(2);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public LiveData<WeatherData> getCurrentWeather() {
        return currentWeather;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getAiAdvice() {
        return aiAdvice;
    }

    public LiveData<List<WeatherEntity>> getFavorites() {
        return favoritesDb.weatherDao().getAllFavorites();
    }

    public void loadWeather(String cityName, String provider, String model) {
        if (cityName == null || cityName.trim().isEmpty()) {
            errorMessage.setValue("Lütfen geçerli bir şehir adı girin.");
            return;
        }

        isLoading.setValue(true);
        weatherRepository.getCurrentWeather(provider, cityName, model, new WeatherCallback<WeatherData>() {
            @Override
            public void onSuccess(WeatherData data) {
                isLoading.setValue(false);
                currentWeather.setValue(data);
                fetchAiAdviceAsync(data);
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void fetchAiAdviceAsync(WeatherData data) {
        if (data == null || data.cityName == null) return;
        aiAdvice.setValue("Yapay zeka tavsiyesi hazırlanıyor...");

        executorService.execute(() -> {
            try {
                String advice = geminiPrompter.getWeatherAdvice(data.cityName, data.temp, data.description, data.icon);
                mainHandler.post(() -> {
                    if (advice != null && !advice.isEmpty()) {
                        aiAdvice.setValue(advice);
                        data.aiAdvice = advice;
                        preferencesManager.saveAIAdvice(advice);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> aiAdvice.setValue("Tavsiye alınırken bir sorun oluştu."));
            }
        });
    }

    public void loadSavedWeather() {
        String savedCity = preferencesManager.getSavedCityName();
        if (savedCity == null || savedCity.trim().isEmpty()) return;

        WeatherData data = new WeatherData();
        data.cityName = savedCity;
        data.temp = preferencesManager.getTemp();
        data.feelsLike = preferencesManager.getFeelsLike();
        data.description = preferencesManager.getDesc();
        data.humidity = preferencesManager.getHumidity();
        data.windSpeed = preferencesManager.getWind();
        data.pressure = preferencesManager.getPressure();
        data.icon = preferencesManager.getIcon();
        data.windDirection = preferencesManager.getWindDegree();
        data.cloudiness = preferencesManager.getCloudiness();
        data.timestamp = preferencesManager.getLastUpdateTime();
        data.aiAdvice = preferencesManager.getAIAdvice();

        currentWeather.setValue(data);
        if (data.aiAdvice != null && !data.aiAdvice.isEmpty()) {
            aiAdvice.setValue(data.aiAdvice);
        }

        WeatherWidgetProvider.updateAllWidgets(getApplication());
        WeatherWidgetProvider.fetchHourlyForecastForWidget(getApplication(), preferencesManager, savedCity);
    }

    public void saveDefaultLocation(String cityName, WeatherData data, String provider) {
        preferencesManager.saveLocation(cityName);
        preferencesManager.saveSelectedProvider(provider);
        preferencesManager.updateLastUpdateTime();

        if (data != null) {
            preferencesManager.saveWeatherData(
                    provider,
                    data.aiAdvice,
                    data.cityName,
                    data.temp,
                    data.description,
                    data.humidity,
                    data.windSpeed,
                    data.feelsLike,
                    data.pressure,
                    data.icon,
                    data.windDirection,
                    data.cloudiness
            );
        }
        FavoriteCitiesUpdater.updateFavorites(getApplication(), null);
    }

    public void addToFavorites(WeatherData data, Runnable onSuccess, Runnable onDuplicate) {
        if (data == null) return;
        executorService.execute(() -> {
            boolean isAdded = favoriCities.saveToFavorites(
                    data.cityName,
                    data.temp,
                    data.description,
                    data.humidity,
                    data.windSpeed,
                    data.icon
            );
            mainHandler.post(isAdded ? onSuccess : onDuplicate);
        });
    }

    public void deleteFavorite(String cityName) {
        if (cityName == null) return;
        executorService.execute(() -> favoritesDb.weatherDao().deleteByCity(cityName));
    }

    public void clearAllFavorites() {
        executorService.execute(() -> favoritesDb.weatherDao().deleteallcity());
    }

    public void clearAllSavedData() {
        preferencesManager.clearAll();
        currentWeather.setValue(null);
        aiAdvice.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
