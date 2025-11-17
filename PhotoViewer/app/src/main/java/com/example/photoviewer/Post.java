package com.example.photoviewer;

public class Post {
    private int id;
    private int author;
    private String title;
    private String text;
    private String created_date;
    private String published_date;
    private String image;

    public Post(int id, int author, String title, String text, 
                String created_date, String published_date, String image) {
        this.id = id;
        this.author = author;
        this.title = title;
        this.text = text;
        this.created_date = created_date;
        this.published_date = published_date;
        this.image = image;
    }

    public int getId() {
        return id;
    }

    public int getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String getCreated_date() {
        return created_date;
    }

    public String getPublished_date() {
        return published_date;
    }

    public String getImage() {
        return image;
    }

    public String getFullImageUrl(String baseUrl) {
        if (image != null && !image.isEmpty()) {
            if (image.startsWith("http")) {
                return image;
            }
            return baseUrl + image;
        }
        return null;
    }
}

