package com.example.photoviewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    TextView textView;
    RecyclerView recyclerView;
    EditText editTextSearch;
    ImageAdapter adapter;
    String site_url = "https://gaeng02.pythonanywhere.com";
    List<Post> postList = new ArrayList<>();
    List<Post> allPostList = new ArrayList<>(); // 전체 포스트 리스트

    CloudImage taskDownload;

    JSONObject post_json;
    String imageUrl;
    List<Bitmap> bitmapList = new ArrayList<>();
    String lastJsonResponse;
    List<Post> lastParsedPosts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        recyclerView = findViewById(R.id.recyclerView);
        editTextSearch = findViewById(R.id.editTextSearch);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 검색 기능
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPosts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ImageAdapter imageAdapter = new ImageAdapter(postList, site_url, new ImageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Post post) {
                Intent intent = new Intent(MainActivity.this, PostDetailActivity.class);
                intent.putExtra("post_id", post.getId());
                intent.putExtra("post_title", post.getTitle());
                intent.putExtra("post_text", post.getText());
                intent.putExtra("post_published_date", post.getPublished_date());
                intent.putExtra("post_image", post.getImage());
                intent.putExtra("site_url", site_url);
                startActivity(intent);
            }
        });
        adapter = imageAdapter;
        recyclerView.setAdapter(adapter);
    }

    public void onClickDownload(View v) {
        textView.setText("동기화 중...");
        taskDownload = new CloudImage();
        taskDownload.execute(site_url + "/api_root/Post/");
    }

    public void onClickUpload(View v) {
        Intent intent = new Intent(this, PostCreateActivity.class);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            textView.setText("동기화 중...");
            taskDownload = new CloudImage();
            taskDownload.execute(site_url + "/api_root/Post/");
        }
    }

    private class CloudImage extends AsyncTask<String, Integer, List<Bitmap>> {
        @Override
        protected List<Bitmap> doInBackground(String... params) {
            List<Post> posts = new ArrayList<>();
            bitmapList.clear();

            try {
                URL url = new URL(params[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();

                    lastJsonResponse = result.toString();

                    JSONArray jsonArray = new JSONArray(result.toString());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        post_json = (JSONObject) jsonArray.get(i);

                        int id = post_json.optInt("id", 0);
                        int author = post_json.optInt("author", 0);
                        String title = post_json.optString("title", "");
                        String text = post_json.optString("text", "");
                        String created_date = post_json.optString("created_date", "");
                        String published_date = post_json.optString("published_date", "");
                        String image = post_json.optString("image", "");

                        if (!image.isEmpty()) {
                            try {
                                URL myImageUrl = new URL(image);
                                HttpURLConnection imgConn = (HttpURLConnection) myImageUrl.openConnection();
                                imgConn.setRequestMethod("GET");
                                imgConn.setConnectTimeout(5000);
                                imgConn.setReadTimeout(5000);
                                imgConn.connect();

                                if (imgConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                    InputStream imgStream = imgConn.getInputStream();
                                    Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);
                                    if (imageBitmap != null) {
                                        bitmapList.add(imageBitmap);
                                    }
                                    imgStream.close();
                                }
                                imgConn.disconnect();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        Post post = new Post(id, author, title, text, created_date, published_date, image);
                        posts.add(post);
                    }
                    lastParsedPosts = posts;
                } else {
                    lastParsedPosts = new ArrayList<>();
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                lastParsedPosts = new ArrayList<>();
            }

            return bitmapList;
        }

        @Override
        protected void onPostExecute(List<Bitmap> images) {
            try {
                if (lastParsedPosts != null && !lastParsedPosts.isEmpty()) {
                    allPostList.clear();
                    allPostList.addAll(lastParsedPosts);
                    
                    // 검색어가 있으면 필터링, 없으면 전체 표시
                    String searchText = editTextSearch.getText().toString();
                    filterPosts(searchText);

                    SimpleDateFormat sdf = new SimpleDateFormat("MM월 dd일 HH:mm", Locale.getDefault());
                    String syncTime = sdf.format(new Date());
                    textView.setText("동기화 완료! (" + allPostList.size() + "개) - 마지막 동기화: " + syncTime);
                } else {
                    textView.setText("불러올 데이터가 없습니다.");
                    allPostList.clear();
                    postList.clear();
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                textView.setText("동기화 실패: " + e.getMessage());
                allPostList.clear();
                postList.clear();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }

    private void filterPosts(String searchText) {
        postList.clear();
        
        if (searchText == null || searchText.trim().isEmpty()) {
            // 검색어가 없으면 전체 표시
            postList.addAll(allPostList);
        } else {
            // title 기반으로 필터링
            String searchLower = searchText.toLowerCase().trim();
            for (Post post : allPostList) {
                if (post.getTitle() != null && post.getTitle().toLowerCase().contains(searchLower)) {
                    postList.add(post);
                }
            }
        }
        
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
