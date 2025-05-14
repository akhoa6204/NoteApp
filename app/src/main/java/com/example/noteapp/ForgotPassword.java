package com.example.noteapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.noteapp.interfacePackage.OnEmailExists;
import com.example.noteapp.myDatabase.FirebaseSyncHelper;


import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Message;

public class ForgotPassword extends AppCompatActivity {
    private TextView btBack;
    private EditText edEmail;
    private Button btnAccept;
    @Override
    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_forgot_password);

        edEmail = (EditText) findViewById(R.id.edEmail);
        btnAccept =(Button) findViewById(R.id.btnAccept);
        btBack=(TextView) findViewById(R.id.btBack);
        btBack.setOnClickListener(v -> {
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
        });
        btnAccept.setOnClickListener(v -> {
            if(edEmail!=null){
                String otp = generateOtp();
                String email = edEmail.getText().toString().trim();
                FirebaseSyncHelper syncHelper = new FirebaseSyncHelper(this);
                syncHelper.checkEmailExists(email, new OnEmailExists() {
                    @Override
                    public void onResult(boolean result) {
                        if (result){
                            ExecutorService executorService = Executors.newSingleThreadExecutor();
                            executorService.execute(() -> {
                                sendOtp(email, otp);
                            });
                            Intent intent = new Intent(getBaseContext(), VerifyCode.class);
                            intent.putExtra("otp", otp);
                            intent.putExtra("email", email);
                            startActivity(intent);
                            executorService.shutdown();
                        }
                        else{
                            Toast.makeText(getBaseContext(), "Email không tồn tại", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        });
    }
    public void sendOtp(String email, String otp){
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        String myEmail = "smartbuyshop25@gmail.com";
        String myPassword = "gtis urgx gkhk avah";
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(myEmail, myPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(myEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("Xác thực OTP");
            message.setText("Mã OTP của bạn là:"+otp);

            Transport.send(message);
            Log.d("Mail", "Email sent successfully");
        } catch (MessagingException e) {
            e.printStackTrace();
        }

    }
    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}

