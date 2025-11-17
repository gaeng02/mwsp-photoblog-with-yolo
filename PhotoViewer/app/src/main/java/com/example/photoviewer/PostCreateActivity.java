package com.example.photoviewer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PostCreateActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    
    EditText editTextTitle;
    EditText editTextText;
    ImageView imageViewPreview;
    Button buttonSelectImage;
    Button buttonRemoveImage;
    Button buttonSubmit;
    TextView textViewStatus;
    
    String site_url = "https://gaeng02.pythonanywhere.com";
    Bitmap selectedBitmap;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_post_create);
        
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextText = findViewById(R.id.editTextText);
        imageViewPreview = findViewById(R.id.imageViewPreview);
        buttonSelectImage = findViewById(R.id.buttonSelectImage);
        buttonRemoveImage = findViewById(R.id.buttonRemoveImage);
        buttonSubmit = findViewById(R.id.buttonSubmit);
        textViewStatus = findViewById(R.id.textViewStatus);
        
        buttonSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });
        
        buttonRemoveImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeImage();
            }
        });
        
        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitPost();
            }
        });
        
        updateImageVisibility();
    }
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                selectedBitmap = BitmapFactory.decodeStream(inputStream);
                imageViewPreview.setImageBitmap(selectedBitmap);
                inputStream.close();
                updateImageVisibility();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "이미지를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void removeImage() {
        selectedBitmap = null;
        imageViewPreview.setImageBitmap(null);
        updateImageVisibility();
    }
    
    private void updateImageVisibility() {
        if (selectedBitmap != null) {
            imageViewPreview.setVisibility(View.VISIBLE);
            buttonRemoveImage.setVisibility(View.VISIBLE);
            buttonSelectImage.setText("이미지 변경");
        } else {
            imageViewPreview.setVisibility(View.GONE);
            buttonRemoveImage.setVisibility(View.GONE);
            buttonSelectImage.setText("이미지 선택 (선택사항)");
        }
    }
    
    private void submitPost() {
        String title = editTextTitle.getText().toString().trim();
        String text = editTextText.getText().toString().trim();
        
        if (title.isEmpty()) {
            Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (text.isEmpty()) {
            Toast.makeText(this, "내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        textViewStatus.setText("게시글 업로드 중...");
        buttonSubmit.setEnabled(false);
        
        CreatePostTask task = new CreatePostTask();
        task.execute(title, text);
    }
    
    private class CreatePostTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            String title = params[0];
            String text = params[1];
            
            try {
                URL url = new URL(site_url + "/api_root/Post/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                String currentDate = sdf.format(new Date());
                
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("author", 1);
                jsonObject.put("title", title);
                jsonObject.put("text", text);
                jsonObject.put("published_date", currentDate);
                
                OutputStream os = conn.getOutputStream();
                os.write(jsonObject.toString().getBytes("UTF-8"));
                os.flush();
                os.close();
                
                int responseCode = conn.getResponseCode();
                conn.disconnect();
                
                return responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean success) {
            buttonSubmit.setEnabled(true);
            
            if (success) {
                textViewStatus.setText("게시글 작성 완료!");
                Toast.makeText(PostCreateActivity.this, "게시글이 작성되었습니다.", Toast.LENGTH_SHORT).show();
                
                Intent resultIntent = new Intent();
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                textViewStatus.setText("게시글 작성 실패");
                Toast.makeText(PostCreateActivity.this, "게시글 작성에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

