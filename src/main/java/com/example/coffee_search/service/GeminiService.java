package com.example.coffee_search.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.example.coffee_search.model.GeminiSchema;
import com.example.coffee_search.model.GeminiSchema.GeminiRequest;
import com.example.coffee_search.model.GeminiSchema.GeminiResponse;
import com.example.coffee_search.model.GeminiAnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiService {

    @Value("${google.gemini.apiKey}")
    private String apiKey;

    @Value("${google.gemini.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper; // 用於解析 JSON 回應

    public GeminiService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 呼叫 Gemini 進行語言偵測與必要的翻譯
     * @param userQuery 使用者原始輸入
     * @return GeminiAnalysisResult (包含 language 和 query)
     */
    public GeminiAnalysisResult analyzeQuery(String userQuery) {
        // 預設值 (若 API 失敗時的兜底方案)
        GeminiAnalysisResult fallback = new GeminiAnalysisResult("en", userQuery);

        if (userQuery == null || userQuery.trim().isEmpty()) {
            return fallback;
        }

        try {
            // 1. 精心設計的 Prompt (提示詞)
            // 要求 Gemini 判斷語言並回傳 JSON 格式
            String prompt = """
                You are a search query pre-processor.
                Task:
                1. Detect the language of the input query.
                2. If the language is Chinese (Traditional or Simplified), return code "zh".
                3. If the language is Japanese, return code "ja".
                4. If the language is English, return code "en".
                5. If it is any other language, translate the query into English and return code "en".
                6. For "zh", "ja", and "en", return the original query text.
                
                Output Requirement:
                Return ONLY a JSON object. Do not use Markdown code blocks.
                Format: {"language": "code", "query": "text"}
                
                Input: %s
                """.formatted(userQuery);

            GeminiRequest requestBody = GeminiSchema.createRequest(prompt);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<GeminiRequest> entity = new HttpEntity<>(requestBody, headers);

            String finalUrl = apiUrl + "?key=" + apiKey;
            
            // 2. 發送請求
            GeminiResponse response = restTemplate.postForObject(finalUrl, entity, GeminiResponse.class);

            // 3. 解析回應
            if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                String rawText = response.candidates().get(0).content().parts().get(0).text();
                
                // 清理可能殘留的 Markdown 標記 (例如 ```json ... ```)
                String jsonText = cleanJsonString(rawText);
                
                // 將 JSON 字串轉為 Java 物件
                return objectMapper.readValue(jsonText, GeminiAnalysisResult.class);
            }

        } catch (Exception e) {
            System.err.println("Gemini 分析失敗: " + e.getMessage());
            e.printStackTrace();
        }

        return fallback;
    }

    // 輔助方法：清理 Markdown 標記
    private String cleanJsonString(String text) {
        text = text.trim();
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
    }
}