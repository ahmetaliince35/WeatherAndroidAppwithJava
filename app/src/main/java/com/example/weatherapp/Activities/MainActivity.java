package com.example.weatherapp.Activities;

import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_LONG;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.weatherapp.Adapters.FavoriCityAdapter;
import com.example.weatherapp.Helpers.FavoriteCitiesUpdater;
import com.example.weatherapp.Helpers.WeatherReceiver.OpenMeteoService;
import com.example.weatherapp.Helpers.WeatherReceiver.VisualCrossingService;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherAPIService;
import com.example.weatherapp.generalFeatures.WeatherWidgetProvider;
import com.example.weatherapp.generalFeatures.AutoUpdateConfig;
import com.example.weatherapp.Helpers.CitySearchDB;
import com.example.weatherapp.Helpers.FavoriCities;
import com.example.weatherapp.Helpers.UIUpdate;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherJsonAPI;
import com.example.weatherapp.generalFeatures.LocationGetter;
import com.example.weatherapp.generalFeatures.PreferencesManager;
import com.example.weatherapp.R;
import com.example.weatherapp.data.Databases.AppDatabase;
import com.example.weatherapp.data.Entities.WeatherEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private AutoCompleteTextView editTextCity;
    private Button buttonSearch;
    private Button buttonForecast;
    private Button buttonCurrentLocation;
    private Button buttonClearAll;
    private Button buttonHourlyForecast;
    private Button saveFavorites;
    private Button deleteFavories;
    private Button seePastedData;
    private TextView textViewCityName;
    private TextView textViewTemperature;
    private TextView textViewDescription;
    private TextView textViewHumidity;
    private TextView textViewWindSpeed;
    private TextView textViewPressure;
    private TextView textViewLastUpdate;
    private TextView textViewCloudiness;
    private TextView textViewAIAdvice;
    private TextView textViewopenFavoriCities;
    private ImageView imageViewWeatherIcon;
    private ImageView windDirect;
    private ScrollView root;
    private ProgressBar progressBar;
    private DrawerLayout drawerLayout;
    private RecyclerView recyclerFavorites;
    private List<WeatherEntity> favoriteList;
    private int currentbackround = R.drawable.background;

    private String currentCity = "";
    private String selectedCity;
    private boolean isNewSearch = false;
    private boolean isSaveLocation;
    private boolean isMGM = false;


    private String currentProvider = "OPEN_METEO";

    private LocationGetter locationGetter;
    private PreferencesManager preferencesManager;
    private WeatherJsonAPI.WeatherData tempData;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private AppDatabase citydb;
    private AppDatabase favoritesdb;
    FavoriCityAdapter favoriCityAdapter;
    private static final int Location_Permission_Request = 100;
    private static final String TAG = "Main Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        connectionDatabase();
        setupRecyclerView();

        locationGetter = new LocationGetter(this);
        preferencesManager = PreferencesManager.getInstance(this);
        currentProvider = preferencesManager.getSelectedProvider();

        AutoUpdateConfig.setupAutoUpdate(getApplicationContext());
        AutoUpdateConfig.setupAutoUpdateFavoriCities(getApplicationContext());

        textViewopenFavoriCities.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(Gravity.START);
            }
        });

        setupAutoComplete();
        loadSavedWeather();
        buttonClicked();
        notificationSettings();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.provider_owm) {
            currentProvider = "OWM";
            preferencesManager.saveSelectedProvider(currentProvider);
            Toast.makeText(this, "Veri Kaynağı: OpenWeatherMap", Toast.LENGTH_SHORT).show();
            WeatherWidgetProvider.updateAllWidgets(MainActivity.this);
            if (!currentCity.isEmpty()) {
                isNewSearch = true;
                fetchWeatherByCurrentProvider(currentCity);
            }
            return true;
        } else if (id == R.id.provider_open_meteo) {
            currentProvider = "OPEN_METEO";
            preferencesManager.saveSelectedProvider(currentProvider);
            Toast.makeText(this, "Veri Kaynağı: Open-Meteo", Toast.LENGTH_SHORT).show();
            WeatherWidgetProvider.updateAllWidgets(MainActivity.this);
            if (!currentCity.isEmpty()) {
                isNewSearch = true;
                fetchWeatherByCurrentProvider(currentCity);
            }
            return true;
        } else if (id == R.id.provider_weather_api) {
            currentProvider = "WEATHER_API";
            preferencesManager.saveSelectedProvider(currentProvider);
            Toast.makeText(this, "Veri Kaynağı: WeatherAPI", Toast.LENGTH_SHORT).show();
            WeatherWidgetProvider.updateAllWidgets(MainActivity.this);
            if (!currentCity.isEmpty()) {
                isNewSearch = true;
                fetchWeatherByCurrentProvider(currentCity);
            }
            return true;
        } else if (id == R.id.provider_visual_crossing) {
            currentProvider = "VISUAL_CROSSING";
            preferencesManager.saveSelectedProvider(currentProvider);
            preferencesManager.saveVisualCrossingModel("");
            Toast.makeText(this, "Veri Kaynağı: Visual Crossing", Toast.LENGTH_SHORT).show();
            WeatherWidgetProvider.updateAllWidgets(MainActivity.this);
            if (!currentCity.isEmpty()) {
                isNewSearch = true;
                fetchWeatherByCurrentProvider(currentCity);
            }
            return true;
        } else if (id == R.id.action_add_widget) {
            requestPinWidget();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void requestPinWidget() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.appwidget.AppWidgetManager appWidgetManager = getSystemService(android.appwidget.AppWidgetManager.class);
            android.content.ComponentName myProvider = new android.content.ComponentName(this, WeatherWidgetProvider.class);
            if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported()) {
                appWidgetManager.requestPinAppWidget(myProvider, null, null);
                Toast.makeText(this, "Widget ana ekrana ekleme penceresi açıldı.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Toast.makeText(this, "Ana ekranda boş bir yere basılı tutup 'Araçlar/Widget'lar' menüsünden weatherapp'i seçebilirsiniz.", Toast.LENGTH_LONG).show();
    }

    private void fetchWeatherByCurrentProvider(String cityName) {
        switch (currentProvider) {
            case "WEATHER_API":
                getWeatherFromWeatherAPI(cityName);
                break;
            case "OPEN_METEO":
                getWeatherFromOpenMeteoService(cityName);
                break;
            case "OWM":
                getWeatherfromOpenWeatherMap(cityName);
                break;
            case "VISUAL_CROSSING": // YENİ
                getWeatherFromVisualCrossing(cityName);
                break;
            default:
                getWeatherFromOpenMeteoService(cityName);
                break;
        }
    }

    private void initViews() {
        editTextCity = findViewById(R.id.editTextCity);

        buttonSearch = findViewById(R.id.buttonSearch);
        buttonForecast = findViewById(R.id.buttonForecast);
        buttonHourlyForecast = findViewById(R.id.buttonHourlyForecast);
        buttonCurrentLocation = findViewById(R.id.buttonCurrentLocation);
        buttonClearAll = findViewById(R.id.buttonClearAll);
        seePastedData = findViewById(R.id.btnStations);
        textViewCloudiness = findViewById(R.id.textViewCloudiness);
        textViewCityName = findViewById(R.id.textViewCityName);
        textViewTemperature = findViewById(R.id.textViewTemperature);
        textViewDescription = findViewById(R.id.textViewDescription);
        textViewHumidity = findViewById(R.id.textViewHumidity);
        textViewWindSpeed = findViewById(R.id.textViewWindSpeed);
        textViewPressure = findViewById(R.id.textViewPressure);
        textViewLastUpdate = findViewById(R.id.textViewLastUpdate);
        textViewAIAdvice = findViewById(R.id.TextViewAIAdvice);
        textViewopenFavoriCities = findViewById(R.id.openFavoriCities);
        progressBar = findViewById(R.id.progressBar);
        windDirect = findViewById(R.id.imageViewWindTurbine);
        imageViewWeatherIcon = findViewById(R.id.imageViewWeatherIcon);
        drawerLayout = findViewById(R.id.drawerlayout);
        recyclerFavorites = findViewById(R.id.recyclerFavorites);
        root = findViewById(R.id.rootScroll);
        saveFavorites = findViewById(R.id.buttonAddFavori);
        deleteFavories = findViewById(R.id.buttondeleteALlFavori);
    }

    private void connectionDatabase() {
        citydb = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "weather_db.db")
                .fallbackToDestructiveMigration()
                .createFromAsset("weather.db").build();

        favoritesdb = AppDatabase.getInstance(getApplicationContext());
    }

    private void setupRecyclerView() {
        favoriteList = new ArrayList<>();

        favoriCityAdapter = new FavoriCityAdapter(favoriteList, new FavoriCityAdapter.OnCityAction() {
            @Override
            public void onClick(WeatherEntity city) {
                currentCity = city.cityName;
                isNewSearch = true;
                isSaveLocation = false;
                fetchWeatherByCurrentProvider(currentCity);
                drawerLayout.closeDrawer(Gravity.START);
            }

            @Override
            public void onDelete(WeatherEntity city) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Şehri Sil")
                        .setMessage(city.cityName + " favorilerden silinsin mi?")
                        .setPositiveButton("Evet", (dialog, which) -> {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                favoritesdb.weatherDao().deleteByCity(city.cityName);
                            });
                        })
                        .setNegativeButton("Hayır", null)
                        .show();
            }
        });
        recyclerFavorites.setLayoutManager(new LinearLayoutManager(this));
        recyclerFavorites.setAdapter(favoriCityAdapter);
        favoritesdb.weatherDao().getAllFavorites().observe(this, newList -> {
            if (newList != null) {
                favoriteList.clear();
                favoriteList.addAll(newList);
                favoriCityAdapter.notifyDataSetChanged();
                Log.d("LIVE_DATA", "Liste otomatik güncellendi. Eleman sayısı: " + newList.size());
            }
        });
    }

    private void setupAutoComplete() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        editTextCity.setAdapter(adapter);

        editTextCity.setOnItemClickListener((parent, view, position, id) -> {
            selectedCity = adapter.getItem(position);
            if (selectedCity != null) {
                String displayCity = selectedCity.split(",")[0].trim();
                editTextCity.setText(displayCity);
                editTextCity.dismissDropDown();
                currentCity = displayCity;
            }
        });

        editTextCity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final String input = s.toString().trim();
                if (input.length() >= 2) {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        List<String> cityNames = CitySearchDB.GetCityList(input, citydb);

                        runOnUiThread(() -> {
                            adapter.clear();
                            if (cityNames != null && !cityNames.isEmpty()) {
                                adapter.addAll(cityNames);
                                adapter.notifyDataSetChanged();
                                editTextCity.showDropDown();
                            } else {
                                OpenMeteoService api = new OpenMeteoService(MainActivity.this);
                                api.searchGlobalLocation(input, new OpenMeteoService.GeoSearchCallback() {
                                    @Override
                                    public void onSuccess(List<OpenMeteoService.GeoSearchResult> results) {
                                        adapter.clear();
                                        for (OpenMeteoService.GeoSearchResult res : results) {
                                            adapter.add(res.toString());
                                        }
                                        adapter.notifyDataSetChanged();
                                        if (!results.isEmpty()) {
                                            editTextCity.showDropDown();
                                        }
                                    }

                                    @Override
                                    public void onError(String error) {}
                                });
                            }
                        });
                    });
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadSavedWeather() {
        if (preferencesManager.getSavedCityName() == null) return;

        currentProvider = preferencesManager.getSelectedProvider();

        if (preferencesManager.getIcon() != null) {
            updateBackgroundByWeather(preferencesManager.getIcon(), preferencesManager.getDesc());
        }
        currentCity = preferencesManager.getSavedCityName();
        buttonForecast.setEnabled(true);
        buttonHourlyForecast.setEnabled(true);

        updateUI(
                isMGM,
                preferencesManager.getAIAdvice(),
                preferencesManager.getSavedCityName(),
                preferencesManager.getTemp(),
                preferencesManager.getDesc(),
                preferencesManager.getHumidity(),
                preferencesManager.getWind(),
                preferencesManager.getPressure(),
                preferencesManager.getIcon(),
                preferencesManager.getWindDegree(),
                preferencesManager.getLastUpdateTime(),
                preferencesManager.getCloudiness()
        );
        WeatherWidgetProvider.updateAllWidgets(this);
        WeatherWidgetProvider.fetchHourlyForecastForWidget(this, preferencesManager);
    }

    private void buttonClicked() {
        buttonSearch.setOnClickListener(v -> {
            String city = editTextCity.getText().toString().trim();
            if (!city.isEmpty()) {
                currentCity = city;
                buttonClearAll.setEnabled(true);
                isNewSearch = true;

                fetchWeatherByCurrentProvider(currentCity);
            } else {
                Toast.makeText(MainActivity.this, "Lütfen şehir adı girin", Toast.LENGTH_SHORT).show();
            }
        });

        // 5 Günlük Tahmin Butonu (PROVIDER Gönderiliyor)
        buttonForecast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentCity.isEmpty()) {
                    Intent intent = new Intent(MainActivity.this, Forecastactivity.class);
                    String cleanCity = currentCity.contains("/") ? currentCity.split("/")[0].trim() : currentCity;

                    intent.putExtra("CITY_NAME", cleanCity);
                    intent.putExtra("PROVIDER", currentProvider); // YENİ
                    intent.putExtra("VC_MODEL", preferencesManager.getVisualCrossingModel());
                    intent.putExtra("background-res", currentbackround);
                    intent.putExtra("isNewSearch", isNewSearch);
                    intent.putExtra("isSaveLocation", isSaveLocation);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Önce bir şehir arayın", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 72 Saatlik Tahmin Butonu (PROVIDER Gönderiliyor)
        buttonHourlyForecast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentCity.isEmpty()) {
                    Intent intent = new Intent(MainActivity.this, Hourlyforecastactivity.class);
                    String cleanCity = currentCity.contains("/") ? currentCity.split("/")[0].trim() : currentCity;

                    intent.putExtra("CITY_NAME", cleanCity);
                    intent.putExtra("PROVIDER", currentProvider); // YENİ
                    intent.putExtra("VC_MODEL", preferencesManager.getVisualCrossingModel());
                    intent.putExtra("background-res", currentbackround);
                    intent.putExtra("isNewSearch", isNewSearch);
                    intent.putExtra("isSaveLocation", isSaveLocation);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Önce bir şehir arayın", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonClearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                preferencesManager.clearAll();

                currentCity = "";
                isSaveLocation = false;
                textViewAIAdvice.setText("Verilebilecek bir hava durumu tavsiyesi yok");
                textViewCityName.setText("Şehir yok");
                textViewTemperature.setText("--°C");
                textViewDescription.setText("---");
                textViewHumidity.setText("Nem: --%");
                textViewWindSpeed.setText("Rüzgar hızı: -- km/h");
                textViewPressure.setText("Basınç: -- hPa");
                textViewLastUpdate.setText("Son güncelleme: --");
                textViewCloudiness.setText("Bulut Oranı: --%");
                imageViewWeatherIcon.setImageResource(R.drawable.icon_sunny);
                root.setBackgroundResource(R.drawable.background);
                windDirect.setRotation(0);
            }
        });

        buttonCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPermission();
            }
        });

        saveFavorites.setOnClickListener(new View.OnClickListener() {
            FavoriCities favoriCities = new FavoriCities(favoritesdb);

            @Override
            public void onClick(View v) {
                if (tempData == null) {
                    Toast.makeText(MainActivity.this, "Önce bir şehir araması yapmalısın!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Executors.newSingleThreadExecutor().execute(() -> {
                    boolean isAdded = favoriCities.saveToFavorites(
                            currentCity,
                            tempData.temp,
                            tempData.description,
                            tempData.humidity,
                            tempData.windSpeed,
                            tempData.icon);

                    if (isAdded) {
                        runOnUiThread(() -> drawerLayout.openDrawer(Gravity.START));
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(getApplicationContext(), "Bu şehir zaten kayıtlı", Toast.LENGTH_SHORT).show();
                            drawerLayout.openDrawer(Gravity.START);
                        });
                    }
                });
            }
        });

        deleteFavories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Hepsini Sil")
                        .setMessage("Tüm favori şehirleriniz silinecek. Emin misiniz?")
                        .setPositiveButton("Evet, Sil", (dialog, which) -> {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                favoritesdb.weatherDao().deleteallcity();
                            });
                        })
                        .setNegativeButton("Vazgeç", null)
                        .show();
            }
        });

        seePastedData.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, pastDailyData.class);
            intent.putExtra("background-res", currentbackround);
            startActivity(intent);
        });
    }

    private void notificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher = registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            Log.d("MAINACTIVITY", "Bildirim izni verildi");
                            Toast.makeText(this, "Bildirimler aktif", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d("MAINACTIVITY", "Bildirim izni reddedildi");
                            Toast.makeText(this, "Bildirimler kapalı", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Bildirim izni isteniyor...");
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void getPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    Location_Permission_Request);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Location_Permission_Request) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Konum izni reddedildi", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation() {
        Toast.makeText(this, "Konum Alınıyor", Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(VISIBLE);
        locationGetter.getCurrentLocation(new LocationGetter.LocationCallback() {
            @Override
            public void onLocationReceived(String cityName) {
                currentCity = cityName;
                isNewSearch = true;
                fetchWeatherByCurrentProvider(currentCity);
                Toast.makeText(MainActivity.this, "Konum: " + cityName, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLocationError(String error) {
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void getWeatherFromVisualCrossing(String cityName) {
        progressBar.setVisibility(VISIBLE);
        VisualCrossingService repo = new VisualCrossingService(this);

        String cleanCity = cityName.contains("/") ? cityName.split("/")[0].trim() : cityName;
        String vcModel = preferencesManager.getVisualCrossingModel();

        repo.getCurrentWeather(cleanCity, vcModel, true, new VisualCrossingService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherJsonAPI.WeatherData data) {
                progressBar.setVisibility(View.INVISIBLE);

                updateUI(false, data.AIAdvice, data.cityName, data.temp, data.description,
                        data.humidity, data.windSpeed * 3.6, data.pressure, data.icon,
                        data.windDirection, System.currentTimeMillis(), data.cloudiness);

                fetchAIAdviceAsync(data.cityName, data.temp, data.description, data.icon);

                tempData = data;
                showSaveLocationDialog(cleanCity);
                buttonForecast.setEnabled(true);
                buttonHourlyForecast.setEnabled(true);
                updateBackgroundByWeather(data.icon, data.description);
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void getWeatherFromOpenMeteoService(String cityName) {
        progressBar.setVisibility(VISIBLE);
        OpenMeteoService repo = new OpenMeteoService(this);

        String cleanCity = cityName.contains("/") ? cityName.split("/")[0].trim() : cityName;
        repo.searchGlobalLocation(cleanCity, new OpenMeteoService.GeoSearchCallback() {
            @Override
            public void onSuccess(List<OpenMeteoService.GeoSearchResult> results) {
                if (!results.isEmpty()) {
                    OpenMeteoService.GeoSearchResult loc = results.get(0);

                    repo.getWeatherFromOpenMeteo(loc.latitude, loc.longitude, cityName, true, new OpenMeteoService.WeatherCallback() {
                        @Override
                        public void onSuccess(OpenMeteoService.WeatherData data) {
                            progressBar.setVisibility(View.INVISIBLE);

                            updateUI(false, data.AIAdvice, data.cityName, data.temp, data.description,
                                    data.humidity, data.windSpeed * 3.6, data.pressure, data.icon,
                                    data.windDirection, System.currentTimeMillis(), data.cloudiness);

                            fetchAIAdviceAsync(data.cityName, data.temp, data.description, data.icon);

                            updateBackgroundByWeather(data.icon, data.description);

                            tempData = new WeatherJsonAPI.WeatherData();
                            tempData.cityName = data.cityName;
                            tempData.temp = data.temp;
                            tempData.description = data.description;
                            tempData.humidity = data.humidity;
                            tempData.windSpeed = data.windSpeed;
                            tempData.icon = data.icon;
                            tempData.pressure = data.pressure;
                            tempData.AIAdvice = data.AIAdvice;
                            tempData.windDirection = data.windDirection;
                            tempData.cloudiness = data.cloudiness;
                            tempData.feelsLike = data.temp;

                            showSaveLocationDialog(cityName);
                            buttonForecast.setEnabled(true);
                            buttonHourlyForecast.setEnabled(true);
                        }

                        @Override
                        public void onError(String error) {
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(MainActivity.this, "Konum koordinatları bulunamadı.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(MainActivity.this, "Open-Meteo arama hatası.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void getWeatherFromWeatherAPI(String cityName) {
        progressBar.setVisibility(VISIBLE);
        WeatherAPIService repo = new WeatherAPIService(this);

        String cleanCity = cityName.contains("/") ? cityName.split("/")[0].trim() : cityName;
        repo.getCurrentWeather(cleanCity, true, new WeatherAPIService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherJsonAPI.WeatherData data) {
                progressBar.setVisibility(View.INVISIBLE);

                updateUI(false, data.AIAdvice, data.cityName, data.temp, data.description,
                        data.humidity, data.windSpeed * 3.6, data.pressure, data.icon,
                        data.windDirection, System.currentTimeMillis(), data.cloudiness);

                fetchAIAdviceAsync(data.cityName, data.temp, data.description, data.icon);

                tempData = data;
                showSaveLocationDialog(cleanCity);
                buttonForecast.setEnabled(true);
                buttonHourlyForecast.setEnabled(true);
                updateBackgroundByWeather(data.icon, data.description);
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getWeatherfromOpenWeatherMap(String city) {
        isMGM = false;
        final String il = city.contains("/") ? city.split("/")[0] : city;
        progressBar.setVisibility(VISIBLE);
        WeatherJsonAPI repo = new WeatherJsonAPI(this);
        repo.getWeatherfromOpenWEatherMAp(il, true, new WeatherJsonAPI.WeatherCallback() {
            @Override
            public void onSuccess(WeatherJsonAPI.WeatherData data) {
                progressBar.setVisibility(View.INVISIBLE);
                updateUI(isMGM, data.AIAdvice, data.cityName, data.temp, data.description, data.humidity, data.windSpeed * 3.6,
                        data.pressure, data.icon, data.windDirection, System.currentTimeMillis(), data.cloudiness);

                fetchAIAdviceAsync(data.cityName, data.temp, data.description, data.icon);

                tempData = data;
                showSaveLocationDialog(il);
                buttonForecast.setEnabled(true);
                buttonHourlyForecast.setEnabled(true);
                updateBackgroundByWeather(data.icon, data.description);
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSaveLocationDialog(String cityName) {
        new AlertDialog.Builder(this)
                .setTitle("Konum Kaydet")
                .setMessage(cityName + " konumunu varsayılan olarak kaydetmek ister misiniz?\n\n" +
                        "Kaydederseniz:\n" +
                        "• Uygulama açıldığında otomatik yüklenecek\n" +
                        "• Günde 2 kere otomatik güncellenecek")
                .setPositiveButton("Evet, Kaydet", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        preferencesManager.saveLocation(cityName);
                        preferencesManager.updateLastUpdateTime();
                        preferencesManager.saveSelectedProvider(currentProvider);
                        isSaveLocation = true;
                        if (tempData != null) {
                            preferencesManager.saveWeatherData(
                                    currentProvider,
                                    tempData.AIAdvice, tempData.cityName, tempData.temp, tempData.description,
                                    tempData.humidity, tempData.windSpeed * 3.6, tempData.feelsLike,
                                    tempData.pressure, tempData.icon, tempData.windDirection, tempData.cloudiness);

                            updateUI(isMGM, tempData.AIAdvice, tempData.cityName, tempData.temp, tempData.description,
                                    tempData.humidity, tempData.windSpeed * 3.6,
                                    tempData.pressure, tempData.icon, tempData.windDirection, preferencesManager.getLastUpdateTime(), tempData.cloudiness);
                        }
                        FavoriteCitiesUpdater.updateFavorites(MainActivity.this, null);
                        Toast.makeText(MainActivity.this,
                                cityName + " (" + currentProvider + ") varsayılan konum olarak kaydedildi",
                                LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Hayır", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        preferencesManager.saveSelectedProvider(currentProvider);
                        Toast.makeText(MainActivity.this,
                                "Konum kaydedilmedi.",
                                Toast.LENGTH_SHORT).show();
                        isSaveLocation = false;
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void fetchAIAdviceAsync(String cityName, double temp, String desc, String icon) {
        if (cityName == null || cityName.trim().isEmpty()) return;
        textViewAIAdvice.setText(R.string.ai_advice_waiting);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                com.example.weatherapp.Helpers.GeminiHelperPackages.GeminiPrompter ai =
                        new com.example.weatherapp.Helpers.GeminiHelperPackages.GeminiPrompter();
                String advice = ai.getWeatherAdvice(cityName, temp, desc, icon);
                runOnUiThread(() -> {
                    if (advice != null && !advice.isEmpty()) {
                        textViewAIAdvice.setText(advice);
                        if (tempData != null) {
                            tempData.AIAdvice = advice;
                        }
                        preferencesManager.saveAIAdvice(advice);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "AI Hatası: " + e.getMessage());
            }
        });
    }

    private void updateUI(boolean isMgm, String AIAdvice, String cityName,
                          double temperature, String description,
                          double humidity, double windSpeed, int pressure, String icon, float winddirection,
                          long timestamp, double cloudiness) {
        if (isMgm) {
            textViewCloudiness.setText(getString(R.string.rain_mm_label, cloudiness));
        } else {
            textViewCloudiness.setText(getString(R.string.cloudiness_label, cloudiness));
        }
        if (AIAdvice != null && !AIAdvice.isEmpty()) {
            textViewAIAdvice.setText(AIAdvice);
        } else {
            textViewAIAdvice.setText(R.string.ai_advice_waiting);
        }
        if (pressure == 0) textViewPressure.setText(R.string.no_data);
        else textViewPressure.setText(getString(R.string.pressure_label, (double) pressure));

        textViewCityName.setText(cityName != null ? cityName : " --");
        textViewTemperature.setText(getString(R.string.temperature_decimal_label, temperature));
        String safeDesc = "--";
        if (description != null && !description.trim().isEmpty()) {
            safeDesc = (description.length() > 1)
                    ? description.substring(0, 1).toUpperCase(new Locale("tr", "TR")) + description.substring(1).toLowerCase(new Locale("tr", "TR"))
                    : description.toUpperCase(new Locale("tr", "TR"));
        }
        textViewDescription.setText(safeDesc);
        textViewHumidity.setText(getString(R.string.humidity_label, humidity));
        textViewWindSpeed.setText(getString(R.string.wind_speed_label, windSpeed));
        windDirect.setRotation(winddirection + 180);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm", new Locale("tr", "TR"));
        String updateTime = sdf.format(new Date(timestamp));

        String providerName;
        switch (currentProvider != null ? currentProvider : "OPEN_METEO") {
            case "OWM":
                providerName = "OpenWeatherMap";
                break;
            case "WEATHER_API":
                providerName = "WeatherAPI";
                break;
            case "VISUAL_CROSSING":
                providerName = "Visual Crossing";
                break;
            case "MGM":
                providerName = "MGM";
                break;
            case "OPEN_METEO":
            default:
                providerName = "Open-Meteo";
                break;
        }

        // Son güncelleme metnine veri kaynağını da ekliyoruz
        textViewLastUpdate.setText(getString(R.string.last_update_label, updateTime, providerName));

        if (icon != null) setWeatherIcon(icon, description);

        WeatherWidgetProvider.updateAllWidgets(this);
        WeatherWidgetProvider.fetchHourlyForecastForWidget(this, preferencesManager, cityName);
    }

    private void setWeatherIcon(String icon, String description) {
        int iconResource = UIUpdate.setWeatherIcon(icon, description);
        imageViewWeatherIcon.setImageResource(iconResource);
    }

    private void updateBackgroundByWeather(String iconCode, String description) {
        int backgroundRes = UIUpdate.updateBackgroundByWeather(iconCode, description);
        currentbackround = backgroundRes;
        root.setBackgroundResource(currentbackround);
        recyclerFavorites.setBackgroundResource(R.drawable.general_component_background);
    }
}