package com.example.noteapp;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.noteapp.interfacePackage.OnDataSyncListener;
import com.example.noteapp.model.NoteModel;
import com.example.noteapp.model.User;
import com.example.noteapp.myDatabase.FirebaseSyncHelper;
import com.example.noteapp.settings.AppSettings;
import com.example.noteapp.settings.UserSession;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;
import java.util.Locale;
import de.hdodenhof.circleimageview.CircleImageView;

public class Account extends AppCompatActivity implements View.OnClickListener {
    private static final int PICK_IMAGE_REQUEST = 1;

    private TextView btnLogOut, btnBack, tvName, tvEmail, tvName2, btnEdit;
    private TextView tvLanguage, tvTheme, tvAccount, tvChangePassword, tvPassword, tvLanguageLogo, tvThemeLogo;
    private LinearLayout btnLanguageWrap, btnLanguage, lrAccount, lrPassword;
    private CircleImageView ivImgUser;

    private AppSettings appSettings;
    private UserSession userSession;
    private FirebaseSyncHelper syncHelper;

    private int currentTheme, currentLanguage;
    private String userId;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        initViews();

        userId = userSession.getUserSession(this);
        syncHelper.getUser(userId, new OnDataSyncListener() {
            @Override public void onUserLoaded(User user) {
                if (user != null) {
                    tvName.setText(user.getName());
                    tvName2.setText(user.getName());
                    tvEmail.setText(user.getEmail());
                }
            }
            @Override public void onNotesUpdated(List<NoteModel> updatesNotes) {}
            @Override public void onNoteLoaded(NoteModel note) {}
            @Override public void onSharedNoteLoaded(List<User> sharedUserList) {}
        });

        currentTheme = appSettings.getTheme(this);
        currentLanguage = appSettings.getLanguage(this);
        applyToggleState(currentLanguage, btnLanguageWrap, btnLanguage, false);

        updateTexts();
    }

    private void initViews() {
        btnLogOut = findViewById(R.id.btnLogOut);
        btnBack = findViewById(R.id.btnBack);
        tvEmail = findViewById(R.id.tvEmail);
        tvName = findViewById(R.id.tvName);
        tvName2 = findViewById(R.id.tvName2);
        btnLanguageWrap = findViewById(R.id.btnLanguageWrap);
        btnLanguage = findViewById(R.id.btnLanguage);
        btnEdit = findViewById(R.id.btnEdit);
        lrAccount = findViewById(R.id.lrAccount);
        lrPassword = findViewById(R.id.lrPassword);
        ivImgUser = findViewById(R.id.ivImgUser);

        tvLanguage = findViewById(R.id.tvLanguage);
        tvTheme = findViewById(R.id.tvTheme);
        tvAccount = findViewById(R.id.tvAccount);
        tvChangePassword = findViewById(R.id.tvChangePassword);
        tvPassword = findViewById(R.id.tvPassword);
        tvLanguageLogo = findViewById(R.id.tvLanguageLogo);
        tvThemeLogo = findViewById(R.id.tvThemeLogo);

        userSession = new UserSession();
        appSettings = new AppSettings();
        syncHelper = new FirebaseSyncHelper(this);

        btnLogOut.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        btnLanguageWrap.setOnClickListener(this);
        lrAccount.setOnClickListener(this);
        btnEdit.setOnClickListener(this);

        lrPassword.setOnClickListener(v -> startActivity(new Intent(this, ChangePassword.class)));
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void applyToggleState(int value, LinearLayout wrapper, LinearLayout toggle, boolean isTheme) {
        wrapper.setBackgroundResource(value == 1 ? R.drawable.border_toggle : R.drawable.border_toggle_gray);
        toggle.setTranslationX(dpToPx(value == 1 ? 0 : 20));
        if (isTheme) appSettings.saveTheme(this, value);
        else appSettings.saveLanguage(this, value);
    }

    private void toggleSetting(boolean isTheme) {
        int current = isTheme ? appSettings.getTheme(this) : appSettings.getLanguage(this);
        int newValue = (current == 1) ? -1 : 1;
        if (isTheme) {
        } else {
            applyToggleState(newValue, btnLanguageWrap, btnLanguage, false);
            changeLanguage(newValue);
        }
    }
    private void changeLanguage(int languageValue) {
        appSettings.saveLanguage(this, languageValue);
        appSettings.applyLanguage(this);
        currentLanguage = languageValue;
        updateTexts();
    }

    private void updateTexts() {
        btnLogOut.setText(R.string.logout);
        tvTheme.setText(currentTheme == 1 ? R.string.light_theme : R.string.dark_theme);
        tvLanguage.setText(currentLanguage == 1 ? R.string.language_vn : R.string.language_en);
        tvLanguageLogo.setText(R.string.language);
        tvThemeLogo.setText(R.string.theme);
        tvAccount.setText(R.string.account);
        tvChangePassword.setText(R.string.change_password);
        tvPassword.setText(R.string.password);
    }

    private void logOut() {
        userSession.saveUserSession(this, null);
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            ivImgUser.setImageURI(imageUri);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnLogOut) logOut();
        else if (id == R.id.btnBack) goToMain();
        else if (id == R.id.btnLanguageWrap) toggleSetting(false);
        else if (id == R.id.lrAccount) {}
        else if (id == R.id.btnEdit) openImagePicker();
    }
}
