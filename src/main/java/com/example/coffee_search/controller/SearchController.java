package com.example.coffee_search.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

import com.example.coffee_search.model.Keyword;
import com.example.coffee_search.repository.KeywordRepository;
import com.example.coffee_search.service.TranslationService;

@Controller
public class SearchController {

    @Autowired
    private TranslationService translationService;

    @Autowired
    private KeywordRepository keywordRepository; // 注入 Repository

    @Value("${google.cse.apiKey}")
    private String apiKey;

    @Value("${google.cse.cx}")
    private String cx;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    // [新增] 取得所有關鍵字選項的 API
    @GetMapping("/api/keywords")
    @ResponseBody
    public List<Keyword> getKeywords() {
        return keywordRepository.findAll();
    }

    @PostMapping("/api/coffee_search")
    @ResponseBody
    public List<Map<String, String>> searchCoffee(
            @RequestParam("apiInput") String userQuery,
            @RequestParam(value = "keywordValue", defaultValue = "coffee") String keywordValue) { // [修改] 接收前端選的關鍵字

        List<Map<String, String>> results = new ArrayList<>();
        
        if (userQuery == null || userQuery.trim().isEmpty()) {
            return results;
        }

        try {
            // 1. 偵測語言
            String detectedLang = translationService.detectLanguage(userQuery);

            // 2. 翻譯前端傳來的關鍵字 (keywordValue)
            String translatedKeywords = translationService.translate(keywordValue, detectedLang);
            
            // 3. 組合
            String finalQuery = userQuery + " " + translatedKeywords;
            System.out.println("搜尋字串: " + finalQuery);

            // 4. 呼叫 Google API
            String q = URLEncoder.encode(finalQuery, StandardCharsets.UTF_8);
            String url = "https://www.googleapis.com/customsearch/v1?key=" + apiKey + "&cx=" + cx + "&num=10&q=" + q;

            RestTemplate restTemplate = new RestTemplate();
            @SuppressWarnings("unchecked")
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            
            if (resp.getBody() != null) {
                Map<String, Object> body = resp.getBody();
                if (body.containsKey("items")) {
                    List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
                    for (Map<String, Object> item : items) {
                        String title = (String) item.get("title");
                        String link = (String) item.get("link");
                        if (title != null && link != null) {
                            Map<String, String> entry = new HashMap<>();
                            entry.put("title", title);
                            entry.put("link", link);
                            results.add(entry);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }
}