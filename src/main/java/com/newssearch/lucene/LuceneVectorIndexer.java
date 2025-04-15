package com.newssearch.lucene;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.KnnVectorField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Comparator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class LuceneVectorIndexer {

    public void indexVectors(String jsonPath, String indexPath) throws IOException {
        // Delete old index directory if it exists
        Path path = Paths.get(indexPath);
        if (Files.exists(path)) {
            try {
                Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(file -> {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Proceed with vector indexing
        Directory dir = FSDirectory.open(path);
        IndexWriterConfig config = new IndexWriterConfig(new WhitespaceAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE); // Better to force recreation
        IndexWriter writer = new IndexWriter(dir, config);

        ObjectMapper mapper = new ObjectMapper();
        try (BufferedReader reader = new BufferedReader(new FileReader(jsonPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                JsonNode node = mapper.readTree(line);

                String index = node.get("Index").asText();
                Document doc = new Document();
                doc.add(new StringField("Index", index, Field.Store.YES));

                addVectorField(doc, node, "HeadlineEmbedding");
                addVectorField(doc, node, "DescriptionEmbedding");
                addVectorField(doc, node, "SecondHeadlineEmbedding");
                addVectorField(doc, node, "ArticleTextEmbedding");
                addVectorField(doc, node, "AuthorEmbedding");
                addVectorField(doc, node, "KeywordsEmbedding");
                addVectorField(doc, node, "SectionEmbedding");
                addVectorField(doc, node, "CategoryEmbedding");

                writer.addDocument(doc);
            }
            System.out.println("done!");
        }

        writer.close();
    }

    private void addVectorField(Document doc, JsonNode node, String fieldName) {
        if (node.has(fieldName)) {
            ArrayNode array = (ArrayNode) node.get(fieldName);
            float[] vector = new float[array.size()];
            for (int i = 0; i < array.size(); i++) {
                vector[i] = (float) array.get(i).asDouble();
            
            }
            doc.add(new KnnVectorField(fieldName, vector));
        }
    }
}
