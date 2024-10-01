package com.example.codexdb.models;

import android.graphics.Bitmap;

public class Book {
    private String title;
    private String author;
    private Bitmap cover;

    public Book() {
        this.title = "";
        this.author = "";
        this.cover = null;
    }

    public Book(String title, String author, Bitmap cover) {
        this.title = title;
        this.author = author;
        this.cover = cover;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Bitmap getCover() {
        return cover;
    }

    public void setCover(Bitmap cover) {
        this.cover = cover;
    }
}
