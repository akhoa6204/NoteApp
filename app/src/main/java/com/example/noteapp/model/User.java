package com.example.noteapp.model;

import android.text.TextUtils;
import android.util.Patterns;

import java.util.regex.Pattern;

public class User {
    private String id, email, firstName, lastName, password;
    public User(){

    }
    public User(String id, String email, String firstName, String lastName, String password){
        this.id = id;
        this.email=email;
        this.firstName=firstName;
        this.lastName=lastName;
        this.password=password;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
