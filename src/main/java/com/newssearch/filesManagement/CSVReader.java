package com.newssearch.filesManagement;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.newssearch.model.Article;

import java.io.InputStreamReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CSVReader {

    public List<Article> readCSV(String filePath) throws IOException {
        List<Article> articles = new ArrayList<>();

        
        Resource resource = new ClassPathResource(filePath);
        
        
        try (InputStreamReader fileReader = new InputStreamReader(resource.getInputStream())) {
          
            CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withHeader().withSkipHeaderRecord());

            for (CSVRecord record : csvParser) {
               
                int index = Integer.parseInt(record.get("Index"));
                Date datePublished = null;

                try {
                    
                    datePublished = new SimpleDateFormat("yyyy-MM-dd").parse(record.get("Date published"));
                } catch (ParseException e) {
                    e.printStackTrace(); 
                }

                String author = record.get("Author");
                String category = record.get("Category");
                String section = record.get("Section");
                String url = record.get("Url");
                String headline = record.get("Headline");
                String description = record.get("Description");
                String keywords = record.get("Keywords");
                String secondHeadline = record.get("Second headline");
                String articleText = record.get("Article text");

               
                Article article = new Article(index, author, datePublished, category, section, url, headline, description, keywords, secondHeadline, articleText);
                articles.add(article);
            }

            csvParser.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error reading CSV file: " + e.getMessage());
        }
        
        return articles;
    }
}
