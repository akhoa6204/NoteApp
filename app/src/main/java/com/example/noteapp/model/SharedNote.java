package com.example.noteapp.model;

public class SharedNote {
    private String shareId, noteId, sharedUser;
    private int permission;
    public SharedNote(){

    }
    public SharedNote(String shareId, String noteId, int permission, String sharedUser){
        this.shareId = shareId;
        this.noteId = noteId;
        this.permission=permission;
        this.sharedUser=sharedUser;
    }

    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public int getPermission() {
        return permission;
    }

    public void setPermission(int permission) {
        this.permission = permission;
    }

    public String getSharedUser() {
        return sharedUser;
    }

    public void setSharedUser(String sharedUser) {
        this.sharedUser = sharedUser;
    }
}
