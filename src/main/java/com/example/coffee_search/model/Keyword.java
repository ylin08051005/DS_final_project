package com.example.coffee_search.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Keyword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;       // 顯示名稱 (例如: "濃縮咖啡")
    private String searchTerm; // 實際搜尋/比對的關鍵字 (例如: "espresso")
    private Integer weight;    // 權重分數
    
    // [新增] 語言標籤 (例如: "en", "zh", "ja")
    private String language; 

    public Keyword() {
    }

    // [修改] 建構子加入 language
    public Keyword(String name, String searchTerm, Integer weight, String language) {
        this.name = name;
        this.searchTerm = searchTerm;
        this.weight = weight;
        this.language = language;
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSearchTerm() { return searchTerm; }
    public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }

    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }

    // [新增] Language 的 Getter/Setter
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}