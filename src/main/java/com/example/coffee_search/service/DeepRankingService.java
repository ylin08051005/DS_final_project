package com.example.coffee_search.service;

import com.example.coffee_search.model.Keyword;
import com.example.coffee_search.model.SearchResult;
import com.example.coffee_search.repository.KeywordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class DeepRankingService {

    @Autowired private HtmlFetcher htmlFetcher;
    @Autowired private KeywordRepository keywordRepository;

    /**
     * 非同步深度排序入口
     * 為每個搜尋結果啟動一個非同步任務去分析子頁面
     */
    public List<SearchResult> deepRankAsync(List<SearchResult> results, String language) {
        System.out.println("🚀 啟動非同步深度重排序 (Deep Ranking)...");

        List<Keyword> keywords = keywordRepository.findByLanguage(language);
        if (keywords.isEmpty()) keywords = keywordRepository.findByLanguage("en");
        
        final List<Keyword> finalKeywords = keywords;
        final BoyerMoore boyerMoore = new BoyerMoore();

        // 1. 將每個主結果映射為一個 CompletableFuture 任務
        List<CompletableFuture<Void>> futures = results.stream()
            .map(res -> processSubPagesAsync(res, finalKeywords, boyerMoore))
            .collect(Collectors.toList());

        // 2. 等待所有深度爬蟲任務完成 (Non-blocking 等待)
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 3. 所有分數更新完畢，重新排序
        HeapSorter sorter = new HeapSorter();
        results.forEach(sorter::insert);

        return sorter.getSortedList();
    }

    /**
     * 內部方法：處理單一主結果的子頁面分析
     * @Async("crawlerExecutor") 確保使用專用執行緒池，不佔用主執行緒
     */
    @Async("crawlerExecutor")
    public CompletableFuture<Void> processSubPagesAsync(SearchResult res, List<Keyword> keywords, BoyerMoore boyerMoore) {
        try {
            // 1. 提取子連結 (HtmlFetcher 已限制最多 5 個)
            Set<String> subLinks = htmlFetcher.extractLinks(res.getLink());
            
            double subPageScoreTotal = 0.0;
            int processedCount = 0;

            for (String subLink : subLinks) {
                // [安全閥] 每個主結果最多只爬 3 個子頁面 (兼顧深度與速度)
                if (processedCount >= 3) break; 
                
                // 爬取子頁面內容
                String content = htmlFetcher.fetchContent(subLink);
                if (content.isEmpty()) continue;
                
                String searchContent = content.toLowerCase();
                processedCount++;

                // 計算關鍵字分數
                for (Keyword k : keywords) {
                    String pattern = k.getSearchTerm().toLowerCase();
                    int count = 0;
                    int idx = 0;
                    while ((idx = boyerMoore.search(searchContent, pattern, idx)) != -1) {
                        count++;
                        idx += pattern.length();
                    }
                    if (count > 0) {
                        subPageScoreTotal += (k.getWeight() * count);
                    }
                }
            }

            // 2. 將子頁面總分加權 (30%) 後併入主結果
            if (subPageScoreTotal > 0) {
                double bonus = subPageScoreTotal * 0.3;
                
                // 這裡需要同步鎖，因為 SearchResult 可能同時被讀取
                synchronized (res) {
                    res.setScore(res.getScore() + bonus);
                    // System.out.println("  [DeepRank] 加分: " + String.format("%.2f", bonus) + " -> " + res.getTitle());
                }
            }
        } catch (Exception e) {
            // 子頁面爬取失敗不應影響主流程
        }
        return CompletableFuture.completedFuture(null);
    }
}