# 📰 CNN News Article Search Engine

**An advanced, multi-field text and semantic search engine built with Java, Spring Boot, and Apache Lucene.**

This project implements a highly optimized search and retrieval system for CNN news articles. It bridges traditional information retrieval with advanced natural language processing (NLP) pipelines, typo tolerance, custom synonym graphs, and field-level dense vector embeddings for highly accurate semantic search. 

Everything is tied together with a clean, interactive Web UI for seamless querying and document reading.

---

## ✨ Search Capabilities & Web UI

* **Interactive Web Interface:** A custom frontend built with HTML5, CSS3, and Spring Boot Thymeleaf. It features clean styling (mirroring CNN's branding), smooth staggered result animations, pagination (10 results per page), and a dedicated "Show Article" view to read full documents.
* **Granular Multi-Field Filtering:** Users can search comprehensively across the whole document or isolate queries to explicit metadata slices: *Headline, Second Headline, Content, Description, Keywords, Author, Category, Section, and Date Published*.
* **Traditional Keyword Search (BM25):** Standard Lucene relevance scoring with fuzzy-matching capabilities (handling user typos up to an edit distance of 1).
* **Dynamic Synonym Toggle:** A UI checkbox allows users to enable or disable structural word alternatives on the fly. 
* **Semantic Vector Search:** Users can switch to a dedicated Vector Search mode powered by the HuggingFace `all-MiniLM-L6-v2` transformer model. Instead of exact word matches, the engine evaluates dense vector dimensions to find articles based on *context and meaning*. 
  * *Note: Vector queries take slightly longer to yield results, as the backend dynamically boots a Python sub-process to generate a normalized embedding for the user's query in real-time.*
* **Dynamic Highlighting:** Matched search terms are automatically highlighted and bolded within the result snippets for quick visual scanning.
* **Result Sorting:** Users can instantly toggle their result view between algorithmic relevance (Lucene score) and alphabetical sorting.
* **Personalized "Trending" Mechanics:** The system maintains a local search history audit log. Historically clicked or popular documents receive a dynamic relevance boost and are tagged with a visual "Popular Result" badge in the UI.

---

## 🧠 Architecture & Engineering Highlights

* **Storage-Over-Compute Synonym Strategy:** To prevent runtime query latency when users toggle synonyms, the system pre-builds separate physical inverted indexes (with and without a `SynonymGraphFilter`) and dynamically routes the query to the correct index.
* **Isolated Multi-Index Layout:** The system allocates physical index records into 3 dedicated index environments to guarantee maximum performance and clean query routing:
  1. `lucene_index`: Full-text inverted tokens with synonym structural expansions.
  2. `lucene_index2`: Exact text tokens for precise matching requirements.
  3. `lucene_index3`: Deep vector indices containing native `KnnVectorField` dense representations across 7 distinct metadata fields.
* **Smart Bootstrapping:** To avoid compute-heavy bottlenecks during deployment, the `IndexInitializer` validates the existence of the vectorized dataset (`article_embeddings.json`). If the cache is present, it bypasses the massive Python initialization script completely, ensuring near-instant Spring Boot startup cycles.
* **Custom Dictionary Pruning:** Includes a decoupled Python script that maps the massive WordNet vocabulary database against the real vocabulary present inside the CNN dataset. This filters out irrelevant noise and maintains an optimized, highly-performant synonym footprint.
* **Strategy Design Pattern:** Embraces decoupled software architectures by using the Strategy Pattern to swap algorithmic runtime behaviors cleanly depending on single-field or multi-field query actions.

---

## 🛠️ Tech Stack

* **Backend Core:** Java 17+, Spring Boot
* **Search Engine:** Apache Lucene (v9+ native Vector API support)
* **Machine Learning / NLP:** Python 3.x, SentenceTransformers (`all-MiniLM-L6-v2`)
* **Serialization & Parsing:** Jackson (`ObjectMapper`), Apache Commons CSV
* **Frontend UI Engine:** HTML5, CSS3, Thymeleaf Server-Side Rendering

---

## 🚀 Local Deployment Guide

### Prerequisites
* Java Development Kit (JDK 17 or higher)
* Apache Maven
* Python 3.x 

### Step-by-Step Execution

1. **Clone the repository** locally and navigate to the project root directory.
2. **Data Setup:** Ensure your dataset (`CNN_Articels_clean.csv`) is placed inside the root or `src/main/resources/` directory as required by the configuration.
3. **Install Python Dependencies:** Before running the application, install the required machine learning libraries. Open your terminal in the project root and run:
   ```bash
   pip install -r requirements.txt
