# DS_final_project
2025 Data Structure class
11/3 Today is rainy.

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
    T[TranslationService - detectAndTranslate  detectLanguage  translateToEnglish]
    S[SearchService - fetchResults  buildSearchUrl  parseSearchResponse]
    A[AnalysisService - analyze  fetchHtml  extractText  tokenize]
    R[RankingService - rank  loadWeights  score]
    O[ResultService - formatAsHTML  formatAsJSON]
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

