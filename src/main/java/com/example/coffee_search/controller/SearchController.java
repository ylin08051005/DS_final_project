package com.example.coffee_search.controller;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
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

    /**
     * [SSE 串流搜尋接口]
     * 回傳 SseEmitter，讓瀏覽器保持連線，接收即時更新
     */
    @GetMapping(value = "/api/stream-search", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSearch(
            @RequestParam("apiInput") String userQuery,
            @RequestParam(value = "searchType", defaultValue = "web") String searchType) {

        // 設定超時時間為 2 分鐘 (足夠爬蟲跑完)
        SseEmitter emitter = new SseEmitter(120_000L);

        // 開啟一個背景執行緒來處理搜尋，讓 Controller 可以立刻回傳 emitter
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("🚀 [SSE] 啟動串流搜尋: " + userQuery);
                
                // 1. Gemini 分析
                GeminiAnalysisResult analysis = geminiService.analyzeQuery(userQuery);
                String finalSearchQuery = analysis.query();
                String detectedLang = analysis.language();
                
                // 決定關鍵字
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

                // 2. Google API 呼叫
                String q = URLEncoder.encode(finalSearchQuery, StandardCharsets.UTF_8);
                RestTemplate restTemplate = new RestTemplate();
                ObjectMapper mapper = new ObjectMapper();
                List<SearchResult> rawResults = new ArrayList<>();

                for (int i = 0; i < 2; i++) {
                    int start = 1 + (i * 10);
                    StringBuilder urlBuilder = new StringBuilder("https://www.googleapis.com/customsearch/v1?");
                    urlBuilder.append("key=").append(apiKey).append("&cx=").append(cx)
                              .append("&num=10").append("&q=").append(q).append("&start=").append(start);

                    if ("image".equals(searchType)) urlBuilder.append("&searchType=image");
                    else if ("pdf".equals(searchType)) urlBuilder.append("&fileType=pdf");

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
                                String thumbnail = null;
                                
                                if ("image".equals(searchType) && item.containsKey("image")) {
                                    Map<String, Object> imgData = (Map<String, Object>) item.get("image");
                                    thumbnail = (String) imgData.get("thumbnailLink");
                                }

                                if (link != null && (link.contains("android.googlesource") || link.contains("github.com"))) continue;

                                if (link != null) {
                                    if ("image".equals(searchType)) {
                                        rawResults.add(new SearchResult(title, link, thumbnail));
                                    } else {
                                        rawResults.add(new SearchResult(title, link, snippet != null ? snippet : "", 50.0));
                                    }
                                }
                            }
                        }
                    } catch (Exception e) { System.err.println("Google API Error: " + e.getMessage()); }
                }

                if (rawResults.isEmpty()) {
                    emitter.send(SseEmitter.event().name("error").data("找不到相關結果"));
                    emitter.complete();
                    return;
                }

                // [SSE 事件 1] init: 傳送初步名單給前端渲染 (此時還沒爬蟲)
                emitter.send(SseEmitter.event().name("init").data(rawResults));

                // 如果是圖片或PDF，不做深度爬蟲，直接結束
                if ("image".equals(searchType) || "pdf".equals(searchType)) {
                    emitter.send(SseEmitter.event().name("complete").data("done"));
                    emitter.complete();
                    return;
                }

                // 3. 執行爬蟲與計分
                List<Keyword> keywords = keywordRepository.findByLanguage(targetLangForKeywords);
                if (keywords.isEmpty()) keywords = keywordRepository.findByLanguage("en");
                final List<Keyword> finalKeywords = keywords;
                BoyerMoore boyerMoore = new BoyerMoore();

                // 使用 CompletableFuture 並行爬取
                List<CompletableFuture<Void>> futures = rawResults.stream()
                    .map(res -> scoringService.scorePageAsync(res, finalKeywords, boyerMoore)
                        .thenRun(() -> {
                            // [SSE 事件 2] update: 每爬完一個網頁，推送更新後的分數
                            try {
                                synchronized (emitter) {
                                    // 傳送：連結 + 新分數
                                    Map<String, Object> updateData = new HashMap<>();
                                    updateData.put("link", res.getLink());
                                    updateData.put("score", res.getScore());
                                    emitter.send(SseEmitter.event().name("update").data(updateData));
                                }
                            } catch (Exception e) {
                                // 忽略傳送失敗 (可能是前端斷線)
                            }
                        }))
                    .collect(Collectors.toList());

                // 等待所有主頁面爬蟲完成
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                // 4. 深度排序 (Deep Ranking)
                // 這裡我們不即時推送子頁面分數，以免太過頻繁，而是一次性做完後推送最終結果
                List<SearchResult> finalResults = deepRankingService.deepRankAsync(rawResults, targetLangForKeywords);

                // [SSE 事件 3] final: 推送最終排序結果
                emitter.send(SseEmitter.event().name("final").data(finalResults));
                
                // 結束連線
                emitter.complete();

            } catch (Exception ex) {
                ex.printStackTrace();
                try {
                    emitter.send(SseEmitter.event().name("error").data("系統發生錯誤: " + ex.getMessage()));
                    emitter.completeWithError(ex);
                } catch (Exception e) {}
            }
        });

        return emitter;
    }
}