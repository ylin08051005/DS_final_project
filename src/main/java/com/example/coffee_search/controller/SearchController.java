package com.example.coffee_search.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.example.coffee_search.service.TranslationService;

@Controller
public class SearchController {

    @Autowired
    private TranslationService translationService;

    @Value("${google.cse.apiKey}")
    private String apiKey;

    @Value("${google.cse.cx}")
    private String cx;

    // 定義要附加的後端關鍵字，這裡設定為 "coffee review" 以搜尋咖啡評論
    private final String BACKEND_KEYWORDS = " coffee review"; 

    // 顯示首頁
    @GetMapping("/")
    public String index() {
        return "index"; // 對應到 resources/templates/index.html 或 static/index.html
    }

    @PostMapping("/api/coffee_search")
    @ResponseBody
    public Map<String, String> searchCoffee(@RequestParam("apiInput") String userQuery) {
        Map<String, String> results = new HashMap<>();
        
        if (userQuery == null || userQuery.trim().isEmpty()) {
            return results;
        }

        try {
            // 1. 翻譯使用者輸入 (例如: "拿鐵" -> "Latte")
            String translatedQuery = translationService.translateToEnglish(userQuery);
            System.out.println("Original: " + userQuery + " -> Translated: " + translatedQuery);

            // 2. 結合關鍵字 (例如: "Latte" + " coffee review")
            String finalQuery = translatedQuery + BACKEND_KEYWORDS;

            // 3. 呼叫 Google Custom Search API
            String encodedQuery = URLEncoder.encode(finalQuery, StandardCharsets.UTF_8);
            String url = "https://www.googleapis.com/customsearch/v1?key=" + apiKey + "&cx=" + cx + "&num=10&q=" + encodedQuery;

            RestTemplate restTemplate = new RestTemplate();
            
            // 使用 Map.class 接收 JSON，也可以建立專門的 DTO class
            @SuppressWarnings("unchecked")
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);

            if (resp.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = resp.getBody();
                
                // 解析 items 陣列
                if (body.containsKey("items")) {
                    List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
                    for (Map<String, Object> item : items) {
                        String title = (String) item.get("title");
                        String link = (String) item.get("link");

                        if (title != null && link != null) {
                            results.put(title, link);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 實際專案中應使用 Logger 並回傳錯誤訊息給前端
        }
        return results;
    }
}