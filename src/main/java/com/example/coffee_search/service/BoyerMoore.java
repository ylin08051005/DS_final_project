package com.example.coffee_search.service;

import java.util.HashMap;
import java.util.Map;

public class BoyerMoore {
    
    // 搜尋模式字串在文本中出現的位置，若沒找到回傳 -1
    public int search(String text, String pattern) {
        if (pattern == null || pattern.length() == 0) return -1;
        if (text == null || text.length() == 0) return -1;

        int m = pattern.length();
        int n = text.length();

        Map<Character, Integer> badCharTable = buildBadCharTable(pattern);

        int s = 0; // s 是 pattern 相對於 text 的位移量 (shift)
        
        while (s <= (n - m)) {
            int j = m - 1;

            // 從後往前比對
            while (j >= 0 && pattern.charAt(j) == text.charAt(s + j)) {
                j--;
            }

            if (j < 0) {
                // 找到匹配！回傳起始索引
                return s;
                // 若要找下一個出現位置: s += (s + m < n) ? m - badCharTable.getOrDefault(text.charAt(s + m), -1) : 1;
            } else {
                // 未匹配，根據壞字元規則移動
                char badChar = text.charAt(s + j);
                // max 確保位移量為正
                s += Math.max(1, j - badCharTable.getOrDefault(badChar, -1));
            }
        }

        return -1;
    }

    // 建立壞字元表
    private Map<Character, Integer> buildBadCharTable(String pattern) {
        Map<Character, Integer> table = new HashMap<>();
        for (int i = 0; i < pattern.length(); i++) {
            table.put(pattern.charAt(i), i);
        }
        return table;
    }
}