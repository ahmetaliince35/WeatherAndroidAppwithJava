package com.example.weatherapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Konum işlemleri için yardımcı sınıf
 * GPS ile mevcut konumu alır ve şehir adını bulur
 */
public class LocationGetter {

    private static final String TAG = "LocationHelper";
    private Context context;
    private LocationManager locationManager;
    private LocationCallback callback;

    public interface LocationCallback {
        void onLocationReceived(String cityName);
        void onLocationError(String error);
    }

    public LocationGetter(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * Mevcut konumu alır
     */
    public void getCurrentLocation(LocationCallback callback) {
        this.callback = callback;

        // İzin kontrolü
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback.onLocationError("Konum izni verilmedi");
            return;
        }

        // GPS aktif mi kontrol et
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGPSEnabled && !isNetworkEnabled) {
            callback.onLocationError("GPS veya ağ konumu kapalı");
            return;
        }

        // Önce son bilinen konumu dene
        Location lastKnownLocation = getLastKnownLocation();
        if (lastKnownLocation != null) {
            getCityFromLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            return;
        }

        // Son bilinen konum yoksa, yeni konum al
        requestNewLocation();
    }

    /**
     * Son bilinen konumu al
     */
    @RequiresPermission(anyOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private Location getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (gpsLocation != null && networkLocation != null) {
            return gpsLocation.getTime() > networkLocation.getTime() ? gpsLocation : networkLocation;
        }

        return gpsLocation != null ? gpsLocation : networkLocation;
    }

    /**
     * Yeni konum talebi
     */
    private void requestNewLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // Konum alındı
                getCityFromLocation(location.getLatitude(), location.getLongitude());
                locationManager.removeUpdates(this);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        // GPS'ten konum al
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0,
                    locationListener
            );
        }
        // Network'ten konum al
        else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    0,
                    0,
                    locationListener
            );
        }
    }

    /**
     * Koordinatlardan şehir adını bul
     */
    private void getCityFromLocation(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String cityName = address.getLocality(); // Şehir adı

                if (cityName == null || cityName.isEmpty()) {
                    cityName = address.getAdminArea(); // İl adı
                }

                if (cityName == null || cityName.isEmpty()) {
                    cityName = address.getCountryName(); // Ülke adı
                }

                if (cityName != null && !cityName.isEmpty()) {

                    callback.onLocationReceived(cityName);
                } else {
                    callback.onLocationError("Şehir adı bulunamadı");
                }
            } else {
                callback.onLocationError("Adres bulunamadı");
            }
        } catch (IOException e) {
            callback.onLocationError("Konum çevirme hatası: " + e.getMessage());
        } catch (Exception e) {
            callback.onLocationError("Konum hatası: " + e.getMessage());
        }
    }

}
