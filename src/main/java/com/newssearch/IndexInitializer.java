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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
public class IndexInitializer {

    private static final String CSV_PATH = "CNN_Articels_clean.csv";
    private static final String INDEX_DIR = "lucene_index";
    private static final String INDEX_DIR2 = "lucene_index2";
    private static final String VECTOR_INDEX_DIR = "lucene_index3";

    @PostConstruct
    public void init() {
        try {
            // Smart Path Resolution for the JSON file (handling the nested folders)
            Path jsonPath = Paths.get("CNNSearchEngine/src/main/resources/article_embeddings.json");

            // Fallback check: If the nested CNNSearchEngine directory doesn't exist, use
            // the standard path
            if (!Files.exists(Paths.get("CNNSearchEngine/src/main/resources"))) {
                jsonPath = Paths.get("src/main/resources/article_embeddings.json");
            }

            String resolvedJsonPath = jsonPath.toAbsolutePath().toString();
            File jsonFile = new File(resolvedJsonPath);

            // Step 1: Run Python embedding script only if JSON doesn't exist
            if (!jsonFile.exists()) {
                System.out.println("Embeddings JSON not found at: " + resolvedJsonPath);
                System.out.println("Running Python script to generate it...");
                runPythonScript();
            } else {
                System.out.println("Embeddings JSON already exists at: " + resolvedJsonPath);
                System.out.println("Skipping Python generation.");
            }

            // Step 2: Proceed with CSV reading and indexing
            List<Article> articles = new CSVReader().readCSV(CSV_PATH);

            Analyzer analyzer = new CustomEnglishAnalyzer();
            LuceneIndexer indexer = new LuceneIndexer(INDEX_DIR, analyzer);
            indexer.indexArticles(articles);

            Analyzer analyzer2 = new CustomAnalyzerNoSynonyms();
            LuceneIndexer indexer2 = new LuceneIndexer(INDEX_DIR2, analyzer2);
            indexer2.indexArticles(articles);

            // Pass the dynamically resolved absolute path to the Vector Indexer
            LuceneVectorIndexer vectorIndexer = new LuceneVectorIndexer();
            vectorIndexer.indexVectors(resolvedJsonPath, VECTOR_INDEX_DIR, articles);

            System.out.println("Indexing completed successfully.");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.err.println("Error during initialization: " + e.getMessage());
        }
    }

    private void runPythonScript() throws IOException, InterruptedException {
        // Try the inner directory structure first due to the nested project folders
        Path scriptPath = Paths.get("CNNSearchEngine/src/main/resources/article_embedding.py").toAbsolutePath();

        // Fallback check: If it doesn't exist there, use the default relative path
        if (!Files.exists(scriptPath)) {
            scriptPath = Paths.get("src/main/resources/article_embedding.py").toAbsolutePath();
        }

        System.out.println("Running script at: " + scriptPath);

        if (!Files.exists(scriptPath)) {
            throw new FileNotFoundException("Could not find article_embedding.py at either expected location.");
        }

        ProcessBuilder processBuilder = new ProcessBuilder("python", scriptPath.toString());
        processBuilder.redirectErrorStream(true); // Merge stderr into stdout
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[PYTHON] " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Python script exited with code " + exitCode);
        }
    }
}