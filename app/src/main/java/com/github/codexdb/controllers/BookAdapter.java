package com.github.codexdb.controllers;


import static com.github.codexdb.models.Book.bytesToBitmap;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.github.codexdb.MainActivity;
import com.github.codexdb.R;
import com.github.codexdb.models.Book;

import java.util.ArrayList;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.ViewHolder> {

    private ArrayList<Book> bookDataSet;

    public void setBookDataSet(ArrayList<Book> data) {
        bookDataSet = data;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView itemCover;
        private final TextView itemTitle;
        private final TextView itemAuthor;
        private final TextView itemISBN;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemCover = itemView.findViewById(R.id.item_cover);
            this.itemTitle = itemView.findViewById(R.id.item_title);
            this.itemAuthor = itemView.findViewById(R.id.item_author);
            this.itemISBN = itemView.findViewById(R.id.item_isbn);
        }

        public ImageView getItemCover() {
            return itemCover;
        }

        public TextView getItemTitle() {
            return itemTitle;
        }

        public TextView getItemAuthor() {
            return itemAuthor;
        }

        public TextView getItemISBN() {
            return itemISBN;
        }
    }

    public BookAdapter(ArrayList<Book> dataSet) {
        this.bookDataSet = dataSet;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.book_row_item, parent, false);
        RelativeLayout layout = view.findViewById(R.id.item_layout);
        //layout.getBackground().setAlpha(30);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.getItemCover().setImageBitmap(bytesToBitmap((bookDataSet.get(position).getCover())));
        holder.getItemTitle().setText(bookDataSet.get(position).getTitle());
        holder.getItemAuthor().setText(bookDataSet.get(position).getAuthor());
        holder.getItemISBN().setText(bookDataSet.get(position).getISBN());
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Book book = new Book(((TextView)v.findViewById(R.id.item_isbn)).getText().toString(),
                        ((TextView)v.findViewById(R.id.item_title)).getText().toString(),
                        ((TextView)v.findViewById(R.id.item_author)).getText().toString(),
                        ((BitmapDrawable)(((ImageView)v.findViewById(R.id.item_cover)).getDrawable())).getBitmap());
                settingsDialogue(v.getContext(), book);
                return false;
            }
        });
    }

    private void settingsDialogue(Context context, Book book) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Book settings");
        //builder.setMessage("test");
        builder.setPositiveButton("Edit", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        editDialogue(context, book);
                        dialog.cancel();
                    }
                })
            .setNeutralButton("Cancel", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.cancel();
                    }
                })
            .setNegativeButton("Delete", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        if(context instanceof MainActivity){

                            MainActivity activity = (MainActivity)context;
                            activity.deleteBookFromDB(book.getISBN());
                        }
                        dialog.cancel();
                    }
                });
        builder.create().show();
    }

    private void editDialogue(Context context, Book book) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        LayoutInflater factory = LayoutInflater.from(context);
        View view = factory.inflate(R.layout.dialog_edit_book, null);
        EditText bookTitle = view.findViewById(R.id.edit_title);
        EditText bookAuthor = view.findViewById(R.id.edit_author);
        bookTitle.setText(book.getTitle());
        bookAuthor.setText(book.getAuthor());
        dialog.setView(view)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(context instanceof MainActivity){
                            book.setTitle(bookTitle.getText().toString());
                            book.setAuthor(bookAuthor.getText().toString());
                            MainActivity activity = (MainActivity)context;
                            activity.updateBookFromDB(book);
                        }
                        else{
                            Toast.makeText(context, "Book could not be updated.", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .setTitle("Book scanned");

        dialog.show();
    }


    @Override
    public int getItemCount() {
        return bookDataSet.size();
    }
}