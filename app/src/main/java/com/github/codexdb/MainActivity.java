package com.github.codexdb;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.widget.PopupMenu;
import androidx.navigation.ui.AppBarConfiguration;

import com.github.codexdb.controllers.BookAdapter;
import com.github.codexdb.controllers.BookDBHelper;
import com.github.codexdb.databinding.ActivityMainBinding;
import com.github.codexdb.models.Book;
import com.github.codexdb.services.RequestCreator;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private ArrayList<Book> bookList;
    private BookAdapter bookAdapter;


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
        RecyclerView bookListRV = findViewById(R.id.bookList);
        bookListRV.setHasFixedSize(true);
        bookList = new ArrayList<Book>();
        readBookTable();
        LinearLayoutManager linearLayout = new LinearLayoutManager(MainActivity.this);
        bookListRV.setLayoutManager(linearLayout);
        bookAdapter = new BookAdapter(bookList);
        bookListRV.setAdapter(bookAdapter);
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
        TextView bookTitle = view.findViewById(R.id.edit_title_label);
        TextView bookAuthor = view.findViewById(R.id.edit_author_label);
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
        long res = db.addBook(book.getISBN(), book.getTitle(), book.getAuthor(), book.getCover());
        if(res != -1) {
            updateRecyclerView();
            Toast.makeText(getApplicationContext(), "Book added.", Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(getApplicationContext(), "The book already exists.", Toast.LENGTH_LONG).show();
        }

    }

    /**
     * Deletes the book from the database
     */
    public void deleteBookFromDB(String ISBN) {
        db.deleteBook(ISBN);
        updateRecyclerView();
        Toast.makeText(getApplicationContext(), "Book deleted.", Toast.LENGTH_LONG).show();
    }

    /**
     * Updates the book from the database
     * @param book  The book's information stored as a Book object
     */
    public void updateBookFromDB(Book book) {
        db.updateBook(book.getTitle(), book.getAuthor(), book.getISBN());
        updateRecyclerView();
        Toast.makeText(getApplicationContext(), "Book updated.", Toast.LENGTH_LONG).show();
    }

    /**
     * Reads the database and stores the books found
     */
    private void readBookTable() {
        bookList = db.readDatabase();
    }

    /**
     * Updates the book dataset and notifies the RecyclerView adapter to update the list of books
     */
    private void updateRecyclerView() {
        readBookTable();
        bookAdapter.setBookDataSet(bookList);
        bookAdapter.notifyDataSetChanged();
    }
}