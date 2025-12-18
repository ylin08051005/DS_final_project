package com.example.coffee_search.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.example.coffee_search.model.GeminiAnalysisResult;
import com.example.coffee_search.model.Keyword;
import com.example.coffee_search.model.SearchResult;
import com.example.coffee_search.repository.KeywordRepository;
import com.example.coffee_search.service.BoyerMoore;
import com.example.coffee_search.service.DeepRankingService;
import com.example.coffee_search.service.GeminiService;
import com.example.coffee_search.service.HtmlFetcher;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class CoffeeSearchControllerV2 {

    @Autowired private GeminiService geminiService;
    @Autowired private KeywordRepository keywordRepository;
    @Autowired private HtmlFetcher htmlFetcher;
    @Autowired private DeepRankingService deepRankingService;

    @Value("${google.cse.apiKey}") private String apiKey;
    @Value("${google.cse.cx}") private String cx;

    @PostMapping("/api/v2/search")
    @ResponseBody
    public Map<String, Double> searchCoffeeV2(@RequestParam("apiInput") String userQuery) {
        
        System.out.println("🚀 啟動 V2 全新搜尋引擎");
        List<SearchResult> cleanResults = new ArrayList<>();

        try {
            // 1. Gemini 分析 (語言偵測與查詢優化)
            GeminiAnalysisResult analysis = geminiService.analyzeQuery(userQuery);
            String detectedLang = analysis.language();
            String finalSearchQuery = analysis.query();
            
            // 2. 處理關鍵字邏輯與語言後綴
            String targetLangForKeywords = "en";
            if ("zh".equals(detectedLang)) {
                targetLangForKeywords = "zh";
                if (!finalSearchQuery.contains("咖啡")) finalSearchQuery += " 咖啡";
            } else if ("ja".equals(detectedLang)) {
                targetLangForKeywords = "ja";
                if (!finalSearchQuery.contains("コーヒー")) finalSearchQuery += " コーヒー";
            } else {
                if (!finalSearchQuery.toLowerCase().contains("coffee")) finalSearchQuery += " coffee";
            }

            String q = URLEncoder.encode(finalSearchQuery, StandardCharsets.UTF_8);
            System.out.println("V2 搜尋詞: " + finalSearchQuery);

            // 3. Google API 呼叫 (獲取原始結果)
            RestTemplate restTemplate = new RestTemplate();
            ObjectMapper mapper = new ObjectMapper();

            for (int i = 0; i < 2; i++) {
                int start = 1 + (i * 10);
                String urlString = "https://www.googleapis.com/customsearch/v1?key=" + apiKey + 
                                   "&cx=" + cx + "&num=10&q=" + q + "&start=" + start;
                
                java.net.URI uri = java.net.URI.create(urlString);
                System.out.println("V2 呼叫 API (第 " + (i+1) + " 頁): " + uri);
                
                try {
                    String jsonResp = restTemplate.getForObject(uri, String.class);
                    Map<String, Object> body = mapper.readValue(jsonResp, Map.class);
                    
                    if (body != null && body.containsKey("items")) {
                        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
                        for (Map<String, Object> item : items) {
                            String link = (String) item.get("link");
                            String title = (String) item.get("title");
                            String snippet = (String) item.get("snippet");

                            // 過濾垃圾連結
                            if (link != null && (link.contains("android.googlesource") || link.contains("github.com"))) {
                                continue; 
                            }

                            if (link != null && title != null) {
                                cleanResults.add(new SearchResult(title, link, snippet != null ? snippet : "", 50.0));
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Google API V2 錯誤: " + ex.getMessage());
                }
            }

            // 4. 主頁面爬蟲與 Boyer-Moore 計分
            List<Keyword> keywords = keywordRepository.findByLanguage(targetLangForKeywords);
            if (keywords.isEmpty()) keywords = keywordRepository.findByLanguage("en");
            
            BoyerMoore boyerMoore = new BoyerMoore();
            final List<Keyword> finalKeywords = keywords;

            cleanResults.parallelStream().forEach(res -> {
                try {
                    String content = htmlFetcher.fetchContent(res.getLink());
                    String searchContent = (content.isEmpty() ? res.getTitle() + " " + res.getSnippet() : content).toLowerCase();

                    for (Keyword k : finalKeywords) {
                        String pattern = k.getSearchTerm().toLowerCase();
                        int count = 0, idx = 0;
                        while ((idx = boyerMoore.search(searchContent, pattern, idx)) != -1) {
                            count++;
                            idx += pattern.length();
                        }
                        if (count > 0) {
                            synchronized (res) {
                                res.setScore(res.getScore() + (k.getWeight() * count));
                            }
                        }
                    }
                } catch (Exception e) {
                    // ignore crawler errors
                }
            });

            // 5. 執行深度排序 (Deep Ranking)
            // 修正點：使用正確的方法名稱 deepRankAsync 並接收回傳的 List
            List<SearchResult> refinedResults = deepRankingService.deepRankAsync(cleanResults, targetLangForKeywords);

            // 6. 封裝結果並回傳
            Map<String, Double> result = new LinkedHashMap<>();
            for (SearchResult r : refinedResults) { 
                result.put(r.getLink(), r.getScore());
            }
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }
}