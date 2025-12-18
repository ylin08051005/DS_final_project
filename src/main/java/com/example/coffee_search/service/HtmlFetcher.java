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

    // [修正] 設定為 0 代表無限大 (Unlimited)，確保下載完整網頁
    private static final int MAX_BODY_SIZE = 0; 
    
    // [修正] 延長至 5 秒，給予下載大檔案緩衝時間
    private static final int TIMEOUT_MS = 5000;

    /**
     * 抓取網頁純文字內容
     */
    public String fetchContent(String url) {
        if (url == null || url.isEmpty()) return "";
        try {
            Document doc = Jsoup.connect(url)
                    // 偽裝成一般瀏覽器
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(TIMEOUT_MS)           
                    .maxBodySize(MAX_BODY_SIZE)    // 設定為無限，確保不截斷 HTML
                    .get();

            // 取得完整純文字內容
            String text = doc.text();
            
            // [修正] 移除文字長度截斷邏輯
            // 不再使用 substring，確保文章後段的關鍵字也能被計分
            return text;

        } catch (Exception e) {
            // 爬蟲失敗回傳空字串
            return "";
        }
    }

    /**
     * 提取網頁中的有效子連結 (Deep Ranking 用)
     */
    public Set<String> extractLinks(String url) {
        Set<String> validLinks = new HashSet<>();
        if (url == null || url.isEmpty()) return validLinks;

        try {
            URI baseUri = new URI(url);
            String host = baseUri.getHost();

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(TIMEOUT_MS)
                    .maxBodySize(MAX_BODY_SIZE) // 同樣取消限制
                    .get();

            Elements links = doc.select("a[href]");

            for (Element link : links) {
                String absUrl = link.attr("abs:href");

                if (!absUrl.isEmpty() 
                    && absUrl.startsWith("http") 
                    && absUrl.contains(host) 
                    && !absUrl.equals(url) 
                    && !isFile(absUrl)) {
                    
                    validLinks.add(absUrl);
                }

                // Deep Ranking 的連結數量限制還是保留一下 (例如 5~10 個)
                // 這是為了避免 DeepRankingService 跑太久，跟網頁內容完整性無關
                if (validLinks.size() >= 10) break;
            }

        } catch (Exception e) {
            // ignore
        }
        return validLinks;
    }

    private boolean isFile(String url) {
        String lower = url.toLowerCase();
        return lower.endsWith(".pdf") || lower.endsWith(".jpg") || lower.endsWith(".png") 
            || lower.endsWith(".xml") || lower.endsWith(".zip") || lower.endsWith(".csv")
            || lower.endsWith(".css") || lower.endsWith(".js");
    }
}