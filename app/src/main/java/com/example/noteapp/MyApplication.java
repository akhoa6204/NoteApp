package com.example.noteapp;

import android.app.Application;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.noteapp.settings.AppSettings;

import java.util.Locale;

public class MyApplication extends Application {
    private AppSettings appSettings;
    @Override
    public void onCreate() {
        super.onCreate();
        appSettings = new AppSettings();
        appSettings.applyLanguage(this);
        appSettings.applyTheme(this);
    }

}
