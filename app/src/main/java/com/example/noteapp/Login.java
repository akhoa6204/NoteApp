package com.example.noteapp;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.noteapp.settings.UserSession;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;


public class Login extends AppCompatActivity implements View.OnClickListener {
    private TextView btRegister;
    private Button btLogin;
    private EditText edEmail, edPassword;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edEmail=(EditText) findViewById(R.id.edEmail);
        edPassword=(EditText)findViewById(R.id.edPassword);

        btRegister=(TextView) findViewById(R.id.btRegister);
        btLogin=(Button) findViewById(R.id.btLogin);

        btLogin.setOnClickListener(this);
        btRegister.setOnClickListener(this);

    }
    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btRegister){
            onRegister();
        }else if(v.getId() == R.id.btLogin){
            onLogin();
        }
    }

    private void onLogin() {
        String email = edEmail.getText().toString().trim();
        String password =edPassword.getText().toString().trim();

        if(email.isEmpty()){
            Toast.makeText(this, "Email is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(this, "Email is not valid", Toast.LENGTH_SHORT).show();
            return;
        }
        if(password.length() < 8){
            Toast.makeText(this, "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            UserSession userSession = new UserSession();
                            userSession.saveUserSession(getBaseContext(), user.getUid());
                            Log.d("DEBUG_Login", "user: " + user);
                            Log.d("DEBUG_Login", "userId: " + user.getUid());

                            Toast.makeText(getApplicationContext(), "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(getBaseContext(), MainActivity.class);
                            intent.putExtra("email", user.getEmail());
                            startActivity(intent);
                            finish();
                        } else {
                            Exception exception = task.getException();
                            if (exception instanceof FirebaseAuthInvalidUserException) {
                                Toast.makeText(getApplicationContext(), "Email chưa được đăng ký!", Toast.LENGTH_SHORT).show();
                            } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(getApplicationContext(), "Sai mật khẩu!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void onRegister() {
        Intent intent = new Intent(this, Register.class);
        startActivity(intent);
        finish();
    }

}
