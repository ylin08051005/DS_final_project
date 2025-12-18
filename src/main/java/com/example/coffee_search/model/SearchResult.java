package com.example.coffee_search.model;

public class SearchResult {
    private String title;
    private String link;
    private String snippet;
    private double score;
    private String thumbnailLink; // [新增] 圖片縮圖連結

    // 無參數建構子
    public SearchResult() {}

    // 一般網頁搜尋用的建構子
    public SearchResult(String title, String link, String snippet, double score) {
        this.title = title;
        this.link = link;
        this.snippet = snippet;
        this.score = score;
    }

    // 圖片搜尋用的建構子
    public SearchResult(String title, String link, String thumbnailLink) {
        this.title = title;
        this.link = link;
        this.thumbnailLink = thumbnailLink;
        this.score = 0; // 圖片預設不計分
    }

    // --- Getters & Setters ---
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getThumbnailLink() { return thumbnailLink; }
    public void setThumbnailLink(String thumbnailLink) { this.thumbnailLink = thumbnailLink; }
}