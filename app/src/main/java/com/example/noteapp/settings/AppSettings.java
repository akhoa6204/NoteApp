package com.example.noteapp.settings;

import android.content.Context;
import android.content.SharedPreferences;

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
    public void saveNoti(Context context, int noti) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("Notification", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("noti", noti);
        editor.apply();
    }
    public int getNoti(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("Notification", Context.MODE_PRIVATE);
        return sharedPreferences.getInt("noti", 1);
    }
}
