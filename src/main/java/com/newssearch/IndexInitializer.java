package com.newssearch;

import org.apache.lucene.analysis.Analyzer;
import org.springframework.stereotype.Component;

import com.newssearch.Analyzers.CustomAnalyzerNoSynonyms;
import com.newssearch.Analyzers.CustomEnglishAnalyzer;
import com.newssearch.filesManagement.CSVReader;
import com.newssearch.lucene.LuceneIndexer;
import com.newssearch.lucene.LuceneVectorIndexer;
import com.newssearch.model.Article;

import jakarta.annotation.PostConstruct;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
public class IndexInitializer {

    private static final String CSV_PATH = "CNN_Articels_clean.csv";
    private static final String INDEX_DIR = "lucene_index";
    private static final String INDEX_DIR2 = "lucene_index2";
    private static final String VECTOR_JSON_PATH = "src/main/resources/article_embeddings.json";
    private static final String VECTOR_INDEX_DIR = "lucene_index3";

    @PostConstruct
    public void init() {
        try {
            // Step 1: Run Python embedding script only if JSON doesn't exist
            File jsonFile = new File(VECTOR_JSON_PATH);
            if (!jsonFile.exists()) {
                System.out.println("Embeddings JSON not found. Running Python script to generate it...");
                runPythonScript();
            } else {
                System.out.println("Embeddings JSON already exists. Skipping Python generation.");
            }

            // Step 2: Proceed with CSV reading and indexing
            List<Article> articles = new CSVReader().readCSV(CSV_PATH);

            Analyzer analyzer = new CustomEnglishAnalyzer();
            LuceneIndexer indexer = new LuceneIndexer(INDEX_DIR, analyzer);
            indexer.indexArticles(articles);

            Analyzer analyzer2 = new CustomAnalyzerNoSynonyms();
            LuceneIndexer indexer2 = new LuceneIndexer(INDEX_DIR2, analyzer2);
            indexer2.indexArticles(articles);

            LuceneVectorIndexer vectorIndexer = new LuceneVectorIndexer();
            vectorIndexer.indexVectors(VECTOR_JSON_PATH, VECTOR_INDEX_DIR, articles);
            
            System.out.println("Indexing completed successfully.");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.err.println("Error during initialization: " + e.getMessage());
        }
    }

    private void runPythonScript() throws IOException, InterruptedException {
        // Make sure to use the full absolute path to your Python script
        //Path scriptPath = Paths.get("src/main/resources/article_embedding.py").toAbsolutePath();
       //System.out.println("Running script at: " + scriptPath);

       // ProcessBuilder processBuilder = new ProcessBuilder("python", scriptPath.toString());
        //processBuilder.redirectErrorStream(true); // Merge stderr into stdout
        //Process process = processBuilder.start();

        //try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
          //  String line;
           // while ((line = reader.readLine()) != null) {
             //   System.out.println("[PYTHON] " + line);
            //}
        //}

        //int exitCode = process.waitFor();
        //if (exitCode != 0) {
          //  throw new RuntimeException("Python script exited with code " + exitCode);
        ///}

    }
}
