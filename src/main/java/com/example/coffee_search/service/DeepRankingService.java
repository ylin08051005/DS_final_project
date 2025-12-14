package com.example.coffee_search.service;

import com.example.coffee_search.model.Keyword;
import com.example.coffee_search.model.SearchResult;
import com.example.coffee_search.repository.KeywordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class DeepRankingService {

    @Autowired private HtmlFetcher htmlFetcher;
    @Autowired private KeywordRepository keywordRepository;

    /**
     * 執行深度重排序：爬取每個結果的子頁面，計算分數並加權至主結果，然後重新排序。
     * @param originalResults Google 初步搜尋結果 (分數已包含主頁面計分)
     * @param language 目標語言 ("en", "zh", "ja") 用於選取關鍵字
     * @return 重新計分並排序後的結果列表 (使用 Heap Sort)
     */
    public List<SearchResult> deepRank(List<SearchResult> originalResults, String language) {
        System.out.println("🚀 啟動深度重排序 (Deep Ranking)...");

        // 1. 準備關鍵字與演算法工具
        List<Keyword> keywords = keywordRepository.findByLanguage(language);
        if (keywords.isEmpty()) keywords = keywordRepository.findByLanguage("en");
        
        final List<Keyword> finalKeywords = keywords;
        BoyerMoore boyerMoore = new BoyerMoore();

        // 2. 平行處理每個搜尋結果 (使用 parallelStream 加速爬蟲)
        originalResults.parallelStream().forEach(result -> {
            try {
                // A. 抓取子連結
                Set<String> subLinks = htmlFetcher.extractLinks(result.getLink());
                
                // B. 累計子頁面分數
                double subPageScoreTotal = 0.0;

                for (String subLink : subLinks) {
                    String content = htmlFetcher.fetchContent(subLink);
                    if (content.isEmpty()) continue;
                    
                    String searchContent = content.toLowerCase();
                    
                    // 計算該子頁面的關鍵字分數 (使用 Boyer-Moore)
                    for (Keyword k : finalKeywords) {
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

                // C. 將子頁面分數加權後，併入主結果分數
                // 採用 0.3 的加權分數，避免子頁面分數過高，但仍提供獎勵
                if (subPageScoreTotal > 0) {
                    double bonus = subPageScoreTotal * 0.3;
                    synchronized (result) {
                        double oldScore = result.getScore();
                        result.setScore(oldScore + bonus);
                        System.out.println("📈 加分: [" + result.getTitle().substring(0, Math.min(10, result.getTitle().length())) + "...] 原分:" + oldScore + " + 子頁加權:" + String.format("%.2f", bonus));
                    }
                }

            } catch (Exception e) {
                System.err.println("重排序單項失敗: " + e.getMessage());
            }
        });

        // 3. 使用 HeapSorter 重新排序 (Data Structure 應用)
        HeapSorter sorter = new HeapSorter();
        for (SearchResult res : originalResults) {
            sorter.insert(res);
        }

        // 回傳排序好的 List
        return sorter.getSortedList();
    }
}