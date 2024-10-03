package com.example.codexdb;

import static com.example.codexdb.models.Bookshelf.BookEntry.AUTHOR;
import static com.example.codexdb.models.Bookshelf.BookEntry.COVER;
import static com.example.codexdb.models.Bookshelf.BookEntry.TABLE_NAME;
import static com.example.codexdb.models.Bookshelf.BookEntry.TITLE;
import static com.example.codexdb.models.Bookshelf.BookEntry.ID;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.widget.PopupMenu;
import androidx.navigation.ui.AppBarConfiguration;

import com.example.codexdb.controllers.BookDBHelper;
import com.example.codexdb.databinding.ActivityMainBinding;
import com.example.codexdb.models.Book;
import com.example.codexdb.services.RequestCreator;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private String bookName = null;
    private String bookKey = null;
    private int resultCode = 0;
    private final RequestCreator request = new RequestCreator();
    private final int NOT_FOUND_ERROR = 4;
    private final int IO_CONNECTION_ERROR = 5;
    private final int DEFAULT_ERROR = 0;
    private final int JSON_ERROR = 3;
    private final int READ_ERROR = 2;
    private final int CONNECTION_OK = 1;
    private BookDBHelper db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ImageButton addButton = findViewById(R.id.addButton);
        ImageButton menuButton = findViewById(R.id.menuButton);
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, menuButton);
        popupMenu.getMenuInflater().inflate(R.menu.menu_main, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Toast.makeText(MainActivity.this, item.getTitle(), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanCode();
            }
        });
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupMenu.show();
            }
        });

        db = new BookDBHelper(this);
    }

    /**
     * Opens the ZXing camera scan
     */
    private void scanCode() {
        new IntentIntegrator(this)
                .setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES)
                .setPrompt("Scan the book's barcode.")
                .setTorchEnabled(false)
                .setBeepEnabled(true)
                .initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(data != null && resultCode != RESULT_CANCELED) {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null) {
                if (result.getContents() != null && resultCode != RESULT_CANCELED) {
                    startRequestProcess(result);
                } else {
                    Toast.makeText(getApplicationContext(), "Scan cancelled.", Toast.LENGTH_LONG).show();
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    /**
     * Begins the transaction process with the API to obtain the book data
     * @param result    The scanned data from the previous activity
     */
    private void startRequestProcess(IntentResult result) {
        String ISBN = result.getContents();
        binding.scanResult.setText(result.getContents());
        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Loading...");
        progress.setMessage("Please wait...");
        progress.setCancelable(false);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        progress.show();
        executor.execute(() -> {

            request.sendRequest("https://openlibrary.org/isbn/" + ISBN + ".json", "GET");
            handler.post(() -> {
                progress.dismiss();
                switch(request.getResultCode()) {
                    case CONNECTION_OK:
                        bookDialogue(request.getBookData(), ISBN);
                        request.resetRequest();
                        break;
                    case NOT_FOUND_ERROR:
                        Toast.makeText(getApplicationContext(), "The book could not be found.", Toast.LENGTH_LONG).show();
                        request.resetRequest();
                        break;
                    case JSON_ERROR:
                        Toast.makeText(getApplicationContext(), "An error has occurred.", Toast.LENGTH_LONG).show();
                        request.resetRequest();
                        break;
                }
            });
        });
    }

    /**
     * Displays a dialogue with the received book information and gives the option to add the book to the database or cancel
     * @param bookData  The book's information received from the API
     * @param ISBN      The book's code scanned previously with scanCode()
     */
    private void bookDialogue(ArrayList<Object> bookData, String ISBN) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater factory = LayoutInflater.from(MainActivity.this);
        View view = factory.inflate(R.layout.dialog_add_book, null);
        TextView bookTitle = view.findViewById(R.id.dialog_title);
        TextView bookAuthor = view.findViewById(R.id.dialog_author);
        bookTitle.setText("Book: " + bookData.get(0));
        bookAuthor.setText("Author: " + bookData.get(2));
        ImageView bookCover = view.findViewById(R.id.dialog_cover);
        bookCover.setImageBitmap(Bitmap.createScaledBitmap((Bitmap)bookData.get(3), 210, 360, false));
        dialog.setView(view)
            .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Book book = new Book(ISBN, bookData.get(0).toString(), bookData.get(2).toString(), (Bitmap)bookData.get(3));
                    addBookToDB(book);
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Toast.makeText(getApplicationContext(), "Cancelled.", Toast.LENGTH_LONG).show();
                }
            })
            .setTitle("Book scanned");

        dialog.show();

    }

    /**
     * Adds the book's information to the database
     * @param book  The book's information stored as a Book object
     */
    private void addBookToDB(Book book) {
        db.addBook(book.getISBN(), book.getTitle(), book.getAuthor(), book.getCover());
        SQLiteDatabase database = db.getWritableDatabase();
        Toast.makeText(getApplicationContext(), "Book added.", Toast.LENGTH_LONG).show();
    }
}