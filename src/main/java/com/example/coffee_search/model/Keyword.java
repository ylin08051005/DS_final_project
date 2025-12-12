package com.example.coffee_search.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Keyword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;       // 顯示名稱 (如: 咖啡評論)
    private String searchTerm; // 修正: 改名避開 SQL 保留字 "value"

    public Keyword(String name, String searchTerm) {
        this.name = name;
        this.searchTerm = searchTerm;
    }
}