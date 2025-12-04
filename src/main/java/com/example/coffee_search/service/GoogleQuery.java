package com.example.coffee_search.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI; // 新增
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class GoogleQuery {
    public String searchKeyword;
    public String url;
    public String content;

    public GoogleQuery(String searchKeyword) {
        this.searchKeyword = searchKeyword;
        try {
            // 修正編碼寫法
            String encodeKeyword = URLEncoder.encode(searchKeyword, StandardCharsets.UTF_8);
            this.url = "https://www.google.com/search?q=" + encodeKeyword + "&oe=utf8&num=20";
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private String fetchContent() throws IOException {
        StringBuilder retVal = new StringBuilder(); // 優化字串串接

        // 修正 new URL() 過時問題
        URL u = URI.create(url).toURL();
        
        URLConnection conn = u.openConnection();
        conn.setRequestProperty("User-agent", "Chrome/107.0.5304.107");
        
        try (InputStream in = conn.getInputStream();
             InputStreamReader inReader = new InputStreamReader(in, StandardCharsets.UTF_8);
             BufferedReader bufReader = new BufferedReader(inReader)) {
            
            String line;
            while ((line = bufReader.readLine()) != null) {
                retVal.append(line);
            }
        }
        return retVal.toString();
    }

    public HashMap<String, String> query() throws IOException {
        if (content == null) {
            content = fetchContent();
        }

        HashMap<String, String> retVal = new HashMap<>(); // 使用鑽石運算子

        Document doc = Jsoup.parse(content);
        Elements lis = doc.select("div.kCrYT"); // 稍微簡化 select 寫法

        for (Element li : lis) {
            try {
                // 加強空值檢查，避免 IndexOutOfBoundsException
                Element aTag = li.select("a").first();
                if (aTag != null) {
                    String citeUrl = aTag.attr("href").replace("/url?q=", "");
                    String title = aTag.select(".vvjwJb").text();

                    if (!title.isEmpty()) {
                        System.out.println("Title: " + title + " , url: " + citeUrl);
                        retVal.put(title, citeUrl);
                    }
                }
            } catch (Exception e) {
                // 忽略解析錯誤的項目
            }
        }
        return retVal;
    }
}