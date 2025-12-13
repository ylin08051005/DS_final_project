package com.example.coffee_search.model;

import java.util.List;

/**
 * 這裡定義 Gemini API 請求與回應的 JSON 結構
 * 使用 Java 17 record 功能，自動產生 Getter/Constructor
 */
public class GeminiSchema {

    // --- Request DTOs ---
    public record GeminiRequest(List<Content> contents) {}
    
    public record Content(List<Part> parts) {}
    
    public record Part(String text) {}

    // --- Response DTOs ---
    public record GeminiResponse(List<Candidate> candidates) {}
    
    public record Candidate(Content content) {}
    
    // 為了方便建構 Request 的輔助方法
    public static GeminiRequest createRequest(String prompt) {
        return new GeminiRequest(
            List.of(new Content(
                List.of(new Part(prompt))
            ))
        );
    }
}