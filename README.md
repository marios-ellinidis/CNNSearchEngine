# 📰 CNN News Article Search Engine

**An advanced, multi-field text and semantic search engine built with Java, Spring Boot, and Apache Lucene.**

This project implements a highly optimized search and retrieval system for CNN news articles. It bridges traditional information retrieval with advanced natural language processing (NLP) pipelines, typo tolerance, custom synonym graphs, and field-level dense vector embeddings for highly accurate semantic search. 

Everything is tied together with a clean, interactive Web UI for seamless querying and document reading.

---

## ✨ Search Capabilities & Web UI

* **Interactive Web Interface:** A custom frontend built with HTML5, CSS3, and Spring Boot Thymeleaf. It features clean styling (mirroring CNN's branding), smooth staggered result animations, pagination (10 results per page), and a dedicated "Show Article" view to read full documents.
* **Granular Multi-Field Filtering:** Users can search comprehensively across the whole document or isolate queries to explicit metadata slices: *Headline, Second Headline, Content, Description, Keywords, Author, Category, Section, and Date Published*.
* **Traditional Keyword Search (BM25):** Standard Lucene relevance scoring with fuzzy-matching capabilities (handling minor user typos up to an edit distance of 1).
* **Semantic Vector Search:** Users can switch to a dedicated Vector Search mode powered by the HuggingFace `all-MiniLM-L6-v2` transformer model. Instead of exact word matches, the engine evaluates dense vector dimensions to find articles based on *context and meaning*. 
  * *Note: Vector queries take slightly longer to yield results, as the backend dynamically boots a Python sub-process to generate a normalized embedding for the user's query in real-time.*
* **Dynamic Synonym Toggle:** A UI checkbox allows users to enable or disable structural word alternatives on the fly. 
* **Dynamic Highlighting:** Matched search terms are automatically highlighted and bolded within the result snippets for quick visual scanning.
* **Result Sorting:** Users can instantly toggle their result view between algorithmic relevance (Lucene score) and alphabetical sorting.
* **Personalized "Trending" Mechanics & UI Badges:** The system automatically logs recent queries and their top 5 associated article IDs into a local cache (`search-history.txt`). If these historically highly-ranked articles surface in future searches, their relevance scores are artificially boosted to pin them to the top of the page. These items are distinctively marked in the UI with a **"Popular Result"** badge and chart-line icon.
* **Recent Search History:** The homepage dynamically reads the local history state to display a staggered, animated list of the user's most recent search queries.

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
2. **Data Setup:** Ensure your dataset (`CNN_Articels_clean.csv`) is present in the `src/main/resources/` directory.
3. **Install Python Dependencies:** Before running the application, install the required machine learning libraries. Open your terminal in the project root and run: `pip install -r requirements.txt`
4. **Launch the Application:** Build and run the Spring Boot entrypoint: `CnnArticlesSearchEngineApplication.java`.
5. **First Run Initialization:** The system will detect if the local vector cache (`article_embeddings.json`) is missing and automatically execute the Python transformer script to generate it. 
   * *Note: The first run will download the HuggingFace model and process the CSV, which may take a few minutes depending on your hardware.*
6. **Monitor the logs:** Watch the Java console. Once the system finishes building the underlying physical Lucene directories, it will log: `"Indexing completed successfully."`
7. **Access the Application:** Open your preferred local web browser and head directly to the UI: **`http://localhost:8080`**
