package com.example.noteapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class VerifyCode extends AppCompatActivity {
    private TextView btBack;
    private Button btnAccept;
    private EditText etCode1, etCode2, etCode3, etCode4, etCode5, etCode6;
    private String otp, email;
    @Override
    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_verify_code);
        initView();
        setupOtpInputs();
        otp = getIntent().getStringExtra("otp");
        email = getIntent().getStringExtra("email");
        if (otp == null){
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
        }
    }
    private void initView(){
        btBack =(TextView) findViewById(R.id.btBack);
        btnAccept=(Button) findViewById(R.id.btnAccept);
        etCode1=(EditText) findViewById(R.id.etCode1);
        etCode2=(EditText) findViewById(R.id.etCode2);
        etCode3=(EditText) findViewById(R.id.etCode3);
        etCode4=(EditText) findViewById(R.id.etCode4);
        etCode5=(EditText) findViewById(R.id.etCode5);
        etCode6=(EditText) findViewById(R.id.etCode6);

        btnAccept.setOnClickListener(v -> {
            String otp_actual = etCode1.getText().toString() + etCode2.getText().toString() + etCode3.getText().toString() + etCode4.getText().toString() + etCode5.getText().toString() + etCode6.getText().toString();
            if(otp_actual.equals(otp)){
                sendEmailSetPassword(email);
            }
        });
        btBack.setOnClickListener(v -> {
            Intent intent = new Intent(this, ForgotPassword.class);
            startActivity(intent);
            finish();
        });
    }
    private void sendEmailSetPassword(String email){
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Đã gửi email đặt lại mật khẩu", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, Login.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
    }
    private void setupOtpInputs() {
        EditText[] otpInputs = {etCode1, etCode2, etCode3, etCode4, etCode5, etCode6};

        for (int i = 0; i < otpInputs.length; i++) {
            int index = i;

            otpInputs[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && index < otpInputs.length - 1) {
                        otpInputs[index + 1].requestFocus();
                    }
                }
            });

            otpInputs[i].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        keyCode == KeyEvent.KEYCODE_DEL &&
                        otpInputs[index].getText().toString().isEmpty() &&
                        index > 0) {
                    otpInputs[index - 1].requestFocus();
                    return true;
                }
                return false;
            });
        }
    }
}
