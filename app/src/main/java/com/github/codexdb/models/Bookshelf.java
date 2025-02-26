package com.github.codexdb.models;

import android.provider.BaseColumns;

/**
 * Book table structure
 */
public class Bookshelf {


    public abstract class BookEntry implements BaseColumns {
        public static final String TABLE_NAME ="books";

        public static final String ID = "id";
        public static final String TITLE = "title";
        public static final String ISBN = "isbn";
        public static final String AUTHOR = "author";
        public static final String COVER = "cover";
    }
}
