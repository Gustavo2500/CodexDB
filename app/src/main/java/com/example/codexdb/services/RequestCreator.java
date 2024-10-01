package com.github.codexdb.services;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class  RequestCreator {

    private URL url;
    private String method;
    public int resultCode;
    private String bookName;
    private String bookKey;
    private String authorName = "";
    private Bitmap bookCover;

    public int sendRequest(String urlAPI, String methodType) {
        setConnection(urlAPI, methodType);
        resultCode = requestBookData();
        getBookCover("https://covers.openlibrary.org/b/olid/" + bookKey + ".jpg");
        return resultCode;
    }

    private void setConnection(String urlAPI, String methodType) {
        try {
            this.url = new URL(urlAPI);
        } catch (MalformedURLException e) {

        }
        this.method = methodType;
    }

    private int requestBookData() {
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(10000);
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                resultCode = formatBookData(new BufferedInputStream(conn.getInputStream()));

                return resultCode;
            }
            else {
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    resultCode = 4;
                    conn.disconnect();
                    return resultCode;
                }
            }
        } catch (IOException e) {
            resultCode = 5;
            return resultCode;
        }
        resultCode = 0;
        return resultCode;
    }

    private int formatBookData(InputStream inputStream) {
        InputStream in = inputStream;
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder result = new StringBuilder();
        String line;
        try{
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            JSONObject retObj = new JSONObject(result.toString());
            bookName = retObj.getString("title");
            bookKey = retObj.getString("key").replace("/books/", "");
            if(retObj.has("authors")) {
                JSONArray author = retObj.getJSONArray("authors");
                getAuthor(author.getJSONObject(0).getString("key").replace("/authors/", ""));
            }
            else {
                authorName = "Not found";
            }

            resultCode = 1;
            return resultCode;

        }catch(IOException e){
            resultCode = 2;
            return resultCode;
        } catch (JSONException e) {
            resultCode = 3;
            return resultCode;
        }
    }

    private void getAuthor(String authorKey) {
        try{
            HttpURLConnection conn = (HttpURLConnection) new URL("https://openlibrary.org/authors/" + authorKey + ".json").openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream in = new BufferedInputStream(conn.getInputStream());
                conn.disconnect();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder result = new StringBuilder();
                String line;
                try{
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    JSONObject retObj = new JSONObject(result.toString());
                    authorName = retObj.getString("name");
                }catch(IOException e){
                    authorName = "Not found";
                } catch (JSONException e) {
                    authorName = "Not found";
                }

            }
            else {
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    authorName = authorKey;
                    conn.disconnect();

                }
            }
        } catch (IOException e) {
            authorName = "Not found";
        }
    }

    private void getBookCover(String url) {
        try {
            bookCover = BitmapFactory.decodeStream((InputStream)new URL(url).getContent());
        } catch (MalformedURLException e) {
            resultCode = 2;
        } catch (IOException e) {
            resultCode = 2;
        }
    }

    public void resetRequest() {
        url = null;
        method = null;
        resultCode = 0;
        bookName = null;
        bookKey = null;
    }

    public int getResultCode() {
        return resultCode;
    }

    public ArrayList<Object> getBookData() {
        ArrayList<Object> data = new ArrayList<Object>();
        data.add(bookName);
        data.add(bookKey);
        data.add(authorName);
        data.add(bookCover);
        return data;
    }
}
