# DS_final_project
2025 Data Structure class

```mermaid
flowchart TD
    %% 全域樣式定義
    classDef unifiedStyle fill:#fff4dd,stroke:#6F4E37,stroke-width:2px,color:#333

    %% 使用者介面層
    subgraph UI_Layer ["UI Layer 使用者介面層"]
        User((使用者))
        Index[index.html]
    end

    %% 控制層
    subgraph Controller_Layer ["Controller Layer 控制層"]
        C2[CoffeeSearchControllerV2]
    end

    %% 應用邏輯層
    subgraph Service_Layer ["Service Layer 應用邏輯層"]
        GS[GeminiService]
        DRS[DeepRankingService]
        HF[HtmlFetcher]
        BM[BoyerMoore]
        HS[HeapSorter]
    end

    %% 資料與模型層
    subgraph Model_Layer ["Model Layer 資料與模型層"]
        KR[KeywordRepository]
        SR[SearchResult]
        M_DB[(H2 Database)]
    end

    %% 外部服務
    subgraph External_Services ["External Services 外部服務"]
        GE_API[Gemini AI API]
        GS_API[Google Custom Search API]
    end

    %% 流程連線
    User --> Index
    Index -- "1. 送出搜尋請求" --> C2
    
    %% 修正後的外部 API 呼叫路徑
    C2 -- "2. 分析語言/優化查詢" --> GS
    GS <--> GE_API
    
    C2 -- "3. 獲取原始搜尋結果" --> GS_API
    
    C2 -- "4. 執行深度排序與爬蟲" --> DRS
    DRS -- "抓取網頁內容" --> HF
    DRS -- "計算關鍵字頻率" --> BM
    DRS -- "執行堆積排序" --> HS
    
    DRS -- "讀取多語言權重" --> KR
    KR <--> M_DB
    
    DRS -.-> SR
    C2 -- "5. 回傳排序結果" --> Index
    Index -- "渲染頁面" --> User

    %% 套用統一顏色
    class UI_Layer,Controller_Layer,Service_Layer,Model_Layer,External_Services,Index,C2,GS,DRS,HF,BM,HS,KR,SR,GE_API,GS_API unifiedStyle
```

## 2. Folder Structure
```mermaid
flowchart TB
  A[TopicSearchEngine] --> B[src]
  B --> C[main]
  C --> D[controller]
  C --> E[service]
  C --> F[model]
  C --> G[util]
  C --> H[view]
  B --> R[resources]

  D --> D1[SearchController.java]
  E --> E1[TranslationService.java]
  E --> E2[SearchService.java]
  E --> E3[AnalysisService.java]
  E --> E4[RankingService.java]
  E --> E5[ResultService.java]
  F --> F1[Keyword.java]
  F --> F2[SearchResult.java]
  G --> G1[HttpUtil.java]
  G --> G2[JsonUtil.java]
  G --> G3[TextUtil.java]
  G --> G4[HtmlParser.java]
  G --> G5[WeightConfig.java]
  G --> G6[ConfigLoader.java]
  G --> G7[Cache.java]
  H --> H1[ResultView.java]
  R --> R1[config.properties]
  R --> R2[D1_weight.json]
  R --> R3[stopwords.txt]
```

## 3. Class Diagram
```mermaid
classDiagram
    class CoffeeSearchControllerV2 {
        -GeminiService geminiService
        -KeywordRepository keywordRepository
        -HtmlFetcher htmlFetcher
        -DeepRankingService deepRankingService
        +searchCoffeeV2(String apiInput) Map
    }

    class DeepRankingService {
        -HtmlFetcher htmlFetcher
        -KeywordRepository keywordRepository
        +deepRank(List~SearchResult~, String) List~SearchResult~
    }

    class GeminiService {
        -String apiKey
        -String apiUrl
        +analyzeQuery(String userQuery) GeminiAnalysisResult
        -cleanJsonString(String text) String
    }

    class HtmlFetcher {
        +fetchContent(String url) String
        +extractLinks(String url) Set~String~
    }

    class BoyerMoore {
        +search(String text, String pattern, int fromIndex) int
        -buildBadCharTable(String pattern) Map
    }

    class HeapSorter {
        -ArrayList~SearchResult~ heap
        +insert(SearchResult result)
        +extractMax() SearchResult
        +getSortedList() List~SearchResult~
    }

    class SearchResult {
        +String title
        +String link
        +String snippet
        +double score
    }

    class Keyword {
        +Long id
        +String name
        +String searchTerm
        +Integer weight
        +String language
    }

    %% 關係定義
    CoffeeSearchControllerV2 ..> GeminiService : 使用
    CoffeeSearchControllerV2 ..> DeepRankingService : 調用
    CoffeeSearchControllerV2 ..> HtmlFetcher : 使用
    DeepRankingService ..> BoyerMoore : 執行比對
    DeepRankingService ..> HeapSorter : 執行排序
    DeepRankingService ..> HtmlFetcher : 爬取子連結
    HeapSorter "1" *-- "many" SearchResult : 管理
    DeepRankingService ..> Keyword : 匹配權重
```

## 4. Sequence Diagram
```mermaid
sequenceDiagram
    autonumber
    actor User as 使用者
    participant UI as index.html (前端)
    participant C as CoffeeSearchControllerV2
    participant G as GeminiService
    participant API as Google Search API
    participant H as HtmlFetcher
    participant D as DeepRankingService
    participant B as BoyerMoore
    participant S as HeapSorter

    User->>UI: 輸入關鍵字並點擊搜尋
    UI->>C: POST /api/v2/search (apiInput)
    
    Note over C,G: 1. 語言偵測與查詢優化
    C->>G: analyzeQuery(userQuery)
    G-->>C: 回傳 GeminiAnalysisResult (語言與優化查詢)

    Note over C,API: 2. 獲取原始搜尋結果
    loop 分頁抓取 (共 2 頁)
        C->>API: 呼叫 Google Custom Search
        API-->>C: 回傳搜尋結果 (SearchResult 列表)
    end

    Note over C,H: 3. 初步網頁計分
    C->>H: fetchContent(網址)
    H-->>C: 回傳網頁內容
    C->>B: 使用 Boyer-Moore 比對關鍵字
    B-->>C: 累計主頁面分數

    Note over C,D: 4. 啟動深度重排序 (Deep Ranking)
    C->>D: deepRank(results, language)
    
    loop 對於每一筆結果 (平行處理)
        D->>H: extractLinks(主連結)
        H-->>D: 回傳子連結集合
        
        loop 對於每個子連結
            D->>H: fetchContent(子連結)
            H-->>D: 回傳內容
            D->>B: search(內容, 關鍵字)
            B-->>D: 回傳出現次數
        end
        D->>D: 計算子頁面加權獎勵並更新分數
    end

    Note over D,S: 5. 堆積排序 (Heap Sort)
    D->>S: insert(SearchResult)
    S->>D: getSortedList() (從大到小排序)
    D-->>C: 回傳排序後的結果清單

    C-->>UI: 回傳最終排序 Map (網址: 分數)
    UI-->>User: 渲染搜尋結果列表
```

## 5. System Architecture
```mermaid
graph TD
    subgraph "前端介面 (Thymeleaf)"
        UI[index.html]
    end

    subgraph "控制層 (Controller)"
        SC[SearchController / V2]
    end

    subgraph "服務層 (Service Layer)"
        GS[GeminiService]
        HF[HtmlFetcher]
        DRS[DeepRankingService]
        HS[HeapSorter]
        BM[BoyerMoore]
    end

    subgraph "資料層 (Repository)"
        KR[KeywordRepository]
        DB[(H2 Database)]
    end

    subgraph "外部服務 (External APIs)"
        G_API[Google Custom Search API]
        GEMINI[Gemini AI API]
    end

    UI -- "POST /api/v2/search" --> SC
    SC -- "1. 語言偵測/翻譯" --> GS
    GS -- "REST" --> GEMINI
    SC -- "2. 搜尋網頁" --> G_API
    SC -- "3. 網頁爬取" --> HF
    SC -- "4. 深度重排序" --> DRS
    DRS -- "子頁面爬取" --> HF
    DRS -- "關鍵字比對" --> BM
    DRS -- "排序演算法" --> HS
    SC -- "讀取權重" --> KR
    KR --> DB
    SC -- "回傳 JSON 結果" --> UI
```


