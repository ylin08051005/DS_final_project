# DS_final_project
2025 Data Structure class

```mermaid
flowchart LR
  subgraph UI["UI Layer 使用者介面層"]
    U[使用者]
    V[ResultView - displayResults  displayError]
  end

  subgraph Controller["Controller Layer"]
    C[SearchController - start  handleQuery  handleError]
  end

  subgraph Service["Service Layer 應用邏輯層"]
    T[TranslationService - detect language translate to English]
    S[SearchService - fetchResults  buildSearchUrl  parseSearchResponse]
    A[AnalysisService - analyze  fetchHtml  extractText  tokenize]
    R[RankingService - rank  loadWeights  score]
    O[ResultService - format as HTML JSON]
  end

  subgraph Model["Model Layer 資料模型層"]
    M1[Keyword  name  weight]
    M2[SearchResult  url  title  snippet  score]
  end

  subgraph Util["Utility Layer 工具層"]
    H[HttpUtil  get]
    J[JsonUtil  parseLanguageCode  parseTranslatedText]
    X[TextUtil  stripHtml  normalize]
    P[HtmlParser  extractMainText  extractTitle]
    W[WeightConfig  loadFromJson]
    G[ConfigLoader  get]
    K[Cache  InMemoryCache]
  end

  subgraph External["External Services"]
    GT[(Google Translation API)]
    GS[(Google Custom Search API)]
  end

  U --> C
  C --> T --> GT
  C --> S --> GS
  C --> A
  C --> R
  C --> O
  C --> V

  T --> H
  T --> J
  S --> H
  S --> J
  R --> J
  A --> P
  A --> X
  R --> W
  T --> G
  S --> G
  A --> G
  R --> G
  C --> K

  S --> M2
  A --> M2
  R --> M2
  R --> M1
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


