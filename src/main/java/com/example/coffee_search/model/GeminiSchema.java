package com.example.coffee_search.model;

import java.util.List;

/**
 * 定義 Gemini API 請求與回應的 JSON 結構
 * 新增 system_instruction 欄位以支援系統級 Prompt
 */
public class GeminiSchema {

    // --- Request DTOs ---
    // [修正] 加入 system_instruction 欄位
    public record GeminiRequest(
        Content system_instruction, 
        List<Content> contents
    ) {}
    
    public record Content(List<Part> parts) {}
    
    public record Part(String text) {}

    // --- Response DTOs ---
    public record GeminiResponse(List<Candidate> candidates) {}
    
    public record Candidate(Content content) {}
    
    // [新增] 建立安全請求的輔助方法 (包含 System Prompt 與 User Prompt)
    public static GeminiRequest createSecureRequest(String systemText, String userText) {
        Content system = new Content(List.of(new Part(systemText)));
        Content user = new Content(List.of(new Part(userText)));
        
        // Gemini API 格式: system_instruction 是單一物件，contents 是陣列
        return new GeminiRequest(system, List.of(user));
    }
}