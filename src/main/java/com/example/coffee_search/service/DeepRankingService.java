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
     * 非同步處理深度排序
     * 為每個搜尋結果啟動一個非同步任務去抓子頁面
     */
    public List<SearchResult> deepRankAsync(List<SearchResult> results, String language) {
        System.out.println("🚀 啟動非同步深度重排序...");

        List<Keyword> keywords = keywordRepository.findByLanguage(language);
        if (keywords.isEmpty()) keywords = keywordRepository.findByLanguage("en");
        
        final List<Keyword> finalKeywords = keywords;
        final BoyerMoore boyerMoore = new BoyerMoore();

        // 1. 為每個結果建立一個非同步任務
        List<CompletableFuture<Void>> futures = results.stream()
            .map(res -> processSubPagesAsync(res, finalKeywords, boyerMoore))
            .collect(Collectors.toList());

        // 2. 等待所有任務完成 (join)
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 3. 排序
        HeapSorter sorter = new HeapSorter();
        results.forEach(sorter::insert);

        return sorter.getSortedList();
    }

    // 內部方法：使用 crawlerExecutor 執行緒池來跑子頁面爬蟲
    @Async("crawlerExecutor")
    public CompletableFuture<Void> processSubPagesAsync(SearchResult res, List<Keyword> keywords, BoyerMoore boyerMoore) {
        try {
            Set<String> subLinks = htmlFetcher.extractLinks(res.getLink());
            double subPageScoreTotal = 0.0;
            int processedCount = 0;

            for (String subLink : subLinks) {
                if (processedCount >= 3) break; // 限制爬 3 個子頁面

                String content = htmlFetcher.fetchContent(subLink);
                if (content.isEmpty()) continue;
                
                String searchContent = content.toLowerCase();
                processedCount++;

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

            if (subPageScoreTotal > 0) {
                double bonus = subPageScoreTotal * 0.3;
                synchronized (res) {
                    res.setScore(res.getScore() + bonus);
                }
            }
        } catch (Exception e) {
            // ignore error
        }
        return CompletableFuture.completedFuture(null);
    }
}