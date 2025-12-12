package com.example.coffee_search;

import com.example.coffee_search.model.Keyword;
import com.example.coffee_search.repository.KeywordRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CoffeeSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoffeeSearchApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(KeywordRepository repository) {
        return args -> {
            // 修正: 建構子參數對應新的欄位 searchTerm
            repository.save(new Keyword("咖啡評論 (Reviews)", "coffee reviews"));
            repository.save(new Keyword("咖啡店 (Shops)", "coffee shops"));
            repository.save(new Keyword("沖煮教學 (Brewing)", "coffee brewing methods"));
            repository.save(new Keyword("咖啡豆推薦 (Beans)", "best coffee beans"));
        };
    }
}