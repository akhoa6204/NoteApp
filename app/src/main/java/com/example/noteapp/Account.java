package com.example.noteapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.noteapp.interfacePackage.OnDataSyncListener;
import com.example.noteapp.model.NoteModel;
import com.example.noteapp.model.User;
import com.example.noteapp.myDatabase.Database;
import com.example.noteapp.myDatabase.FirebaseSyncHelper;
import com.example.noteapp.settings.AppSettings;
import com.example.noteapp.settings.UserSession;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class Account extends AppCompatActivity implements View.OnClickListener{
    private TextView btnLogOut, btnBack, tvName, tvEmail, tvName2;
    private LinearLayout btnTheme, btnThemeWrap, btnLanguageWrap, 
            btnLanguage, btnNotiWrap, btnNoti, lrAccount;
    private AppSettings appSettings;
    private UserSession userSession;
    private int  currentTheme, currentLanguage, currentNoti;
    private String userId;
    private User user;
    private FirebaseSyncHelper syncHelper;
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        init();

        userId = userSession.getUserSession(this);

        syncHelper.getUser(userId, new OnDataSyncListener() {
            @Override
            public void onNotesUpdated(List<NoteModel> updatesNotes) {

            }

            @Override
            public void onUserLoaded(User userReturn) {
                user = userReturn;
            }

            @Override
            public void onNoteLoaded(NoteModel note) {

            }

            @Override
            public void onSharedNoteLoaded(List<User> sharedUserList) {

            }
        });
        if(user != null){
            tvName.setText(user.getFirstName() + " " + user.getLastName());
            tvName2.setText(user.getFirstName() + " " + user.getLastName());
            tvEmail.setText(user.getEmail());
        }

        currentTheme = appSettings.getTheme(this);
        UpdateTheme(appSettings, currentTheme, btnThemeWrap, btnTheme, 1);

        currentLanguage = appSettings.getLanguage(this);
        UpdateTheme(appSettings, currentLanguage, btnLanguageWrap, btnLanguage, 2);

        currentNoti = appSettings.getNoti(this);
        UpdateTheme(appSettings, currentNoti, btnNotiWrap, btnNoti, 0);

        btnLogOut.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        btnThemeWrap.setOnClickListener(this);
        btnLanguageWrap.setOnClickListener(this);
        btnNotiWrap.setOnClickListener(this);
        lrAccount.setOnClickListener(this);
    }
    public void init(){
        btnLogOut =(TextView) findViewById(R.id.btnLogOut);
        btnBack =(TextView) findViewById(R.id.btnBack);
        tvEmail =(TextView) findViewById(R.id.tvEmail);
        tvName =(TextView) findViewById(R.id.tvName);
        tvName2 =(TextView) findViewById(R.id.tvName2);
        btnTheme =(LinearLayout) findViewById(R.id.btnTheme);
        btnThemeWrap =(LinearLayout) findViewById(R.id.btnThemeWrap);
        btnLanguageWrap=(LinearLayout) findViewById(R.id.btnLanguageWrap);
        btnLanguage=(LinearLayout) findViewById(R.id.btnLanguage);
        btnNotiWrap=(LinearLayout) findViewById(R.id.btnNotiWrap);
        btnNoti=(LinearLayout) findViewById(R.id.btnNoti);
        lrAccount =(LinearLayout) findViewById(R.id.lrAccount);
        userSession = new UserSession();
        syncHelper = new FirebaseSyncHelper(this);
        appSettings = new AppSettings();


    }
    public float changeValue(float value){
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
    public void UpdateTheme(AppSettings appSettings, int theme, LinearLayout btnThemeWrap, LinearLayout btnTheme, int type){
        if(theme == 1){
            btnThemeWrap.setBackgroundResource(R.drawable.border_toggle);
            float dp = changeValue(0);
            btnTheme.setTranslationX(dp);
            if(type == 1){
                appSettings.saveTheme(this, 1);
            }
            else if(type == 2){
                appSettings.saveLanguage(this, 1);
            }
            else {
                appSettings.saveNoti(this, 1);
            }
        }else {
            btnThemeWrap.setBackgroundResource(R.drawable.border_toggle_gray);
            float dp = changeValue(20);
            btnTheme.setTranslationX(dp);
            if(type == 1){
                appSettings.saveTheme(this, -1);
            }
            else if(type == 2){
                appSettings.saveLanguage(this, -1);
            }
            else {
                appSettings.saveNoti(this, -1);
            }
        }

    }
    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btnLogOut){
            onLogOut();
        } else if (v.getId() == R.id.btnBack) {
            onMain();
        } else if (v.getId() == R.id.btnThemeWrap) {
            onChangeTheme(appSettings, btnThemeWrap, btnTheme, 1);
        }else if (v.getId() == R.id.btnLanguageWrap) {
            onChangeTheme(appSettings, btnLanguageWrap, btnLanguage, 2);
        }
        else if (v.getId() == R.id.btnNotiWrap) {
            onChangeTheme(appSettings, btnNotiWrap, btnNoti, 0);
        }else if (v.getId() == R.id.lrAccount){
            onChangeAccount();
        }
    }

    private void onChangeAccount() {

    }

    private void onChangeTheme(AppSettings appSettings, LinearLayout btnThemeWrap, LinearLayout btnTheme, int type) {
        int current;
        if(type == 1){
            current = appSettings.getTheme(this);
        } else if(type == 2){
            current = appSettings.getLanguage(this);
        } else {
            current = appSettings.getNoti(this);
        }

        int newValue = (current == 1) ? -1 : 1;
        UpdateTheme(appSettings, newValue, btnThemeWrap, btnTheme, type);
    }


    private void onMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void onLogOut(){
        userSession.saveUserSession(this, null);

        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
