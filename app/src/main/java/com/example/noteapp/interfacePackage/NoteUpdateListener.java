package com.example.noteapp.interfacePackage;

import org.json.JSONObject;

import java.util.List;

public interface NoteUpdateListener {
    void onTitleUpdated(String newTitle);
    void onContentUpdated(List<Object> contentList);

}


