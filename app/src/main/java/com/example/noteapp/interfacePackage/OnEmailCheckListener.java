package com.example.noteapp.interfacePackage;

import com.example.noteapp.model.User;

public interface OnEmailCheckListener {
    void onResult(boolean exists, User user);
}
