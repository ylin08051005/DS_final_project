package com.example.coffee_search.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

@Service
public class HtmlFetcher {

    // 方法 1: 抓取純文字 (舊有的)
    public String fetchContent(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(5000)
                    .get();

            return doc.text();
        } catch (IOException e) {
            System.err.println("爬取失敗 [" + url + "]: " + e.getMessage());
            return "";
        } catch (Exception e) {
            System.err.println("爬蟲未預期錯誤: " + e.getMessage());
            return "";
        }
    }

    /**
     * 方法 2: [這是原本缺少的] 解析網頁並抓取內部的有效連結
     */
    public Set<String> extractLinks(String url) {
        Set<String> links = new HashSet<>();
        if (url == null || url.isEmpty()) return links;

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(3000)
                    .get();

            Elements elements = doc.select("a[href]");
            String host = URI.create(url).getHost();

            for (Element e : elements) {
                String absUrl = e.attr("abs:href");
                if (absUrl.startsWith("http") && host != null && absUrl.contains(host)) {
                    links.add(absUrl);
                }
                if (links.size() >= 5) break;
            }
        } catch (Exception e) {
            // ignore
        }
        return links;
    }
}