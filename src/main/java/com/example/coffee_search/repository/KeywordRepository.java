package com.example.coffee_search.repository;

import com.example.coffee_search.model.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    
    // [新增] 根據語言代碼查詢關鍵字
    // Spring Data JPA 會自動根據方法名稱產生 SQL: SELECT * FROM keyword WHERE language = ?
    List<Keyword> findByLanguage(String language);
    
}