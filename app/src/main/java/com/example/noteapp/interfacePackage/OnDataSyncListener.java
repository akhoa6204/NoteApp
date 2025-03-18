package com.example.noteapp.interfacePackage;

import com.example.noteapp.model.NoteModel;
import com.example.noteapp.model.SharedNote;
import com.example.noteapp.model.User;

import java.util.List;

public interface OnDataSyncListener {
    void onNotesUpdated(List<NoteModel> updatesNotes);
    void onUserLoaded(User user);
    void onNoteLoaded(NoteModel note);
    void onSharedNoteLoaded(List<User> sharedUserList);
}
