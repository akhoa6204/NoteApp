package com.example.noteapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.noteapp.interfacePackage.OnDataSyncListener;
import com.example.noteapp.model.NoteModel;
import com.example.noteapp.model.User;
import com.example.noteapp.myDatabase.FirebaseSyncHelper;
import com.example.noteapp.settings.UserSession;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class ChangePassword extends AppCompatActivity {
    private TextView btnBack, tvNameUser, btnFinish;
    private EditText edCurrentPassword, edNewPassword, edConfirmNewPassword;
    private Button btnFinish2;
    private User currentUser;
    @Override
    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_change_password);
        init();
        UserSession userSession = new UserSession();
        String userId = userSession.getUserSession(this);
        FirebaseSyncHelper firebaseSyncHelper = new FirebaseSyncHelper(this);
        firebaseSyncHelper.getUser(userId, new OnDataSyncListener() {
            @Override
            public void onNotesUpdated(List<NoteModel> updatesNotes) {

            }

            @Override
            public void onUserLoaded(User user) {
                tvNameUser.setText(user.getName());
                currentUser = user;
            }

            @Override
            public void onNoteLoaded(NoteModel note) {

            }

            @Override
            public void onSharedNoteLoaded(List<User> sharedUserList) {

            }
        });
        btnBack.setOnClickListener(v -> {
            finish();
        });
        btnFinish.setOnClickListener(v -> {
            boolean result = checkLogicNullChangePassword();
            if (result){
                if (currentUser != null){
                    changePassword(currentUser);
                }else{
                    firebaseSyncHelper.getUser(userId, new OnDataSyncListener() {
                        @Override
                        public void onNotesUpdated(List<NoteModel> updatesNotes) {

                        }

                        @Override
                        public void onUserLoaded(User user) {
                            currentUser = user;
                            changePassword(currentUser);
                        }

                        @Override
                        public void onNoteLoaded(NoteModel note) {

                        }

                        @Override
                        public void onSharedNoteLoaded(List<User> sharedUserList) {

                        }
                    });
                }

            }
        });
        btnFinish2.setOnClickListener(v -> {
            boolean result = checkLogicNullChangePassword();
            if (result){
                if (currentUser != null){
                    changePassword(currentUser);
                }else{
                    firebaseSyncHelper.getUser(userId, new OnDataSyncListener() {
                        @Override
                        public void onNotesUpdated(List<NoteModel> updatesNotes) {

                        }

                        @Override
                        public void onUserLoaded(User user) {
                            currentUser = user;
                            changePassword(currentUser);
                        }

                        @Override
                        public void onNoteLoaded(NoteModel note) {

                        }

                        @Override
                        public void onSharedNoteLoaded(List<User> sharedUserList) {

                        }
                    });
                }

            }
        });

    }
    public void init(){
        btnBack = (TextView) findViewById(R.id.btnBack);
        tvNameUser = (TextView) findViewById(R.id.tvNameUser);
        edCurrentPassword = (EditText) findViewById(R.id.current_password);
        edNewPassword = (EditText) findViewById(R.id.new_password);
        edConfirmNewPassword = (EditText) findViewById(R.id.confirm_new_password);
        btnFinish =(TextView) findViewById(R.id.btnFinish);
        btnFinish2 =(Button) findViewById(R.id.btnFinish2);

    }
    public boolean checkLogicNullChangePassword(){
        String currentPassword = String.valueOf(edCurrentPassword.getText());
        String newPassword = String.valueOf(edNewPassword.getText());
        String confirmNewPassword = String.valueOf(edConfirmNewPassword.getText());
        if (TextUtils.isEmpty(currentPassword) ||
                TextUtils.isEmpty(newPassword) ||
                TextUtils.isEmpty(confirmNewPassword)) {
            Toast.makeText(this, "Các trường không được để trống", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!newPassword.equals(confirmNewPassword)) {
            Toast.makeText(this, "Mật khẩu nhập lại không khớp với mật khẩu mới", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    public void changePassword(User currentUser){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            String currentPassword = String.valueOf(edCurrentPassword.getText()).trim();
            String newPassword = String.valueOf(edNewPassword.getText()).trim();
            AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Lỗi khi đổi mật khẩu", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(this, "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
