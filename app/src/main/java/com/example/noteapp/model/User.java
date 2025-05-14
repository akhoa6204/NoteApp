package com.example.noteapp.model;

public class User {
    private String id, email, name, imgUrl;
    public User(){

    }
    public User(String id, String email, String name, String imgUrl){
        this.id = id;
        this.email=email;
        this.name=name;
        this.imgUrl=imgUrl;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
