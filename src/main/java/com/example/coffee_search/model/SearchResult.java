package com.example.coffee_search.model;

public class SearchResult {
    private String title;
    private String link;
    private String snippet;
    private double score;

    // 無參數建構子 (NoArgsConstructor)
    public SearchResult() {
    }

    // 全參數建構子 (AllArgsConstructor)
    public SearchResult(String title, String link, String snippet, double score) {
        this.title = title;
        this.link = link;
        this.snippet = snippet;
        this.score = score;
    }

    // --- Getters and Setters ---
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}