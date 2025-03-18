package com.example.noteapp.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class UserSession {
    public String getUserSession(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        return sharedPreferences.getString("userId", null);
    }
    public void saveUserSession(Context context, String userId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userId", userId);
        editor.apply();
    }
}
