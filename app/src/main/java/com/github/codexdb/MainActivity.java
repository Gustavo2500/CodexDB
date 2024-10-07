package com.github.codexdb;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private RequestCreator request;
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

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        request = new RequestCreator(MainActivity.this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ImageButton addButton = findViewById(R.id.addButton);
        ImageButton menuButton = findViewById(R.id.menuButton);
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, menuButton);
        popupMenu.getMenuInflater().inflate(R.menu.menu_main, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            exportDialogue();
            return true;
        });
        addButton.setOnClickListener(view -> scanCode());
        menuButton.setOnClickListener(view -> popupMenu.show());

        db = new BookDBHelper(this);
        RecyclerView bookListRV = findViewById(R.id.bookList);
        bookListRV.setHasFixedSize(true);
        bookList = new ArrayList<>();
        readBookTable();
        LinearLayoutManager linearLayout = new LinearLayoutManager(MainActivity.this);
        bookListRV.setLayoutManager(linearLayout);
        DividerItemDecoration divider = new DividerItemDecoration(bookListRV.getContext(), linearLayout.getOrientation());
        bookListRV.addItemDecoration(divider);
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
                if (result.getContents() != null) {
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
     * Displays a dialogue with the received book information and gives the option to add the book to the database or cancel.
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
            .setPositiveButton("Add", (dialog1, id) -> {
                Book book = new Book(ISBN, bookData.get(0).toString(), bookData.get(2).toString(), (Bitmap)bookData.get(3));
                addBookToDB(book);
            })
            .setNegativeButton("Cancel", (dialog12, id) -> Toast.makeText(getApplicationContext(), "Cancelled.", Toast.LENGTH_LONG).show())
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
    @SuppressLint("NotifyDataSetChanged")
    private void updateRecyclerView() {
        readBookTable();
        bookAdapter.setBookDataSet(bookList);
        bookAdapter.notifyDataSetChanged();
    }

    /**
     * Creates a dialogue that asks the user if they want to export the database to a PDF file.
     */
    private void exportDialogue() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle("Export list to PDF?");
        dialog.setPositiveButton("Yes", (dialogInterface, i) -> requestPermission())
            .setNegativeButton("No", (dialogInterface, i) -> {

            });
        dialog.show();
    }

    /**
     * Checks if the app has storage write permissions and asks for them if not.
     */
    private void requestPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_PHONE_STATE)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
            }
        }
        else {
            exportToPDF();
        }
    }

    /**
     *  Checks if the writing permissions were granted. Begins the database to PDF export process when granted.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200) {
            if(hasAllPermissionsGranted(grantResults)) {
                exportToPDF();
            }
            else {
                Toast.makeText(MainActivity.this, "Permissions needed to create PDF", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Checks if all of the premissions from a request were granted.
     * @param grantResults  A list of permission status codes.
     * @return              True if all permissions were granted. False if at least one wasn't granted.
     */
    public boolean hasAllPermissionsGranted(@NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }


    /**
     * Initiates the process to export the database to a PDF file.
     */
    private void exportToPDF() {
        if(bookList.size() == 0) {
            Toast.makeText(MainActivity.this, "There are no books to export", Toast.LENGTH_LONG).show();
        }
        else {
            PdfDocument doc = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 841, 1).create();
            PdfDocument.Page page = doc.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setTextSize(30);
            paint.setTextAlign(Paint.Align.LEFT);
            doc = writeToPDF(canvas, doc, paint, page);
            savePDF(doc);
        }
    }

    /**
     * Writes the contents of the list of Books received from the database into the PDF document.
     * @param canvas    The canvas used to paint the text.
     * @param doc       The document to write into.
     * @param paint     Contains the style and settings of the text to paint.
     * @param page      The first page of the document.
     * @return          The PDF document.
     */
    private PdfDocument writeToPDF(Canvas canvas, PdfDocument doc, Paint paint, PdfDocument.Page page) {
        String text;
        float x = 50;
        float y = 70;
        canvas.drawText("List of books", x, y, paint); //Title of the document
        paint.setTextSize(17);
        paint.setColor(Color.LTGRAY);
        canvas.drawText("CodexDB", 230, y, paint);
        paint.setTextSize(12);
        paint.setColor(Color.BLACK);
        y = 100; //Adds some space between the title and the next line of text
        int itemCount = 0;
        PdfDocument.Page nextPage = null;
        boolean multiplePages = false;
        /*
            Loop used to write the Books information into the document.
            Writes up to 45 entries per page. Checks with an item counter how many Books have been written.
            If the limit is reached, a new page is created.
         */
        for (int i = 0; i < bookList.size(); i++) {
            text = bookList.get(i).getISBN() + " - " + bookList.get(i).getTitle() + " - " + bookList.get(i).getAuthor();
            //Checks if the limit per page has been reached.
            if (itemCount < 45) {
                canvas.drawText(text, x, y, paint);
                y += 16;
            } else {
                //Finishes the first page after reaching the limit. Finishes the next pages added after the first iteration.
                if(!multiplePages) {
                    doc.finishPage(page);
                }
                else {
                    doc.finishPage(nextPage);
                }
                PdfDocument.PageInfo nextPageInfo = new PdfDocument.PageInfo.Builder(595, 841, 1).create();
                nextPage = doc.startPage(nextPageInfo);
                canvas = nextPage.getCanvas();
                y = 70;
                canvas.drawText(text, x, y, paint);
                y += 16;
                itemCount = 0;
                multiplePages = true;
            }
            itemCount += 1;
        }
        //Checks if the document has more than one page and finishes the correct page.
        if(!multiplePages) {
            doc.finishPage(page);
        }
        else {
            doc.finishPage(nextPage);
        }
        return doc;
    }

    /**
     * Saves the PDF document into the storage Download directory.
     * @param doc   The PDF document to save.
     */
    private void savePDF(PdfDocument doc) {
        File saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String fileName = "Book_list.pdf";
        File pdf = new File(saveDir, fileName);
        try {
            int duplicateCount = 1;
            /*
                Checks if the file already exists and adds a number at the end of its name. If a file
                with a number already exists, the number increases until finding a name that doesn't exist.
             */
            while (pdf.exists()) {
                pdf = new File(saveDir, "Book_list(" + duplicateCount + ").pdf");
                duplicateCount += 1;
            }
            FileOutputStream fos = new FileOutputStream(pdf);
            doc.writeTo(fos);
            doc.close();
            fos.close();
            Toast.makeText(MainActivity.this, "PDF created on Downloads folder", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "Error while creating PDF", Toast.LENGTH_LONG).show();
        }
    }
}