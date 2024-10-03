package com.example.codexdb.controllers;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.codexdb.models.Bookshelf.BookEntry;

/**
 * Database handler class
 */
public class BookDBHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Library.db";

    public BookDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE BOOKS ("
                + BookEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + BookEntry.ISBN + " TEXT NOT NULL,"
                + BookEntry.TITLE + " TEXT NOT NULL,"
                + BookEntry.AUTHOR + " TEXT NOT NULL,"
                + BookEntry.COVER + " BLOB)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /**
     * Adds a book to the database
     * @param ISBN
     * @param title
     * @param author       
     * @param cover
     */
    public void addBook(String ISBN, String title, String author, byte[] cover) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(BookEntry.ISBN, ISBN);
        cv.put(BookEntry.TITLE, title);
        cv.put(BookEntry.AUTHOR, author);
        cv.put(BookEntry.COVER, cover);
        database.insert(BookEntry.TABLE_NAME, null, cv);
        database.close();
    }
}
