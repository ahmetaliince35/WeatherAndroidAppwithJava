package com.example.weatherapp.Helpers.charts;

public class ChartDataPoint {
    public final String label;
    public final float value;
    public final String displayValue;

    public ChartDataPoint(String label, float value, String displayValue) {
        this.label = (label != null) ? label : "--";
        this.value = (Float.isNaN(value) || Float.isInfinite(value)) ? 0.0f : value;
        this.displayValue = (displayValue != null) ? displayValue : "--";
    }
}

