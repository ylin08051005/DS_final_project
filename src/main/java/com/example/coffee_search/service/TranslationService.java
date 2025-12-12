package com.example.coffee_search.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TranslationService {

    @Value("${google.cse.apiKey}")
    private String apiKey;

    // Google Translation API V2 Endpoint
    private final String TRANSLATE_API_URL = "https://translation.googleapis.com/language/translate/v2";

    /**
     * 偵測輸入文字的語言
     * @param text 原始文字
     * @return 偵測到的語言代碼 (例如 "zh-TW", "en")，若失敗則回傳 "en"
     */
    public String detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "en";
        }
        try {
            // 透過翻譯 API 的副作用來取得 detectedSourceLanguage
            JsonNode firstTranslation = fetchTranslationNode(text, "en");
            if (firstTranslation != null && firstTranslation.has("detectedSourceLanguage")) {
                String detectedLang = firstTranslation.get("detectedSourceLanguage").asText();
                
                // [修正] 通用處理所有 -Latn 結尾的語言代碼
                // 例如: bg-Latn -> bg, ar-Latn -> ar
                // 避免 "Bad language pair" 錯誤
                if (detectedLang != null && detectedLang.endsWith("-Latn")) {
                    return detectedLang.split("-")[0];
                }
                
                return detectedLang;
            }
        } catch (Exception e) {
            System.out.println("Detect language failed (fallback to en): " + e.getMessage());
        }
        return "en"; // 預設回傳英文
    }

    /**
     * 將輸入文字翻譯為指定語言
     * @param text 原始文字
     * @param targetLang 目標語言代碼 (例如 "zh-TW")
     * @return 翻譯後的文字
     */
    public String translate(String text, String targetLang) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        try {
            JsonNode firstTranslation = fetchTranslationNode(text, targetLang);
            if (firstTranslation != null && firstTranslation.has("translatedText")) {
                return firstTranslation.get("translatedText").asText();
            }
        } catch (Exception e) {
            System.err.println("Translation failed: " + e.getMessage());
        }
        return text;
    }

    // 輔助方法：發送 API 請求並解析 JSON
    private JsonNode fetchTranslationNode(String text, String targetLang) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        
        // [修正] 使用 URLEncoder 對參數進行編碼，解決中文和空白造成的亂碼/錯誤問題
        // 這是最關鍵的一步，如果沒有編碼，含有特殊字元的請求會直接失敗
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String encodedTargetLang = URLEncoder.encode(targetLang, StandardCharsets.UTF_8);

        // 組合 URL
        String url = TRANSLATE_API_URL + "?q=" + encodedText + "&target=" + encodedTargetLang + "&key=" + apiKey;

        // 發送請求
        String response = restTemplate.getForObject(url, String.class);
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);

        if (root.has("data") && root.get("data").has("translations")) {
            JsonNode translations = root.get("data").get("translations");
            if (translations.isArray() && translations.size() > 0) {
                return translations.get(0);
            }
        }
        return null;
    }
}