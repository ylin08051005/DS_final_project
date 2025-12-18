package com.example.coffee_search.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

@Service
public class HtmlFetcher {

    // 取得網頁純文字內容
    public String fetchContent(String url) {
        if (url == null || url.isEmpty()) return "";
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(3000) // 縮短 timeout 避免深度搜尋卡太久
                    .get();
            return doc.text();
        } catch (Exception e) {
            // 靜默失敗，避免 log 洗版
            return "";
        }
    }

    /**
     * [新增] 提取網頁中的有效子連結
     * 規則：
     * 1. 只抓取同一網域 (Domain) 的連結
     * 2. 排除 PDF, JPG 等非網頁檔案
     * 3. 最多回傳 5 個連結 (避免爬蟲過勞)
     */
    public Set<String> extractLinks(String url) {
        Set<String> validLinks = new HashSet<>();
        if (url == null || url.isEmpty()) return validLinks;

        try {
            // 1. 解析原始 URL 的 Domain (用來判斷是否為站內連結)
            URI baseUri = new URI(url);
            String host = baseUri.getHost();

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(3000)
                    .get();

            // 2. 抓取所有 <a> 標籤
            Elements links = doc.select("a[href]");

            for (Element link : links) {
                // abs:href 會自動處理相對路徑 (如 /about -> https://site.com/about)
                String absUrl = link.attr("abs:href");

                // 3. 過濾邏輯
                if (!absUrl.isEmpty() 
                    && absUrl.startsWith("http") 
                    && absUrl.contains(host) // 限制在同網域
                    && !absUrl.equals(url)   // 排除自己
                    && !isFile(absUrl)) {    // 排除圖片檔案等
                    
                    validLinks.add(absUrl);
                }

                // 4. 硬性限制：每個網頁最多只抓 5 個子連結
                if (validLinks.size() >= 5) break;
            }

        } catch (Exception e) {
            // System.err.println("提取連結失敗: " + url);
        }
        return validLinks;
    }

    // 簡單判斷是否為非 HTML 檔案
    private boolean isFile(String url) {
        String lower = url.toLowerCase();
        return lower.endsWith(".pdf") || lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".xml");
    }
}