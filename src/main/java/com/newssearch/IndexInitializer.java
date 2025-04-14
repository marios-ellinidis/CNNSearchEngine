package com.newssearch;

import org.apache.lucene.analysis.Analyzer;
import org.springframework.stereotype.Component;

import com.newssearch.Analyzers.CustomAnalyzerNoSynonyms;
import com.newssearch.Analyzers.CustomEnglishAnalyzer;
import com.newssearch.filesManagement.CSVReader;
import com.newssearch.lucene.LuceneIndexer;
import com.newssearch.model.Article;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;

@Component
public class IndexInitializer {

    private static final String CSV_PATH = "CNN_Articels_clean.csv";
    private static final String INDEX_DIR = "lucene_index";
    private static final String INDEX_DIR2 = "lucene_index2";

    @PostConstruct
    public void init() {
        try {
            
            List<Article> articles = new CSVReader().readCSV(CSV_PATH);

            Analyzer analyzer = new CustomEnglishAnalyzer();
            LuceneIndexer indexer = new LuceneIndexer(INDEX_DIR,analyzer);
            indexer.indexArticles(articles);

            Analyzer analyzer2 = new CustomAnalyzerNoSynonyms();
            LuceneIndexer indexer2 = new LuceneIndexer(INDEX_DIR2,analyzer2);
            indexer2.indexArticles(articles);

            System.out.println("Indexing completed successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error during CSV reading or indexing: " + e.getMessage());
        }
    }
}
