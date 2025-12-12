package com.example.coffee_search.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

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
import com.example.coffee_search.model.SearchResult;
import com.example.coffee_search.repository.KeywordRepository;
import com.example.coffee_search.service.BoyerMoore;
import com.example.coffee_search.service.HeapSorter;
import com.example.coffee_search.service.HtmlFetcher; // [匯入] 抓取服務
import com.example.coffee_search.service.TranslationService;

@Controller
public class SearchController {

    @Autowired
    private TranslationService translationService;

    @Autowired
    private KeywordRepository keywordRepository;
    
    @Autowired
    private HtmlFetcher htmlFetcher; // [注入] HTML 抓取器

    @Value("${google.cse.apiKey}")
    private String apiKey;

    @Value("${google.cse.cx}")
    private String cx;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/api/keywords")
    @ResponseBody
    public List<Keyword> getKeywords() {
        return keywordRepository.findAll();
    }

    @PostMapping("/api/coffee_search")
    @ResponseBody
    public Map<String, Double> searchCoffee(
            @RequestParam("apiInput") String userQuery,
            @RequestParam(value = "keywordValue", defaultValue = "coffee") String keywordValue) {

        List<SearchResult> rawResults = new ArrayList<>();
        
        if (userQuery == null || userQuery.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            String detectedLang = translationService.detectLanguage(userQuery);
            String translatedKeywords = translationService.translate(keywordValue, detectedLang);
            String finalQuery = userQuery + " " + translatedKeywords;
            String q = URLEncoder.encode(finalQuery, StandardCharsets.UTF_8);

            // 1. 呼叫 Google API 取得初步名單 (20筆)
            for (int i = 0; i < 2; i++) {
                int start = 1 + (i * 10);
                String url = "https://www.googleapis.com/customsearch/v1?key=" + apiKey + 
                             "&cx=" + cx + 
                             "&num=10" + 
                             "&q=" + q + 
                             "&start=" + start;

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
                            String snippet = (String) item.get("snippet");

                            if (title != null && link != null) {
                                if (snippet == null) snippet = "";
                                double score = calculateBaseScore(title, userQuery);
                                rawResults.add(new SearchResult(title, link, snippet, score));
                            }
                        }
                    }
                }
            }

            // 2. [並行處理] 抓取完整內文並進行 Boyer-Moore 搜尋
            // 使用 parallelStream() 來加速網路請求
            List<Keyword> allKeywords = keywordRepository.findAll();
            BoyerMoore boyerMoore = new BoyerMoore();

            rawResults.parallelStream().forEach(res -> {
                // A. 抓取完整網頁內容 (最花時間的步驟)
                // 如果不想抓完整內容，註解掉下面這行，改用 String content = res.getTitle() + " " + res.getSnippet();
                String fullContent = htmlFetcher.fetchContent(res.getLink());
                
                // 如果抓不到完整內容，退回使用 snippet
                String contentToSearch = fullContent.isEmpty() ? 
                                       (res.getTitle() + " " + res.getSnippet()) : 
                                       fullContent;
                
                contentToSearch = contentToSearch.toLowerCase();

                // B. 使用 Boyer-Moore 演算法搜尋關鍵字
                for (Keyword k : allKeywords) {
                    if (k.getSearchTerm() != null && k.getWeight() != null) {
                        String pattern = k.getSearchTerm().toLowerCase();
                        
                        // 在完整內文中搜尋
                        if (boyerMoore.search(contentToSearch, pattern) != -1) {
                            // 加上權重
                            // 這裡需要注意執行緒安全，但 SearchResult 是獨立物件所以沒問題，
                            // 只要不共用同一個變數累加即可。
                            // 為了簡單起見，我們直接更新分數
                            synchronized (res) { // 簡單鎖定避免並發寫入衝突(雖然機率低)
                                res.setScore(res.getScore() + k.getWeight());
                            }
                            // System.out.println("在 " + res.getTitle() + " 找到關鍵字: " + pattern);
                        }
                    }
                }
            });

            // 3. 使用 Heap Tree 進行排序
            HeapSorter heapSorter = new HeapSorter();
            for (SearchResult result : rawResults) {
                heapSorter.insert(result);
            }

            // 4. 輸出結果
            Map<String, Double> sortedResultMap = new LinkedHashMap<>();
            List<SearchResult> sortedList = heapSorter.getSortedList();
            
            for (SearchResult res : sortedList) {
                sortedResultMap.put(res.getLink(), res.getScore());
            }

            return sortedResultMap;

        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    private double calculateBaseScore(String title, String query) {
        double score = 50.0;
        score += new Random().nextDouble() * 5; 
        return Math.round(score * 100.0) / 100.0;
    }
}