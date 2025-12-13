package com.example.coffee_search.config;

import com.example.coffee_search.model.Keyword;
import com.example.coffee_search.repository.KeywordRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initDatabase(KeywordRepository repository) {
        return args -> {
            // 先清空舊資料，避免重複啟動時資料疊加 (視需求而定)
            repository.deleteAll();

            // --- 英文權重表 (en) ---
            repository.save(new Keyword("Reviews", "coffee reviews", 5, "en"));
            repository.save(new Keyword("Shops", "coffee shops", 3, "en"));
            repository.save(new Keyword("Brewing", "brewing methods", 4, "en"));
            repository.save(new Keyword("Beans", "coffee beans", 5, "en"));
            repository.save(new Keyword("Latte", "latte", 2, "en"));
            repository.save(new Keyword("Espresso", "espresso", 5, "en"));

            // --- 中文權重表 (zh) ---
            // 注意：這裡的關鍵字必須是爬蟲抓下來的文章中會出現的詞
            repository.save(new Keyword("咖啡評論", "咖啡評論", 5, "zh"));
            repository.save(new Keyword("好喝", "好喝", 3, "zh"));
            repository.save(new Keyword("推薦", "推薦", 4, "zh"));
            repository.save(new Keyword("沖煮", "沖煮", 4, "zh"));
            repository.save(new Keyword("咖啡豆", "咖啡豆", 5, "zh"));
            repository.save(new Keyword("拿鐵", "拿鐵", 2, "zh"));
            repository.save(new Keyword("濃縮", "濃縮咖啡", 5, "zh"));

            // --- 日文權重表 (ja) ---
            repository.save(new Keyword("Review", "レビュー", 5, "ja")); // review
            repository.save(new Keyword("Delicious", "美味しい", 4, "ja")); // delicious
            repository.save(new Keyword("Cafe", "カフェ", 3, "ja")); // cafe
            repository.save(new Keyword("Coffee Beans", "コーヒー豆", 5, "ja")); // coffee beans
            repository.save(new Keyword("Brewing", "淹れ方", 4, "ja")); // brewing method
            repository.save(new Keyword("Latte", "ラテ", 2, "ja"));
            repository.save(new Keyword("Delicious (Kana)", "おいしい", 4, "ja")); 
            repository.save(new Keyword("Coffee (Katakana)", "コーヒー", 5, "ja"));

            System.out.println("多語言關鍵字資料初始化完成！");
        };
    }
}