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
            // [修改] 初始化時加入權重
            repository.save(new Keyword("咖啡評論 (Reviews)", "coffee reviews", 5));
            repository.save(new Keyword("咖啡店 (Shops)", "coffee shops", 3));
            repository.save(new Keyword("沖煮教學 (Brewing)", "coffee brewing methods", 4));
            repository.save(new Keyword("咖啡豆推薦 (Beans)", "best coffee beans", 5));
            // 您也可以加入單字關鍵字來做額外加權
            repository.save(new Keyword("拿鐵 (Latte)", "latte", 2));
        };
    }
}