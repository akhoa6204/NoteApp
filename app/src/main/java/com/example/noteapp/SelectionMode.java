package com.example.noteapp;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SelectionMode extends ViewModel {
    private final MutableLiveData<Boolean> isSelectionMode = new MutableLiveData<>(false);
    public LiveData<Boolean> getSelectionMode() {
        return isSelectionMode;
    }

    public void setSelectionMode(boolean isSelection) {
        isSelectionMode.setValue(isSelection);
    }
}
