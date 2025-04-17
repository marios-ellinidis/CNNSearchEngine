package com.newssearch.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import com.newssearch.Analyzers.CustomAnalyzerNoSynonyms;
import com.newssearch.Analyzers.CustomEnglishAnalyzer;
import com.newssearch.search.AllFieldsSearch;
import com.newssearch.search.FieldSearch;
import com.newssearch.search.SearchStrategy;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LuceneSearcher {
    private final IndexSearcher searcher;
    private Analyzer analyzer;
    private static final String VECTOR_INDEX_DIR = "lucene_index3";

    public LuceneSearcher(String indexDir) throws IOException {
        Directory dir = FSDirectory.open(Paths.get(indexDir));
        DirectoryReader reader = DirectoryReader.open(dir);
        this.searcher = new IndexSearcher(reader);
    }


    public List<Document> search(String queryStr, String field, boolean includeSynonyms, boolean sortAlphabetically) throws Exception {
        setupAnalyzer(includeSynonyms);
    
        // Step 1: Try exact query first
        Query exactQuery = new QueryParser(field, analyzer).parse(queryStr);
        SearchStrategy searchStrategy = new FieldSearch(searcher, analyzer);
        TopDocs results = searchStrategy.executeSearch(exactQuery, sortAlphabetically);
        
        if (results.totalHits.value > 0) {
            return searchStrategy.rankArticles(exactQuery, field, results, sortAlphabetically);
        }
    
        // Step 2: Fallback to fuzzy query , for autocorrect
        String fuzzyQueryStr = makeFuzzyQuery(queryStr);
        Query fuzzyQuery = new QueryParser(field, analyzer).parse(fuzzyQueryStr);
        TopDocs fuzzyResults = searchStrategy.executeSearch(fuzzyQuery, sortAlphabetically);
        
        return searchStrategy.rankArticles(fuzzyQuery, field, fuzzyResults, sortAlphabetically);
    }
    

    public List<Document> searchAllFields(String queryStr, boolean includeSynonyms, boolean sortAlphabetically) throws Exception {
        setupAnalyzer(includeSynonyms);
    
        String[] fields = {
            "Headline", "Description", "Content", "Keywords",
            "Second Headline", "Author", "Category", "Section", "Date Published"
        };
    
        SearchStrategy searchStrategy = new AllFieldsSearch(searcher, analyzer);
    
        // Step 1: Try the original query
        MultiFieldQueryParser exactParser = new MultiFieldQueryParser(fields, analyzer);
        Query exactQuery = exactParser.parse(queryStr);
        TopDocs results = searchStrategy.executeSearch(exactQuery, sortAlphabetically);
    
        if (results.totalHits.value > 0) {
            return searchStrategy.rankArticles(exactQuery, null, results, sortAlphabetically);
        }
    
        // Step 2: Try fuzzy version , gia autocorrect
        String fuzzyQueryStr = makeFuzzyQuery(queryStr);
        MultiFieldQueryParser fuzzyParser = new MultiFieldQueryParser(fields, analyzer);
        Query fuzzyQuery = fuzzyParser.parse(fuzzyQueryStr);
        TopDocs fuzzyResults = searchStrategy.executeSearch(fuzzyQuery, sortAlphabetically);
    
        return searchStrategy.rankArticles(fuzzyQuery, null, fuzzyResults, sortAlphabetically);
    }
    

    
    private void setupAnalyzer(boolean includeSynonyms) {
        this.analyzer = includeSynonyms ? new CustomEnglishAnalyzer() : new CustomAnalyzerNoSynonyms();
    }

    private String makeFuzzyQuery(String queryStr) {
        String[] words = queryStr.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.length() > 2) {
                builder.append(word).append("~1 ");  // 
            } else {
                builder.append(word).append(" ");
            }
        }
        return builder.toString().trim();
    }
     

    public List<Document> vectorSearch(float[] queryVector, String embeddingField, int k) throws Exception {
       
        KnnVectorQuery knnQuery = new KnnVectorQuery(embeddingField, queryVector, k);
        TopDocs topDocs = searcher.search(knnQuery, k);
        List<Document> results = new ArrayList<>();
       
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            results.add(doc);
        }
        
        return results;
    }

    public List<Document> vectorSearchAcrossFields(float[] queryVector, int k) throws IOException {
        Map<String, Integer> fieldResultLimits = Map.of(
            "ContentVector", 40,
            "HeadlineVector", 20,
            "DescriptionVector", 20,
            "KeywordsVector", 5,
            "AuthorVector", 5,
            "CategoryVector", 5,
            "SectionVector", 5
        );
    
        Map<Integer, Float> docScoreMap = new HashMap<>();
    
        for (Map.Entry<String, Integer> entry : fieldResultLimits.entrySet()) {
            String field = entry.getKey();
            int limit = entry.getValue();
    
            KnnVectorQuery knnQuery = new KnnVectorQuery(field, queryVector, limit);
            TopDocs topDocs = searcher.search(knnQuery, limit);
    
            for (ScoreDoc sd : topDocs.scoreDocs) {
                // Accumulate scores across fields
                docScoreMap.merge(sd.doc, sd.score, Float::sum);  // or Math::max depending on strategy
            }
        }
    
        // Sort by score descending and limit to top `k` total
        List<Map.Entry<Integer, Float>> sortedDocs = docScoreMap.entrySet().stream()
            .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
            .limit(k)
            .toList();
    
        List<Document> results = new ArrayList<>();
        for (Map.Entry<Integer, Float> entry : sortedDocs) {
            results.add(searcher.doc(entry.getKey()));
        }
    
        return results;
    }
    
    
    
}

