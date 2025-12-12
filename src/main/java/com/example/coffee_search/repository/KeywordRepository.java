package com.example.coffee_search.repository;

import com.example.coffee_search.model.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    // JpaRepository 已經內建了 findAll(), save() 等方法，所以這裡保持空白即可
}