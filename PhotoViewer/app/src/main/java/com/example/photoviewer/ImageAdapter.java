package com.example.photoviewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private final List<Post> postList;
    private final String baseUrl;
    private final OnItemClickListener clickListener;

    public interface OnItemClickListener {
        void onItemClick(Post post);
    }

    public ImageAdapter(List<Post> postList, String baseUrl, OnItemClickListener clickListener) {
        this.postList = postList;
        this.baseUrl = baseUrl;
        this.clickListener = clickListener;
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.textViewTitle.setText(post.getTitle());

        // published_date 표시
        String publishedDate = post.getPublished_date();
        if (publishedDate != null && !publishedDate.isEmpty()) {
            holder.textViewDate.setText("published: " + formatDate(publishedDate));
        } else {
            holder.textViewDate.setText("published: -");
        }

        // 이미지 로드
        holder.imageView.setImageBitmap(null);
        String imageUrl = post.getFullImageUrl(baseUrl);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            new LoadImageTask(holder.imageView).execute(imageUrl);
        }

        // 텍스트 표시
        String text = post.getText();
        if (text != null && !text.isEmpty()) {
            holder.textViewText.setText(text);
        } else {
            holder.textViewText.setText("");
        }
        
        // 클릭 리스너 설정
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    clickListener.onItemClick(post);
                }
            }
        });
    }

    private String formatDate(String dateStr) {
        // ISO 형식의 날짜를 간단하게 표시
        if (dateStr != null && dateStr.length() >= 10) {
            return dateStr.substring(0, 10);
        }
        return dateStr;
    }

    @Override
    public int getItemCount() {
        return postList != null ? postList.size() : 0;
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textViewTitle;
        TextView textViewDate;
        TextView textViewText;

        public ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewItem);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewDate = itemView.findViewById(R.id.textViewDate);
            textViewText = itemView.findViewById(R.id.textViewText);
        }
    }

    private static class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        private final ImageView imageView;

        public LoadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

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
            if (bitmap != null && imageView != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}
