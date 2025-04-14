package com.newssearch.Analyzers;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.util.CharsRef;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

public class CustomEnglishAnalyzer extends Analyzer {

    private SynonymMap synonymMap;

    public CustomEnglishAnalyzer() {
        try {
            SynonymMap.Builder synonymBuilder = new SynonymMap.Builder();
            loadSynonymsFromCSV("src/main/resources/FilteredSynonyms.csv", synonymBuilder);
            synonymMap = synonymBuilder.build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new StandardTokenizer();
        TokenStream filter = new LowerCaseFilter(source);
        filter = new StopFilter(filter, EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
        filter = new PorterStemFilter(filter);  
        filter = new SynonymGraphFilter(filter, synonymMap, true); 
        return new TokenStreamComponents(source, filter);
    }

    private void loadSynonymsFromCSV(String filePath, SynonymMap.Builder synonymBuilder) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String word = parts[0].trim().toLowerCase();
                    String[] synonyms = parts[1].split("[;|]");
                    for (String synonym : synonyms) {
                        synonym = synonym.trim().toLowerCase();
                        addSynonym(word, synonym, synonymBuilder);
                    }
                }
            }
        }
    }

    private void addSynonym(String word, String synonym, SynonymMap.Builder synonymBuilder) throws IOException {
        CharsRef wordRef = new CharsRef(word);
        CharsRef synonymRef = new CharsRef(synonym);
        synonymBuilder.add(wordRef, synonymRef, true);
    }
}

