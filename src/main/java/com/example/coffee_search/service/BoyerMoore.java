package com.example.coffee_search.service;

import java.util.HashMap;
import java.util.Map;

public class BoyerMoore {
    
    // 原始方法 (保留相容性，但建議少用)
    public int search(String text, String pattern) {
        return search(text, pattern, 0);
    }

    /**
     * [新增] 支援指定起始位置的搜尋
     * @param text 完整文本
     * @param pattern 搜尋關鍵字
     * @param fromIndex 從文本的第幾個字元開始找
     * @return 找到的索引位置，若無則回傳 -1
     */
    public int search(String text, String pattern, int fromIndex) {
        if (pattern == null || pattern.length() == 0) return -1;
        if (text == null || fromIndex >= text.length()) return -1;

        int m = pattern.length();
        int n = text.length();

        Map<Character, Integer> badCharTable = buildBadCharTable(pattern);

        // s 是 pattern 相對於 text 的位移量，起始點設為 fromIndex
        int s = fromIndex; 
        
        while (s <= (n - m)) {
            int j = m - 1;

            // 從後往前比對
            while (j >= 0 && pattern.charAt(j) == text.charAt(s + j)) {
                j--;
            }

            if (j < 0) {
                // 找到匹配！回傳該位置
                return s;
            } else {
                // 未匹配，根據壞字元規則移動
                char badChar = text.charAt(s + j);
                
                // 這裡要小心：壞字元表是根據 pattern 建的，不會變
                // 但我們要確保 s 是往後移動的
                int shift = j - badCharTable.getOrDefault(badChar, -1);
                s += Math.max(1, shift);
            }
        }

        return -1;
    }

    // 建立壞字元表 (保持不變)
    private Map<Character, Integer> buildBadCharTable(String pattern) {
        Map<Character, Integer> table = new HashMap<>();
        for (int i = 0; i < pattern.length(); i++) {
            table.put(pattern.charAt(i), i);
        }
        return table;
    }
}