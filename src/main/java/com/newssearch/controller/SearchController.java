package com.newssearch.controller;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.Query;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.newssearch.VectorSearch.QueryEmbedding;
import com.newssearch.filesManagement.SearchHistory;
import com.newssearch.lucene.LuceneSearcher;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class SearchController {

    private final String indexDir = "lucene_index";
    private final String indexDir2 = "lucene_index2";
    private final String indexDir3 = "lucene_index3";
    private List<Document> fullResults;

    @GetMapping("/")
    public String home(@RequestParam(value = "error", required = false) String error, Model model) {
        model.addAttribute("fields", Arrays.asList("Headline", "Description", "Content", "Keywords", "Second Headline", "Author", "Category", "Section"));

        List<String> fullHistory = SearchHistory.getRecentQueries(20);
        List<String> queryOnly = fullHistory.stream()
                .map(line -> line.split(":")[0].trim())
                .collect(Collectors.toList());

        model.addAttribute("history", queryOnly);

        // Add the error message to the model if it exists
        if (error != null && !error.isEmpty()) {
            model.addAttribute("error", error);
        }

        return "index";
    }

    @GetMapping("/vector")
    public String homeVector(@RequestParam(value = "error", required = false) String error, Model model) {
        model.addAttribute("fields", Arrays.asList("Headline", "Description", "Content", "Keywords", "Second Headline", "Author", "Category", "Section"));

        if (error != null && !error.isEmpty()) {
            model.addAttribute("error", error);
        }

        return "vector_index";
    }



    @RequestMapping(value = "/search", method = {RequestMethod.GET, RequestMethod.POST})
    public String search(@RequestParam String query, @RequestParam String field,
                     @RequestParam(required = false, defaultValue = "false") boolean includeSynonyms,
                     @RequestParam(required = false, defaultValue = "1") int page,
                     @RequestParam(required = false, defaultValue = "10") int size,
                     Model model) {
        try {
            LuceneSearcher searcher;
            if(includeSynonyms){
                searcher = new LuceneSearcher(indexDir);                
            }
            else{
                searcher = new LuceneSearcher(indexDir2);
            }
            
            List<Document> results ;
        
            if ("All".equals(field)) {
               
                results = searcher.searchAllFields(query, includeSynonyms , false);
                
            } else {
                results = searcher.search(query, field, includeSynonyms, false);

            }

            int totalResults = results.size();
            int start = (page - 1) * size;
            int end = Math.min(start + size, totalResults);
            List<Document> pagedResults = results.subList(start, end);



            SearchHistory.addQuery(query , results);

            model.addAttribute("query", query);
            model.addAttribute("includeSynonyms", includeSynonyms);
            model.addAttribute("field", field); 
            model.addAttribute("results", results); 
            model.addAttribute("results", pagedResults);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("totalPages", (int) Math.ceil((double) totalResults / size));

            return "results";
        } catch (Exception e) {
            
            return "redirect:/?error=" + URLEncoder.encode("No results found", StandardCharsets.UTF_8);

        }
    }

   


    @GetMapping("/results/sorted")
    public String searchSorted(@RequestParam String query, @RequestParam String field,
                                @RequestParam(required = false, defaultValue = "false") boolean includeSynonyms,
                                @RequestParam(required = false, defaultValue = "1") int page,
                                @RequestParam(required = false, defaultValue = "10") int size,
                                Model model) {
        try {
            LuceneSearcher searcher;
            if (includeSynonyms) {
                searcher = new LuceneSearcher(indexDir);
            } else {
                searcher = new LuceneSearcher(indexDir2);
            }
            
            List<Document> results;
            if ("All".equals(field)) {
                results = searcher.searchAllFields(query, includeSynonyms , true);
            } else {
                results = searcher.search(query, field, includeSynonyms, true); // Sorting enabled
            }

            int totalResults = results.size();
            int start = (page - 1) * size;
            int end = Math.min(start + size, totalResults);
            List<Document> pagedResults = results.subList(start, end);
            
            model.addAttribute("query", query);
            model.addAttribute("field", field);
            model.addAttribute("results", results);
            model.addAttribute("includeSynonyms", includeSynonyms); // Preserve the includeSynonyms option
            model.addAttribute("results", pagedResults);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("totalPages", (int) Math.ceil((double) totalResults / size));
            return "sorted_results";
        } catch (Exception e) {
            model.addAttribute("error", "Search failed: " + e.getMessage());
            return "index";
        }
    }

    @RequestMapping(value = "/vectorSearch", method = {RequestMethod.GET, RequestMethod.POST})
    public String vectorSearching(@RequestParam String query,
                                @RequestParam String field,
                                @RequestParam(required = false, defaultValue = "1") int page,
                                @RequestParam(required = false, defaultValue = "10") int size,
                                Model model) {
        try {
           
            LuceneSearcher searcher = new LuceneSearcher(indexDir3);
            
            List<Document> results;
            System.out.println("page "+page);
            if(page==1){
                System.out.println("entered in searching");
                float[] vector = QueryEmbedding.getQueryEmbedding(query);
                if ("All".equals(field)) {
                    results = searcher.vectorSearchAcrossFields(vector,100);
                    this.fullResults =results;
                } else {
                    results = searcher.vectorSearch(vector, field, 100);
                    this.fullResults = results;
                }
            }else{//avoid recalculating for next pages
                results = this.fullResults;
            }
            
            

            int totalResults = results.size();
            int start = (page - 1) * size;
            int end = Math.min(start + size, totalResults);
            List<Document> pagedResults = results.subList(start, end);

            model.addAttribute("query", query);
            model.addAttribute("field", field);
            model.addAttribute("results", pagedResults); // ← only keep pagedResults here
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("totalPages", (int) Math.ceil((double) totalResults / size));

            return "vector_results";

        } catch (Exception e) {
            return "redirect:/?error=" + URLEncoder.encode("yoooo", StandardCharsets.UTF_8);
        }
    }

    





    @GetMapping("/article/{index}")
    public String showArticle(@PathVariable String index, Model model) {
        try {
            
            Query indexQuery = new TermQuery(new Term("index", index));

           
            IndexSearcher indexSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(indexDir))));
            TopDocs results = indexSearcher.search(indexQuery, 1); 
            
            if (results.totalHits.value > 0) {
                Document doc = indexSearcher.doc(results.scoreDocs[0].doc);  
                model.addAttribute("article", doc); 
                model.addAttribute("error", "Article not found");
            }

            return "article";  
        } catch (Exception e) {
            model.addAttribute("error", "Error retrieving article: " + e.getMessage());
            return "index";
        }
    }
}
