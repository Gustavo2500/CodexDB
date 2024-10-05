package com.github.codexdb.controllers;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.github.codexdb.models.Book;
import com.github.codexdb.models.Bookshelf.BookEntry;

import java.util.ArrayList;

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
                + BookEntry.COVER + " BLOB NOT NULL,"
                + "UNIQUE(" + BookEntry.ISBN + "))"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /**
     * Adds a book to the database
     */
    public long addBook(String ISBN, String title, String author, byte[] cover) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(BookEntry.ISBN, ISBN);
        cv.put(BookEntry.TITLE, title);
        cv.put(BookEntry.AUTHOR, author);
        cv.put(BookEntry.COVER, cover);
        long res =  database.insert(BookEntry.TABLE_NAME, null, cv);
        database.close();
        return res;
    }

    /**
     * Deletes a book from the database
     */
    public void deleteBook(String ISBN) {
        SQLiteDatabase database = this.getWritableDatabase();
        database.delete(BookEntry.TABLE_NAME, "isbn = ?", new String[]{ISBN});
        database.close();
    }

    /**
     * Updates a book's information from the database
     */
    public void updateBook(String title, String author, String ISBN) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(BookEntry.TITLE, title);
        cv.put(BookEntry.AUTHOR, author);
        database.update(BookEntry.TABLE_NAME, cv, "isbn = ?", new String[]{ISBN});
        database.close();
    }

    @SuppressLint("Range")
    public ArrayList<Book> readDatabase() {
        SQLiteDatabase database = this.getReadableDatabase();
        String sql = "SELECT * FROM BOOKS";
        Cursor cursor = database.rawQuery(sql, new String[]{});
        ArrayList<Book> bookList = new ArrayList<>();
        while(cursor.moveToNext()) {
            Book book = new Book();
            book.setTitle(cursor.getString(cursor.getColumnIndex("title")));
            book.setAuthor(cursor.getString(cursor.getColumnIndex("author")));
            book.setISBN(cursor.getString(cursor.getColumnIndex("isbn")));
            book.setCover(cursor.getBlob(cursor.getColumnIndex("cover")));
            bookList.add(book);
            Log.e("test1212", book.getTitle());
        }
        database.close();
        cursor.close();
        return bookList;
    }
}
