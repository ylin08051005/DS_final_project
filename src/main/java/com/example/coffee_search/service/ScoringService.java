package com.example.coffee_search.service;

import com.example.coffee_search.model.Keyword;
import com.example.coffee_search.model.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ScoringService {

    @Autowired private HtmlFetcher htmlFetcher;

    /**
     * 非同步計算單一頁面的分數
     * @Async("crawlerExecutor") 告訴 Spring 用我們剛剛設定的專用執行緒池跑這個方法
     */
    @Async("crawlerExecutor")
    public CompletableFuture<Void> scorePageAsync(SearchResult res, List<Keyword> keywords, BoyerMoore boyerMoore) {
        try {
            // 1. 爬取
            String content = htmlFetcher.fetchContent(res.getLink());
            String searchContent = (content.isEmpty() ? res.getTitle() + " " + res.getSnippet() : content).toLowerCase();

            // 2. 計分
            for (Keyword k : keywords) {
                String pattern = k.getSearchTerm().toLowerCase();
                int count = 0;
                int idx = 0;
                while ((idx = boyerMoore.search(searchContent, pattern, idx)) != -1) {
                    count++;
                    idx += pattern.length();
                }
                if (count > 0) {
                    // SearchResult 的修改必須同步，避免競態條件
                    synchronized (res) {
                        res.setScore(res.getScore() + (k.getWeight() * count));
                    }
                }
            }
        } catch (Exception e) {
            // 爬蟲失敗是常態，印出 Log 即可，不要拋出異常中斷流程
            // System.err.println("爬取失敗: " + res.getLink());
        }
        return CompletableFuture.completedFuture(null);
    }
}