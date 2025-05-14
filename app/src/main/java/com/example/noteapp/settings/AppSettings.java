package com.example.noteapp.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

public class AppSettings {
    public void saveTheme(Context context, int theme) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("Theme", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("theme", theme);
        editor.apply();
    }
    public int getTheme(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("Theme", Context.MODE_PRIVATE);
        return sharedPreferences.getInt("theme", 1);
    }
    public void saveLanguage(Context context, int language) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("Language", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("language", language);
        editor.apply();
    }
    public int getLanguage(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("Language", Context.MODE_PRIVATE);
        return sharedPreferences.getInt("language", 1);
    }
    public void applyLanguage(Context context) {
        int langValue = getLanguage(context);
        String langCode = (langValue == 1) ? "vi" : "en";
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }
    public void applyTheme(Context context) {
        int themeValue = getTheme(context);
        if (themeValue == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

    }
}
