package com.example.noteapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.noteapp.interfacePackage.OnEmailExists;
import com.example.noteapp.model.User;
import com.example.noteapp.myDatabase.FirebaseSyncHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
//import com.example.noteapp.myDatabase.FirebaseSyncHelper;

public class Register extends AppCompatActivity implements View.OnClickListener {
    private TextView btBack;
    private Button btRegister;
    private EditText edEmail, edName, edPassword, edConfirmPassword;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initView();

        btBack.setOnClickListener(this);
        btRegister.setOnClickListener(this);

    }
    private void initView(){
        btBack =(TextView) findViewById(R.id.btBack);
        btRegister=(Button) findViewById(R.id.btRegister);
        edEmail =(EditText) findViewById(R.id.edEmail);
        edName =(EditText) findViewById(R.id.edName);
        edPassword = (EditText)findViewById(R.id.edPassword);
        edConfirmPassword=(EditText) findViewById(R.id.edConfirmPassword);
    }
    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btBack){
            onLogin();
        }else if(v.getId() == R.id.btRegister){
            onRegister();
        }
    }

    private void onRegister() {
        String email = edEmail.getText().toString().trim();
        String name = edName.getText().toString().trim();
        String password = edPassword.getText().toString().trim();
        String confirmPassword = edConfirmPassword.getText().toString().trim();

        // Kiểm tra mật khẩu trùng khớp
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Password does not match Confirm Password", Toast.LENGTH_SHORT).show();
            return;
        }
        // Kiểm tra email
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email is not valid", Toast.LENGTH_SHORT).show();
            return;
        }
        // Kiểm tra tên
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "First name is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        // Kiểm tra mật khẩu
        if (TextUtils.isEmpty(password) || password.length() < 8) {
            Toast.makeText(this, "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                String userId = user.getUid(); // Lấy UID từ Firebase Auth
                                DatabaseReference db_users = FirebaseDatabase.getInstance().getReference("users");

                                User newUser = new User(userId, email, name, "");

                                // Lưu thông tin vào Firebase Realtime Database
                                db_users.child(userId).setValue(newUser)
                                        .addOnCompleteListener(subtask -> {
                                            Intent intent = new Intent(getBaseContext(), Login.class);
                                            startActivity(intent);
                                            finish();
                                        });
                            }
                        } else {
                            if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                                Toast.makeText(getBaseContext(), "Email này đã được đăng ký!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getBaseContext(), "Lỗi đăng ký!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }
    private void onLogin() {
        Intent intent= new Intent(this, Login.class);
        startActivity(intent);
        finish();
    }
}
