package com.newssearch.lucene;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.KnnVectorField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.newssearch.model.Article;




public class LuceneVectorIndexer {

    public void indexVectors(String jsonPath, String indexPath, List<Article> articles) throws IOException {
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

        Directory dir = FSDirectory.open(path);
        IndexWriterConfig config = new IndexWriterConfig(new WhitespaceAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter writer = new IndexWriter(dir, config);

        ObjectMapper mapper = new ObjectMapper();
        try (BufferedReader reader = new BufferedReader(new FileReader(jsonPath))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                JsonNode node = null;

                try {
                    node = mapper.readTree(line);
                } catch (Exception e) {
                    System.err.println("Skipping line " + lineNumber + " due to JSON parsing error: " + e.getMessage());
                    continue;
                }

                if (node == null || node.isEmpty()) {
                    System.err.println("Skipping line " + lineNumber + ": node is null or empty");
                    continue;
                }

                JsonNode indexNode = node.get("Index");
                if (indexNode == null || indexNode.isNull()) {
                    System.err.println("Skipping line " + lineNumber + ": 'Index' field is missing");
                    continue;
                }

                String index = indexNode.asText();
                Document doc = new Document();
                doc.add(new StringField("Index", index, Field.Store.YES));

                // Add textual content from the articles
                addTextFieldsFromArticle(doc, articles, index);

                // Add embedding vectors
                addVectorField(doc, node, "HeadlineVector");
                addVectorField(doc, node, "DescriptionVector");
                addVectorField(doc, node, "ContentVector");
                addVectorField(doc, node, "AuthorVector");
                addVectorField(doc, node, "KeywordsVector");
                addVectorField(doc, node, "SectionVector");
                addVectorField(doc, node, "CategoryVector");

                writer.addDocument(doc);
            }
        }

        writer.close();
    }

    private void addTextFieldsFromArticle(Document doc, List<Article> articles, String index) {
      
        for (Article article : articles) {
            if (String.valueOf(article.getIndex()).equals(index)) {
                doc.add(new TextField("Headline", article.getHeadline(), Field.Store.YES));
                doc.add(new TextField("Description", article.getDescription(), Field.Store.YES));
                doc.add(new TextField("Content", article.getArticleText(), Field.Store.YES));
                doc.add(new TextField("Keywords", article.getKeywords(), Field.Store.YES));
                doc.add(new TextField("Second Headline", article.getSecondHeadline(), Field.Store.YES));
                doc.add(new TextField("Author", article.getAuthor(), Field.Store.YES));
                doc.add(new TextField("Category", article.getCategory(), Field.Store.YES));
                doc.add(new TextField("Section", article.getSection(), Field.Store.YES));
                doc.add(new StoredField("url", article.getUrl()));
                doc.add(new StringField("index", String.valueOf(article.getIndex()), Field.Store.YES));

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String dateOnlyStr = dateFormat.format(article.getDatePublished());
                doc.add(new TextField("datePublished", dateOnlyStr, Field.Store.YES));
            }
        }
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

