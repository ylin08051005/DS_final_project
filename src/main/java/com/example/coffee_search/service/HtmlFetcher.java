package com.example.coffee_search.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class HtmlFetcher {

    public String fetchContent(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        try {
            // 使用 Jsoup 連線
            // timeout(5000) 設定 5 秒超時，避免卡住
            // get() 會自動解析 HTML 的 Content-Type 與 meta charset，自動轉成正確的編碼
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(5000) 
                    .get();

            // doc.text() 會移除所有 HTML標籤，只留下純文字
            // 這正是 Boyer-Moore 需要的輸入
            return doc.text();

        } catch (IOException e) {
            // 爬蟲經常會失敗 (403 Forbidden, 404, Timeout)，這是正常的
            // 印出錯誤以便除錯，但回傳空字串讓流程繼續
            System.err.println("爬取失敗 [" + url + "]: " + e.getMessage());
            return "";
        } catch (Exception e) {
            System.err.println("爬蟲未預期錯誤: " + e.getMessage());
            return "";
        }
    }
}