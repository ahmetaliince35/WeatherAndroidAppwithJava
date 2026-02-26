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

public class LocationGetter {

    private static final String TAG = "LocationGetter";
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
        Log.d(TAG, "LocationGetter initialized");
    }

    public void getCurrentLocation(LocationCallback callback) {
        this.callback = callback;
        Log.d(TAG, "getCurrentLocation called");

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            callback.onLocationError("Konum izni verilmedi");
            return;
        }

        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Log.d(TAG, "GPS enabled: " + isGPSEnabled + ", Network enabled: " + isNetworkEnabled);

        if (!isGPSEnabled && !isNetworkEnabled) {
            callback.onLocationError("GPS veya ağ konumu kapalı");
            return;
        }

        Location lastKnownLocation = getLastKnownLocation();
        if (lastKnownLocation != null) {
            Log.d(TAG, "Last known location found");
            getCityFromLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            return;
        }

        Log.d(TAG, "No last known location, requesting new location");
        requestNewLocation();
    }

    @RequiresPermission(anyOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private Location getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        Log.d(TAG, "Last known GPS location: " + gpsLocation + ", Network location: " + networkLocation);

        if (gpsLocation != null && networkLocation != null) {
            return gpsLocation.getTime() > networkLocation.getTime() ? gpsLocation : networkLocation;
        }

        return gpsLocation != null ? gpsLocation : networkLocation;
    }

    private void requestNewLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission not granted for requestNewLocation");
            return;
        }

        Log.d(TAG, "requestNewLocation called");

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, "New location received: " + location);
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

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0,
                    locationListener
            );
            Log.d(TAG, "Requesting GPS location updates");
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    0,
                    0,
                    locationListener
            );
            Log.d(TAG, "Requesting Network location updates");
        }
    }

    private void getCityFromLocation(double latitude, double longitude) {
        Log.d(TAG, "getCityFromLocation: lat=" + latitude + ", lon=" + longitude);
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String cityName = address.getLocality();
                if (cityName == null || cityName.isEmpty()) {
                    cityName = address.getAdminArea();
                }

                if (cityName != null && !cityName.isEmpty()) {
                    Log.d(TAG, "City found: " + cityName);
                    callback.onLocationReceived(cityName);
                } else {
                    Log.e(TAG, "City name not found in address");
                    callback.onLocationError("Şehir adı bulunamadı");
                }
            } else {
                Log.e(TAG, "No addresses found");
                callback.onLocationError("Adres bulunamadı");
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder IOException: " + e.getMessage());
            callback.onLocationError("Konum çevirme hatası: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception in getCityFromLocation: " + e.getMessage());
            callback.onLocationError("Konum hatası: " + e.getMessage());
        }
    }

}