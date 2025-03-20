package com.example.noteapp.model;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;

public class NoteModel implements Serializable {
    private String id;
    private String title;
    private List<NoteContent> content;

    private String tag;
    private String owner;
    private String createdAt;
    private String updatedAt;
    public NoteModel(){

    }
    public NoteModel(String id, String title, List<NoteContent> content, String tag, String owner, String createdAt, String updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.tag = tag;
        this.owner = owner;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<NoteContent> getContent() {
        return content;
    }

    public void setContent(List<NoteContent> content) {
        this.content = content;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getCreatedAt() {
        return createdAt.split(" ")[0];
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
