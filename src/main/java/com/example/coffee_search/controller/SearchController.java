package com.example.coffee_search.controller;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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
import com.example.coffee_search.service.HeapSorter;
import com.example.coffee_search.service.ScoringService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class SearchController {

    @Autowired private GeminiService geminiService;
    @Autowired private KeywordRepository keywordRepository;
    @Autowired private ScoringService scoringService;
    @Autowired private DeepRankingService deepRankingService;

    @Value("${google.cse.apiKey}") private String apiKey;
    @Value("${google.cse.cx}") private String cx;

    @GetMapping("/")
    public String index() { return "index"; }

    @GetMapping("/api/keywords")
    @ResponseBody
    public List<Keyword> getKeywords() { return keywordRepository.findAll(); }

    @PostMapping("/api/coffee_search")
    @ResponseBody
    public Map<String, Object> searchCoffee(
            @RequestParam("apiInput") String userQuery,
            @RequestParam(value = "searchType", defaultValue = "web") String searchType) { // 新增參數
        
        List<SearchResult> rawResults = new ArrayList<>();
        if (userQuery == null || userQuery.trim().isEmpty()) return new HashMap<>();

        try {
            System.out.println("[Start] Query: " + userQuery + " | Type: " + searchType);

            // 1. Gemini 分析 (語言偵測 + 翻譯)
            GeminiAnalysisResult analysis = geminiService.analyzeQuery(userQuery);
            String detectedLang = analysis.language();
            String finalSearchQuery = analysis.query();
            
            // 2. 決定關鍵字表與後綴
            String targetLangForKeywords = "en";
            if ("zh".equals(detectedLang)) {
                targetLangForKeywords = "zh";
                if (!finalSearchQuery.contains("咖啡")) finalSearchQuery += " 咖啡";
            } else if ("ja".equals(detectedLang)) {
                targetLangForKeywords = "ja";
                if (!finalSearchQuery.contains("コーヒー")) finalSearchQuery += " コーヒー";
            } else {
                targetLangForKeywords = "en";
                if (!finalSearchQuery.toLowerCase().contains("coffee")) finalSearchQuery += " coffee";
            }

            // 3. 呼叫 Google API (根據 searchType 調整參數)
            String q = URLEncoder.encode(finalSearchQuery, StandardCharsets.UTF_8);
            RestTemplate restTemplate = new RestTemplate();
            ObjectMapper mapper = new ObjectMapper();

            for (int i = 0; i < 2; i++) {
                int start = 1 + (i * 10);
                StringBuilder urlBuilder = new StringBuilder("https://www.googleapis.com/customsearch/v1?");
                urlBuilder.append("key=").append(apiKey);
                urlBuilder.append("&cx=").append(cx);
                urlBuilder.append("&num=10");
                urlBuilder.append("&q=").append(q);
                urlBuilder.append("&start=").append(start);

                // [分流邏輯]
                if ("image".equals(searchType)) {
                    urlBuilder.append("&searchType=image");
                } else if ("pdf".equals(searchType)) {
                    urlBuilder.append("&fileType=pdf");
                }

                URI uri = URI.create(urlBuilder.toString());
                
                try {
                    String jsonResp = restTemplate.getForObject(uri, String.class);
                    Map<String, Object> body = mapper.readValue(jsonResp, Map.class);
                    
                    if (body != null && body.containsKey("items")) {
                        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
                        for (Map<String, Object> item : items) {
                            String link = (String) item.get("link");
                            String title = (String) item.get("title");
                            String snippet = (String) item.get("snippet");

                            // 處理圖片特殊欄位
                            String thumbnailLink = null;
                            if ("image".equals(searchType) && item.containsKey("image")) {
                                Map<String, Object> imgData = (Map<String, Object>) item.get("image");
                                thumbnailLink = (String) imgData.get("thumbnailLink");
                            }

                            if (link != null && (link.contains("android.googlesource") || link.contains("github.com"))) continue;

                            if (link != null) {
                                if ("image".equals(searchType)) {
                                    // 圖片直接回傳，不計分
                                    rawResults.add(new SearchResult(title, link, thumbnailLink));
                                } else {
                                    // Web 或 PDF 初始分數
                                    rawResults.add(new SearchResult(title, link, snippet != null ? snippet : "", 50.0));
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("[GoogleAPI] Error: " + ex.getMessage());
                }
            }

            // [快速通道] 如果是圖片搜尋，直接回傳，不做爬蟲
            if ("image".equals(searchType)) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("type", "image");
                result.put("data", rawResults);
                return result;
            }

            // 準備關鍵字
            List<Keyword> keywords = keywordRepository.findByLanguage(targetLangForKeywords);
            if (keywords.isEmpty()) keywords = keywordRepository.findByLanguage("en");
            final List<Keyword> finalKeywords = keywords;
            BoyerMoore boyerMoore = new BoyerMoore();

            // [分流通道] 如果是 PDF，只做簡單計分 (不爬蟲，因為 Jsoup 爬不了 PDF)
            if ("pdf".equals(searchType)) {
                System.out.println("🚀 處理 PDF 計分 (僅標題/摘要)...");
                for (SearchResult res : rawResults) {
                    String contentToCheck = (res.getTitle() + " " + res.getSnippet()).toLowerCase();
                    for (Keyword k : finalKeywords) {
                        String pattern = k.getSearchTerm().toLowerCase();
                        int count = 0, idx = 0;
                        while ((idx = boyerMoore.search(contentToCheck, pattern, idx)) != -1) {
                            count++; idx += pattern.length();
                        }
                        if (count > 0) res.setScore(res.getScore() + (k.getWeight() * count));
                    }
                }
            } 
            // [標準通道] 如果是 Web，執行完整爬蟲與深度排序
            else {
                System.out.println("🚀 啟動完整 Web 爬蟲與深度排序...");
                // 1. 主頁面爬蟲
                List<CompletableFuture<Void>> futures = rawResults.stream()
                    .map(res -> scoringService.scorePageAsync(res, finalKeywords, boyerMoore))
                    .collect(Collectors.toList());
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                // 2. 深度排序
                rawResults = deepRankingService.deepRankAsync(rawResults, targetLangForKeywords);
            }

            // 排序與輸出
            HeapSorter sorter = new HeapSorter();
            for (SearchResult result : rawResults) {
                sorter.insert(result);
            }

            // 為了前端好處理，回傳結構化資料
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("type", searchType);
            
            // 轉成 List 方便前端處理 (Map 會打亂順序，這裡建議改用 List 回傳給前端，但為了相容舊邏輯，我們包裝一下)
            // 這裡我將 SearchResult 物件直接回傳，這樣前端可以拿到 title, snippet, score
            List<SearchResult> sortedList = sorter.getSortedList();
            response.put("data", sortedList);

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }
}