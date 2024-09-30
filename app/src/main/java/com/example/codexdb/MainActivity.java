package com.example.codexdb;

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
import android.view.View;

import androidx.navigation.ui.AppBarConfiguration;

import com.example.codexdb.databinding.ActivityMainBinding;
import com.example.codexdb.services.RequestCreator;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanCode();
            }
        });
    }

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
                        bookDialogue(request.getBookData());
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

    private void bookDialogue(ArrayList<Object> bookData) {
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
                    addBookToDB(bookData);
                    Toast.makeText(getApplicationContext(), "Book added.", Toast.LENGTH_LONG).show();
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

    private void addBookToDB(ArrayList<Object> bookData) {

    }
}