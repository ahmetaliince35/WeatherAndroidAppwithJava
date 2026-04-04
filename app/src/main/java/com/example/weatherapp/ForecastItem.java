package com.example.weatherapp;

public class ForecastItem {
    private String date;
    private double temperature;

    private String description;
    private String icon;
    private double humidity;
    private double  probability;
    private double preprainy;
    private double windSpeed;
     private double tempMin;
     private double tempMax;
    public ForecastItem(String date, double temperature,
                        String description, String icon, double humidity, double probability,double preprainy,double windSpeed,double tempMin,double tempMax) {
        this.date = date;
        this.temperature = temperature;
        this.description = description;
        this.icon = icon;
        this.humidity = humidity;
        this.probability=probability;
        this.preprainy=preprainy;
        this.windSpeed = windSpeed;
        this.tempMin=tempMin;
        this.tempMax=tempMax;
    }

    public String getDate() {
        return date;
    }

    public double getTemperature() {
        return temperature;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public double getHumidity() {
        return humidity;
    }
    public double getProbability(){ return probability;}
    public double getPreprainy(){ return preprainy;}
    public double getWindSpeed() {return windSpeed;}
    public double getTempMin() {return tempMin;}
    public double getTempMax() {return tempMax;}
}
