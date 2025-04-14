package com.newssearch.search;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;

import com.newssearch.util.Pair;

public interface SearchStrategy {
    TopDocs executeSearch(Query query, boolean sortAlphabetically) throws IOException, InvalidTokenOffsetsException;
    List<Document> rankArticles(Query query, String field, TopDocs results, boolean sortAlphabetically) throws IOException, InvalidTokenOffsetsException;
    List<Document> sortAndCombineResults(List<Pair<Document, Integer>> boosted, List<Document> normal);
    String getContentSnippet(String text, Query query, String field) throws IOException, InvalidTokenOffsetsException;
    String getHighlightedText(String text, Query query, String field) throws IOException, InvalidTokenOffsetsException;
    void checkIfPopularAnswer(Document doc, Map<Integer, Integer> boostedFreqMap, List<Pair<Document, Integer>> boosted, List<Document> normal);
}
