package com.example.coffee_search.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class HtmlFetcher {

    public String fetchContent(String url) {
        try {
            // 使用 Jsoup 連線
            Document doc = Jsoup.connect(url)
                    // 1. 設定更完整的 User-Agent (偽裝成最新的 Chrome)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    // 2. 設定 Referrer (假裝是從 Google 搜尋點進去的)
                    .referrer("http://www.google.com")
                    // 3. 設定逾時為 20 秒 (原本 5 秒太短容易 timeout)
                    .timeout(20000)
                    // 4. 即使遇到 404/500/403 錯誤也不要拋出例外，嘗試讀取頁面文字
                    .ignoreHttpErrors(true)
                    // 5. 允許重新導向
                    .followRedirects(true)
                    .get();
            
            // doc.text() 會自動移除 HTML標籤，只留下可閱讀的內文
            return doc.text();
            
        } catch (IOException e) {
            // 若抓取失敗 (例如 404 或連線逾時)，回傳空字串，不影響流程
            System.err.println("無法抓取網頁內容: " + url + " (" + e.getMessage() + ")");
            return "";
        }
    }
}