package com.github.codexdb.models;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;

/**
 *
 */
public class Book {
    private String isbn;
    private String title;
    private String author;
    private byte[] cover;

    /**
     * Creates an empty Book
     */
    public Book() {
        this.isbn = "";
        this.title = "";
        this.author = "";
        this.cover = null;
    }

    public Book(String isbn, String title, String author, Bitmap cover) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.cover = bitmapToBytes(cover);
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

    public byte[] getCover() {
        return cover;
    }


    public void setCover(byte[] cover) {
        this.cover = cover;
    }

    public String getISBN() {
        return isbn;
    }

    public void setISBN(String isbn) {
        this.isbn = isbn;
    }

    /**
     * Converts a bitmap into a byte array
     * @param img   The book's cover image as a Bitmap
     * @return      The processed Bitmap as a byte array
     */
    private byte[] bitmapToBytes(Bitmap img) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        img = Bitmap.createScaledBitmap(img, 42, 72, false);
        img.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        return byteArray;
    }

    /**
     * Converts a byte array into a Bitmap
     * @param img   The book's cover image as a byte array
     * @return      The processed byte arrat as a Bitmap
     */
    public static Bitmap bytesToBitmap(byte[] img) {
        return BitmapFactory.decodeByteArray(img, 0, img.length);
    }
}
