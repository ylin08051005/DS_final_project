package com.example.coffee_search.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

@Service
public class HtmlFetcher {

    // [優化設定] 平衡速度與完整性
    // 1. 下載限制：300KB (足夠容納大部分深度長文，但排除過大的檔案)
    private static final int MAX_BODY_SIZE = 300 * 1024; 
    
    // 2. 連線超時：3.5 秒 (給予優質但回應稍慢的網站機會，同時避免嚴重卡頓)
    private static final int TIMEOUT_MS = 3500;
    
    // 3. 分析長度：10000 字 (配合 300KB 的下載量，增加文字分析的涵蓋範圍)
    private static final int MAX_TEXT_LENGTH = 10000;

    /**
     * 抓取網頁純文字內容
     */
    public String fetchContent(String url) {
        if (url == null || url.isEmpty()) return "";
        try {
            Document doc = Jsoup.connect(url)
                    // 偽裝成一般瀏覽器，減少被擋機率
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(TIMEOUT_MS)           // 設定 2.5 秒
                    .maxBodySize(MAX_BODY_SIZE)    // 設定 300 KB
                    .get();

            String text = doc.text();
            
            // 文字截斷：避免 Boyer-Moore 對超長字串運算過久
            if (text.length() > MAX_TEXT_LENGTH) {
                return text.substring(0, MAX_TEXT_LENGTH);
            }
            return text;

        } catch (Exception e) {
            // 爬蟲失敗回傳空字串，不影響主流程
            return "";
        }
    }

    /**
     * 提取網頁中的有效子連結 (用於深度排序)
     */
    public Set<String> extractLinks(String url) {
        Set<String> validLinks = new HashSet<>();
        if (url == null || url.isEmpty()) return validLinks;

        try {
            // 解析原始 URL 的 Domain (用來判斷是否為站內連結)
            URI baseUri = new URI(url);
            String host = baseUri.getHost();

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(TIMEOUT_MS)           // 套用同樣的設定
                    .maxBodySize(MAX_BODY_SIZE)    // 套用同樣的設定
                    .get();

            Elements links = doc.select("a[href]");

            for (Element link : links) {
                String absUrl = link.attr("abs:href");

                // 連結過濾邏輯：
                // 1. 必須是 http/https
                // 2. 必須是同網域 (站內連結)
                // 3. 排除自己
                // 4. 排除 PDF/JPG 等非網頁檔案
                if (!absUrl.isEmpty() 
                    && absUrl.startsWith("http") 
                    && absUrl.contains(host) 
                    && !absUrl.equals(url) 
                    && !isFile(absUrl)) {
                    
                    validLinks.add(absUrl);
                }

                // 硬性限制：每個網頁最多只抓 5 個子連結，避免 Deep Ranking 耗時過久
                if (validLinks.size() >= 5) break;
            }

        } catch (Exception e) {
            // ignore
        }
        return validLinks;
    }

    // 判斷是否為非 HTML 檔案
    private boolean isFile(String url) {
        String lower = url.toLowerCase();
        return lower.endsWith(".pdf") || lower.endsWith(".jpg") || lower.endsWith(".png") 
            || lower.endsWith(".xml") || lower.endsWith(".zip") || lower.endsWith(".csv");
    }
}