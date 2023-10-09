package com.example.weatherappviews.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.example.weatherappviews.R;
import com.example.weatherappviews.model.WeatherInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SharedPreferenceUtil {

    private SharedPreferenceUtil() {}

    public static final String KEY_FOREGROUND_ENABLED = "tracking_foreground_location";
    public static final String LAST_WEATHER_RESPONSE = "last_weather_response";

    private static final String TAG = "SharedPreferenceUtil";
    private static final Gson GSON = new GsonBuilder().create();

    public static Boolean getLocationTrackingPref(Context context) {
        return context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
                .getBoolean(KEY_FOREGROUND_ENABLED, false);
    }

    @SuppressLint("CommitPrefEdits")
    public static void saveLocationTrackingPref(Context context, boolean requestingLocationUpdates) {
        context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE
        ).edit().putBoolean(KEY_FOREGROUND_ENABLED, requestingLocationUpdates).apply();
    }

    public static WeatherInfo getLastWeatherInfo(Context context) {
        String response = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
                .getString(LAST_WEATHER_RESPONSE, "");
        Log.d(TAG, "getLastWeatherInfo response: "+ response);
        return GSON.fromJson(response, WeatherInfo.class);
    }

    @SuppressLint("CommitPrefEdits")
    public static void saveLastWeatherInfo(Context context, WeatherInfo weatherInfo) {
        String response = GSON.toJson(weatherInfo);
        Log.d(TAG, "saveLastWeatherInfo response: "+ response);
        context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE
        ).edit().putString(LAST_WEATHER_RESPONSE, response).apply();
    }
}
