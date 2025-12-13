package com.example.coffee_search.model;

/**
 * 用於接收 Gemini 分析後的結構化結果
 * language: "zh", "ja", "en"
 * query: 處理後 (或翻譯後) 的查詢字串
 */
public record GeminiAnalysisResult(String language, String query) {}
