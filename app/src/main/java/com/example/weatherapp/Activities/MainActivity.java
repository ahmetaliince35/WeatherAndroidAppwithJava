package com.example.weatherapp.Activities;

import android.Manifest;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.weatherapp.Adapters.FavoriCityAdapter;
import com.example.weatherapp.Helpers.CitySearchDB;
import com.example.weatherapp.Helpers.UIUpdate;
import com.example.weatherapp.Helpers.WeatherReceiver.OpenMeteoService;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherData;
import com.example.weatherapp.R;
import com.example.weatherapp.data.Databases.AppDatabase;
import com.example.weatherapp.data.Entities.WeatherEntity;
import com.example.weatherapp.generalFeatures.AutoUpdateConfig;
import com.example.weatherapp.generalFeatures.LocationGetter;
import com.example.weatherapp.generalFeatures.PreferencesManager;
import com.example.weatherapp.generalFeatures.WeatherWidgetProvider;
import com.example.weatherapp.viewmodels.MainViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST = 100;

    private AutoCompleteTextView editTextCity;
    private Button buttonSearch;
    private Button buttonForecast;
    private Button buttonHourlyForecast;
    private Button buttonCurrentLocation;
    private Button buttonClearAll;
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
    private TextView textViewOpenFavoriCities;
    private ImageView imageViewWeatherIcon;
    private ImageView windDirect;
    private ScrollView root;
    private ProgressBar progressBar;
    private DrawerLayout drawerLayout;
    private RecyclerView recyclerFavorites;

    private FavoriCityAdapter favoriCityAdapter;
    private final List<WeatherEntity> favoriteList = new ArrayList<>();

    private MainViewModel viewModel;
    private PreferencesManager preferencesManager;
    private LocationGetter locationGetter;
    private AppDatabase citydb;

    private String currentCity = "";
    private String selectedCity;
    private String currentProvider = "OPEN_METEO";
    private boolean isNewSearch = false;
    private boolean isSaveLocation = false;
    private int currentBackground = R.drawable.background;
    private WeatherData currentWeatherData;

    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferencesManager = PreferencesManager.getInstance(this);
        currentProvider = preferencesManager.getSelectedProvider();
        locationGetter = new LocationGetter(this);

        initViews();
        setupViewModel();
        connectionDatabase();
        setupRecyclerView();
        setupAutoComplete();
        setupButtonListeners();
        setupNotificationSettings();

        AutoUpdateConfig.setupAutoUpdate(getApplicationContext());
        AutoUpdateConfig.setupAutoUpdateFavoriCities(getApplicationContext());

        viewModel.loadSavedWeather();
    }

    private void initViews() {
        editTextCity = findViewById(R.id.editTextCity);
        buttonSearch = findViewById(R.id.buttonSearch);
        buttonForecast = findViewById(R.id.buttonForecast);
        buttonHourlyForecast = findViewById(R.id.buttonHourlyForecast);
        buttonCurrentLocation = findViewById(R.id.buttonCurrentLocation);
        buttonClearAll = findViewById(R.id.buttonClearAll);
        seePastedData = findViewById(R.id.btnStations);
        saveFavorites = findViewById(R.id.buttonAddFavori);
        deleteFavories = findViewById(R.id.buttondeleteALlFavori);

        textViewCloudiness = findViewById(R.id.textViewCloudiness);
        textViewCityName = findViewById(R.id.textViewCityName);
        textViewTemperature = findViewById(R.id.textViewTemperature);
        textViewDescription = findViewById(R.id.textViewDescription);
        textViewHumidity = findViewById(R.id.textViewHumidity);
        textViewWindSpeed = findViewById(R.id.textViewWindSpeed);
        textViewPressure = findViewById(R.id.textViewPressure);
        textViewLastUpdate = findViewById(R.id.textViewLastUpdate);
        textViewAIAdvice = findViewById(R.id.TextViewAIAdvice);
        textViewOpenFavoriCities = findViewById(R.id.openFavoriCities);

        progressBar = findViewById(R.id.progressBar);
        windDirect = findViewById(R.id.imageViewWindTurbine);
        imageViewWeatherIcon = findViewById(R.id.imageViewWeatherIcon);
        drawerLayout = findViewById(R.id.drawerlayout);
        recyclerFavorites = findViewById(R.id.recyclerFavorites);
        root = findViewById(R.id.rootScroll);

        textViewOpenFavoriCities.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.START));
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        viewModel.getCurrentWeather().observe(this, data -> {
            if (data != null) {
                updateUI(data);
                if (isNewSearch) {
                    showSaveLocationDialog(data.cityName);
                }
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getAiAdvice().observe(this, advice -> {
            if (advice != null && !advice.isEmpty()) {
                textViewAIAdvice.setText(advice);
            }
        });

        viewModel.getFavorites().observe(this, newList -> {
            if (newList != null) {
                favoriteList.clear();
                favoriteList.addAll(newList);
                favoriCityAdapter.notifyDataSetChanged();
            }
        });
    }

    private void connectionDatabase() {
        citydb = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "weather_db.db")
                .fallbackToDestructiveMigration()
                .createFromAsset("weather.db").build();
    }

    private void setupRecyclerView() {
        favoriCityAdapter = new FavoriCityAdapter(favoriteList, new FavoriCityAdapter.OnCityAction() {
            @Override
            public void onClick(WeatherEntity city) {
                currentCity = city.cityName;
                isNewSearch = true;
                isSaveLocation = false;
                viewModel.loadWeather(currentCity, currentProvider, preferencesManager.getVisualCrossingModel());
                drawerLayout.closeDrawer(Gravity.START);
            }

            @Override
            public void onDelete(WeatherEntity city) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Şehri Sil")
                        .setMessage(city.cityName + " favorilerden silinsin mi?")
                        .setPositiveButton("Evet", (dialog, which) -> viewModel.deleteFavorite(city.cityName))
                        .setNegativeButton("Hayır", null)
                        .show();
            }
        });

        recyclerFavorites.setLayoutManager(new LinearLayoutManager(this));
        recyclerFavorites.setAdapter(favoriCityAdapter);
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

    private void setupButtonListeners() {
        buttonSearch.setOnClickListener(v -> {
            String city = editTextCity.getText().toString().trim();
            if (!city.isEmpty()) {
                currentCity = city;
                isNewSearch = true;
                viewModel.loadWeather(currentCity, currentProvider, preferencesManager.getVisualCrossingModel());
            } else {
                Toast.makeText(MainActivity.this, "Lütfen şehir adı girin", Toast.LENGTH_SHORT).show();
            }
        });

        buttonForecast.setOnClickListener(v -> {
            if (!currentCity.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, Forecastactivity.class);
                String cleanCity = currentCity.contains("/") ? currentCity.split("/")[0].trim() : currentCity;
                intent.putExtra("CITY_NAME", cleanCity);
                intent.putExtra("PROVIDER", currentProvider);
                intent.putExtra("VC_MODEL", preferencesManager.getVisualCrossingModel());
                intent.putExtra("background-res", currentBackground);
                intent.putExtra("isNewSearch", isNewSearch);
                intent.putExtra("isSaveLocation", isSaveLocation);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Önce bir şehir arayın", Toast.LENGTH_SHORT).show();
            }
        });

        buttonHourlyForecast.setOnClickListener(v -> {
            if (!currentCity.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, Hourlyforecastactivity.class);
                String cleanCity = currentCity.contains("/") ? currentCity.split("/")[0].trim() : currentCity;
                intent.putExtra("CITY_NAME", cleanCity);
                intent.putExtra("PROVIDER", currentProvider);
                intent.putExtra("VC_MODEL", preferencesManager.getVisualCrossingModel());
                intent.putExtra("background-res", currentBackground);
                intent.putExtra("isNewSearch", isNewSearch);
                intent.putExtra("isSaveLocation", isSaveLocation);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Önce bir şehir arayın", Toast.LENGTH_SHORT).show();
            }
        });

        buttonClearAll.setOnClickListener(v -> {
            viewModel.clearAllSavedData();
            currentCity = "";
            isSaveLocation = false;
            currentWeatherData = null;
            resetUI();
        });

        buttonCurrentLocation.setOnClickListener(v -> checkLocationPermission());

        saveFavorites.setOnClickListener(v -> {
            if (currentWeatherData == null) {
                Toast.makeText(MainActivity.this, "Önce bir şehir araması yapmalısın!", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.addToFavorites(
                    currentWeatherData,
                    () -> drawerLayout.openDrawer(Gravity.START),
                    () -> {
                        Toast.makeText(getApplicationContext(), "Bu şehir zaten kayıtlı", Toast.LENGTH_SHORT).show();
                        drawerLayout.openDrawer(Gravity.START);
                    }
            );
        });

        deleteFavories.setOnClickListener(v -> new AlertDialog.Builder(MainActivity.this)
                .setTitle("Hepsini Sil")
                .setMessage("Tüm favori şehirleriniz silinecek. Emin misiniz?")
                .setPositiveButton("Evet, Sil", (dialog, which) -> viewModel.clearAllFavorites())
                .setNegativeButton("Vazgeç", null)
                .show());

        seePastedData.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, pastDailyData.class);
            intent.putExtra("background-res", currentBackground);
            startActivity(intent);
        });
    }

    private void resetUI() {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.provider_owm) {
            selectProvider("OWM", "OpenWeatherMap");
            return true;
        } else if (id == R.id.provider_open_meteo) {
            selectProvider("OPEN_METEO", "Open-Meteo");
            return true;
        } else if (id == R.id.provider_weather_api) {
            selectProvider("WEATHER_API", "WeatherAPI");
            return true;
        } else if (id == R.id.provider_visual_crossing) {
            preferencesManager.saveVisualCrossingModel("");
            selectProvider("VISUAL_CROSSING", "Visual Crossing");
            return true;
        } else if (id == R.id.action_add_widget) {
            requestPinWidget();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void selectProvider(String providerKey, String displayName) {
        currentProvider = providerKey;
        preferencesManager.saveSelectedProvider(currentProvider);
        Toast.makeText(this, "Veri Kaynağı: " + displayName, Toast.LENGTH_SHORT).show();
        WeatherWidgetProvider.updateAllWidgets(this);
        if (!currentCity.isEmpty()) {
            isNewSearch = true;
            viewModel.loadWeather(currentCity, currentProvider, preferencesManager.getVisualCrossingModel());
        }
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

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            fetchCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocation();
            } else {
                Toast.makeText(this, "Konum izni reddedildi", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchCurrentLocation() {
        Toast.makeText(this, "Konum Alınıyor...", Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.VISIBLE);
        locationGetter.getCurrentLocation(new LocationGetter.LocationCallback() {
            @Override
            public void onLocationReceived(String cityName) {
                currentCity = cityName;
                isNewSearch = true;
                viewModel.loadWeather(currentCity, currentProvider, preferencesManager.getVisualCrossingModel());
                Toast.makeText(MainActivity.this, "Konum: " + cityName, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLocationError(String error) {
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(WeatherData data) {
        currentWeatherData = data;
        currentCity = data.cityName != null ? data.cityName : currentCity;

        buttonForecast.setEnabled(true);
        buttonHourlyForecast.setEnabled(true);
        buttonClearAll.setEnabled(true);

        textViewCloudiness.setText(getString(R.string.cloudiness_label, data.cloudiness));
        if (data.aiAdvice != null && !data.aiAdvice.isEmpty()) {
            textViewAIAdvice.setText(data.aiAdvice);
        } else {
            textViewAIAdvice.setText(R.string.ai_advice_waiting);
        }

        if (data.pressure == 0) {
            textViewPressure.setText(R.string.no_data);
        } else {
            textViewPressure.setText(getString(R.string.pressure_label, (double) data.pressure));
        }

        textViewCityName.setText(data.cityName != null ? data.cityName : "--");
        textViewTemperature.setText(getString(R.string.temperature_decimal_label, data.temp));

        String safeDesc = "--";
        if (data.description != null && !data.description.trim().isEmpty()) {
            safeDesc = (data.description.length() > 1)
                    ? data.description.substring(0, 1).toUpperCase(new Locale("tr", "TR")) + data.description.substring(1).toLowerCase(new Locale("tr", "TR"))
                    : data.description.toUpperCase(new Locale("tr", "TR"));
        }
        textViewDescription.setText(safeDesc);
        textViewHumidity.setText(getString(R.string.humidity_label, (double) data.humidity));
        textViewWindSpeed.setText(getString(R.string.wind_speed_label, data.windSpeed));
        windDirect.setRotation(data.windDirection + 180);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm", new Locale("tr", "TR"));
        String updateTime = sdf.format(new Date(data.timestamp > 0 ? data.timestamp : System.currentTimeMillis()));

        String providerName = getProviderDisplayName(currentProvider);
        textViewLastUpdate.setText(getString(R.string.last_update_label, updateTime, providerName));

        if (data.icon != null) {
            int iconResource = UIUpdate.setWeatherIcon(data.icon, data.description);
            imageViewWeatherIcon.setImageResource(iconResource);
            updateBackgroundByWeather(data.icon, data.description);
        }

        WeatherWidgetProvider.updateAllWidgets(this);
        WeatherWidgetProvider.fetchHourlyForecastForWidget(this, preferencesManager, currentCity);
    }

    private void updateBackgroundByWeather(String iconCode, String description) {
        int backgroundRes = UIUpdate.updateBackgroundByWeather(iconCode, description);
        currentBackground = backgroundRes;
        root.setBackgroundResource(currentBackground);
        recyclerFavorites.setBackgroundResource(R.drawable.general_component_background);
    }

    private void showSaveLocationDialog(String cityName) {
        new AlertDialog.Builder(this)
                .setTitle("Konum Kaydet")
                .setMessage(cityName + " konumunu varsayılan olarak kaydetmek ister misiniz?\n\n" +
                        "Kaydederseniz:\n" +
                        "• Uygulama açıldığında otomatik yüklenecek\n" +
                        "• Günde 2 kere otomatik güncellenecek")
                .setPositiveButton("Evet, Kaydet", (dialog, which) -> {
                    viewModel.saveDefaultLocation(cityName, currentWeatherData, currentProvider);
                    isSaveLocation = true;
                    Toast.makeText(MainActivity.this,
                            cityName + " (" + getProviderDisplayName(currentProvider) + ") varsayılan konum olarak kaydedildi",
                            Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Hayır", (dialog, which) -> {
                    preferencesManager.saveSelectedProvider(currentProvider);
                    Toast.makeText(MainActivity.this, "Konum kaydedilmedi.", Toast.LENGTH_SHORT).show();
                    isSaveLocation = false;
                })
                .setCancelable(false)
                .show();
    }

    private String getProviderDisplayName(String providerKey) {
        if ("OWM".equals(providerKey)) return "OpenWeatherMap";
        if ("WEATHER_API".equals(providerKey)) return "WeatherAPI";
        if ("VISUAL_CROSSING".equals(providerKey)) return "Visual Crossing";
        return "Open-Meteo";
    }

    private void setupNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher = registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            Log.d(TAG, "Bildirim izni verildi");
                        } else {
                            Log.d(TAG, "Bildirim izni reddedildi");
                        }
                    }
            );

            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}