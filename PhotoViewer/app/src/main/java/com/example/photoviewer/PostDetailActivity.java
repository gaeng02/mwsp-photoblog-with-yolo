package com.example.photoviewer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PostDetailActivity extends AppCompatActivity {
    private ImageView imageViewDetail;
    private TextView textViewTitle;
    private TextView textViewText;
    private TextView textViewDate;
    private Button buttonSave;
    private Button buttonShare;
    
    private String imageUrl;
    private Bitmap imageBitmap;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_post_detail);
        
        imageViewDetail = findViewById(R.id.imageViewDetail);
        textViewTitle = findViewById(R.id.textViewTitleDetail);
        textViewText = findViewById(R.id.textViewTextDetail);
        textViewDate = findViewById(R.id.textViewDateDetail);
        buttonSave = findViewById(R.id.buttonSave);
        buttonShare = findViewById(R.id.buttonShare);
        
        Intent intent = getIntent();
        String title = intent.getStringExtra("post_title");
        String text = intent.getStringExtra("post_text");
        String publishedDate = intent.getStringExtra("post_published_date");
        String image = intent.getStringExtra("post_image");
        String siteUrl = intent.getStringExtra("site_url");
        
        textViewTitle.setText(title != null ? title : "");
        textViewText.setText(text != null ? text : "");
        
        if (publishedDate != null && !publishedDate.isEmpty()) {
            textViewDate.setText("published: " + formatDate(publishedDate));
        } else {
            textViewDate.setText("published: -");
        }
        
        if (image != null && !image.isEmpty()) {
            if (image.startsWith("http")) {
                imageUrl = image;
            } else {
                imageUrl = siteUrl + image;
            }
            new LoadImageTask().execute(imageUrl);
        }
        
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImage();
            }
        });
        
        buttonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareImageLink();
            }
        });
    }
    
    private String formatDate(String dateStr) {
        if (dateStr != null && dateStr.length() >= 10) {
            return dateStr.substring(0, 10);
        }
        return dateStr;
    }
    
    private void saveImage() {
        if (imageBitmap == null) {
            Toast.makeText(this, "이미지를 불러오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "photo_" + System.currentTimeMillis() + ".jpg");
        values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/PhotoViewer");
        
        android.net.Uri uri = getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        
        try {
            java.io.OutputStream outputStream = getContentResolver().openOutputStream(uri);
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.close();
            Toast.makeText(this, "이미지가 저장되었습니다.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "이미지 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void shareImageLink() {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(this, "이미지 링크가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("이미지 링크", imageUrl);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "이미지 링크가 복사되었습니다.", Toast.LENGTH_SHORT).show();
    }
    
    private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.connect();
                
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream input = conn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    input.close();
                    conn.disconnect();
                    return bitmap;
                } else {
                    conn.disconnect();
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                imageBitmap = bitmap;
                imageViewDetail.setImageBitmap(bitmap);
            } else {
                Toast.makeText(PostDetailActivity.this, "이미지를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

