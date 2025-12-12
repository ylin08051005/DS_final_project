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

    private String name;       // 顯示名稱
    private String searchTerm; // 搜尋關鍵字
    private Integer weight;    // 權重分數

    // 無參數建構子 (JPA 需要)
    public Keyword() {
    }

    // 自訂建構子
    public Keyword(String name, String searchTerm, Integer weight) {
        this.name = name;
        this.searchTerm = searchTerm;
        this.weight = weight;
    }

    // --- Getters and Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }
}