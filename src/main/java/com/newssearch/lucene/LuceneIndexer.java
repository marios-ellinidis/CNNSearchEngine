package com.newssearch.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.util.BytesRef;

import com.newssearch.model.Article;

import java.io.*;
import java.util.Comparator;
import java.util.List;

import java.nio.file.*;
import java.text.SimpleDateFormat;

    public class LuceneIndexer {

        private IndexWriter writer;

        public LuceneIndexer(String indexDir ,Analyzer analyzer) throws IOException {
            Path path = Paths.get(indexDir);

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

        Directory directory = FSDirectory.open(path);

       
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        writer = new IndexWriter(directory, config);
    }

    public void indexDocument(Document doc) throws IOException {
        writer.addDocument(doc);
    }

    public void close() throws IOException {
        writer.close();
    }

    public void indexArticles(List<Article> articles) throws IOException {
        for (Article article : articles) {
            Document doc = new Document();

            // Full-text indexed fields
            doc.add(new TextField("Headline", article.getHeadline(), Field.Store.YES));
            doc.add(new SortedDocValuesField("Headline", new BytesRef(article.getHeadline())));
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


            indexDocument(doc);
            
        }
        close();
    }
}
