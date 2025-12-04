package com.example.coffee_search.service;

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
     * 將輸入文字翻譯為英文
     * @param text 原始文字
     * @return 翻譯後的英文文字，若失敗則回傳原文
     */
    public String translateToEnglish(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            // 構建請求 URL: 來源自動偵測，目標設為英文 (en)
            String url = TRANSLATE_API_URL + "?q=" + text + "&target=en&key=" + apiKey;

            // 發送請求
            String response = restTemplate.getForObject(url, String.class);
            
            // 解析 JSON 取出翻譯結果
            // 回傳結構通常為: { "data": { "translations": [ { "translatedText": "..." } ] } }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            
            if (root.has("data") && root.get("data").has("translations")) {
                JsonNode translations = root.get("data").get("translations");
                if (translations.isArray() && translations.size() > 0) {
                    return translations.get(0).get("translatedText").asText();
                }
            }
        } catch (Exception e) {
            // 簡單的錯誤處理：若 API 呼叫失敗 (例如 Quota 超過或 Key 無效)，印出 Log 並回傳原文
            System.err.println("Translation API failed: " + e.getMessage());
            return text;
        }
        return text;
    }
}