package com.example.coffee_search.controller;

import com.example.coffee_search.service.GoogleQuery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody; 
import org.springframework.web.client.RestTemplate;
import java.net.URLEncoder;


@Controller
public class SearchController {


    @Value("${google.cse.apiKey}")
    private String apiKey;

    @Value("${google.cse.cx}")
    private String cx;

    @PostMapping("/api/coffee_search")
    @ResponseBody
    public Map<String, String> searchCoffee(@RequestParam("apiInput") String query) throws Exception {
    Map<String, String> results = new HashMap<>();
    RestTemplate restTemplate = new RestTemplate();
    String q = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
    String url = "https://www.googleapis.com/customsearch/v1?key=" + apiKey + "&cx=" + cx + "&num=10&q=" + q;
    ResponseEntity<Map<String, Object>> resp = restTemplate.getForEntity(url, (Class<Map<String, Object>>) (Class<?>) Map.class);
    Map<String, Object> body = resp.getBody();
    if (body == null) {
        return results;
    }
    Object itemsObj = body.get("items");
    if (itemsObj instanceof List) {
        List<?> items = (List<?>) itemsObj;
        for (Object itemObj : items) {
            if (itemObj instanceof Map) {
                Map<String, Object> item = (Map<String, Object>) itemObj;
                Object titleObj = item.get("title");
                Object linkObj = item.get("link");
                String title = titleObj == null ? null : titleObj.toString();
                String link = linkObj == null ? null : linkObj.toString();
                if (title != null && !title.isEmpty() && link != null && !link.isEmpty()) {
                    results.put(title, link);
                }
            }
        }
    }
    return results;
}
    
    
    
}

