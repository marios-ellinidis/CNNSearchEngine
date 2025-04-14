package com.newssearch.search;

import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import com.newssearch.util.Pair;
import com.newssearch.filesManagement.SearchHistory;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AllFieldsSearch implements SearchStrategy{

    private final IndexSearcher searcher;
    private final Analyzer analyzer;

    public AllFieldsSearch(IndexSearcher searcher, Analyzer analyzer) {
        this.searcher = searcher;
        this.analyzer = analyzer;

    }

    @Override
    public TopDocs executeSearch(Query query, boolean sortAlphabetically) throws IOException {
        if (sortAlphabetically) {
            Sort sort = new Sort(new SortField("Headline", SortField.Type.STRING));
            return searcher.search(query, Integer.MAX_VALUE, sort);
        } else {
            return searcher.search(query, Integer.MAX_VALUE);
        }
    }

    @Override
    public List<Document> rankArticles(Query query, String field, TopDocs results, boolean sortAlphabetically) throws IOException, InvalidTokenOffsetsException{
        if (sortAlphabetically) {
            return handleSortedResults(results, query);
        } else {
            return handleUnsortedResults(results, query);
        }
    }


    private List<Document> handleSortedResults(TopDocs results, Query query) throws IOException, InvalidTokenOffsetsException {
        List<Document> finalResults = new ArrayList<>();
        for (ScoreDoc sd : results.scoreDocs) {
            Document doc = searcher.doc(sd.doc);
            String highlightedText = handleHighlightingForDocument(doc, query);
            doc.add(new TextField("highlighted", highlightedText, Field.Store.YES));
            finalResults.add(doc);
        }
        return finalResults;
    }
    
    private List<Document> handleUnsortedResults(TopDocs results, Query query) throws IOException, InvalidTokenOffsetsException {
        Map<Integer, Integer> boostedFreqMap = SearchHistory.getBoostedIndexFrequencies(20);
        List<Pair<Document, Integer>> boosted = new ArrayList<>();
        List<Document> normal = new ArrayList<>();
    
        for (ScoreDoc sd : results.scoreDocs) {
            Document doc = searcher.doc(sd.doc);
            String highlightedText = handleHighlightingForDocument(doc, query);
            doc.add(new TextField("highlighted", highlightedText, Field.Store.YES));
    
            checkIfPopularAnswer(doc, boostedFreqMap, boosted, normal);
        }
    
        return sortAndCombineResults(boosted, normal);
    }

    private String handleHighlightingForDocument(Document doc, Query query) throws IOException, InvalidTokenOffsetsException {
        String[] fields = {"Headline", "Description", "Content", "Keywords", "Second Headline", "Author", "Category", "Section", "Date Published"};
        String highlightedText = null;
    
        for (String fieldName : fields) {
            String text = doc.get(fieldName);
            if (text == null || text.isEmpty()) continue;
    
            String snippet;
            if ("Content".equals(fieldName)) {
                snippet = getContentSnippet(text, query, fieldName);
            } else {
                snippet = getHighlightedText(text, query, fieldName);
            }
    
            if (snippet != null && !snippet.isEmpty()) {
                highlightedText = snippet;
                break;
            }
        }
        return highlightedText != null ? highlightedText : "";
    }
    

    @Override
    public String getHighlightedText(String text, Query query, String field) throws IOException, InvalidTokenOffsetsException {
        if (text == null) return "";

        QueryScorer scorer = new QueryScorer(query, field);
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer);
        SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<mark>", "</mark>");  // or use <strong> tags
        Highlighter highlighter = new Highlighter(formatter, scorer);
        highlighter.setTextFragmenter(fragmenter);

        TokenStream tokenStream = analyzer.tokenStream(field, new StringReader(text));
        return highlighter.getBestFragment(tokenStream, text);
    }

    @Override
    public String getContentSnippet(String text, Query query, String field) throws IOException, InvalidTokenOffsetsException {
        if (text == null) return "";
    
        
        int snippetLength = 150; 
        QueryScorer scorer = new QueryScorer(query, field);
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, snippetLength);
        SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<mark>", "</mark>");
        Highlighter highlighter = new Highlighter(formatter, scorer);
        highlighter.setTextFragmenter(fragmenter);
    
        TokenStream tokenStream = analyzer.tokenStream(field, new StringReader(text));
        String snippet = highlighter.getBestFragment(tokenStream, text);
    
        
        if (snippet != null && snippet.length() > snippetLength) {
            snippet = "..." + snippet.substring(0, snippetLength) + "...";
        }
    
        return snippet;
    }    

    @Override
    public void checkIfPopularAnswer(Document doc, Map<Integer, Integer> boostedFreqMap, List<Pair<Document, Integer>> boosted, List<Document> normal) {
        int index = Integer.parseInt(doc.get("index"));
        if (boostedFreqMap.containsKey(index)) {
            int freq = boostedFreqMap.get(index);
            doc.add(new StringField("boosted", "true", Field.Store.YES));
            boosted.add(new Pair<>(doc, freq));
        } else {
            normal.add(doc);
        }
    }

    @Override
    public List<Document> sortAndCombineResults(List<Pair<Document, Integer>> boosted, List<Document> normal) {
        
        boosted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        
        List<Document> finalResults = new ArrayList<>();
        
        for (Pair<Document, Integer> pair : boosted) {
            finalResults.add(pair.getKey());
        }
        
        finalResults.addAll(normal);
        
        return finalResults;
    }
    

}
