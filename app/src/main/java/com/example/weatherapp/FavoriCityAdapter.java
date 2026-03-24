package com.example.weatherapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherapp.data.WeatherEntity;

import java.util.List;

public class FavoriCityAdapter  extends RecyclerView.Adapter<FavoriCityAdapter.ViewHolder> {
        List<WeatherEntity> list;
        OnCityAction listener;

        public interface OnCityAction {
            void onClick(WeatherEntity city);
            void onDelete(WeatherEntity city);
        }

        public FavoriCityAdapter(List<WeatherEntity> list, OnCityAction listener){
            this.list = list;
            this.listener = listener;
        }

        public class ViewHolder extends RecyclerView.ViewHolder{
            TextView cityName;
            ImageView iconimage;
            TextView temp;
            TextView deletecity;

            public ViewHolder(View itemView){
                super(itemView);
                cityName = itemView.findViewById(R.id.txtCity);
                iconimage=itemView.findViewById(R.id.imgFavIcon);
                temp=itemView.findViewById(R.id.txtFavTemp);
                deletecity=itemView.findViewById(R.id.deletecity);
            }
        }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.favorites_cities, parent, false);
        return new ViewHolder(view);
    }
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            WeatherEntity city = list.get(position);
            holder.cityName.setText(city.cityName);
            holder.temp.setText(String.format("%.0f °C",city.temperature));
            int iconRes = com.example.weatherapp.Helpers.UIUpdate.setWeatherIcon(city.icon, city.description);
            holder.iconimage.setImageResource(iconRes);
            int backgroundRes = com.example.weatherapp.Helpers.UIUpdate.updateBackgroundByWeather(city.icon, city.description);
            holder.itemView.setBackgroundResource(backgroundRes);
            holder.itemView.setOnClickListener(v -> listener.onClick(city));
            holder.deletecity.setOnClickListener(v -> listener.onDelete(city));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

