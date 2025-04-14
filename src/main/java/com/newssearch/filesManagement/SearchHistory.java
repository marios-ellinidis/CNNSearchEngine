package com.newssearch.filesManagement;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;

public class SearchHistory {

    private static final String HISTORY_FILE = "search_history.txt";

    public static void addQuery(String query, List<Document> results) {
        Set<String> existingQueries = new LinkedHashSet<>();
    
       
        try (BufferedReader reader = new BufferedReader(new FileReader(HISTORY_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                existingQueries.add(line.trim());
            }
        } catch (IOException e) {
            
        }
    
        
        List<String> topIndexes = results.stream()
                .limit(5)
                .map(doc -> doc.get("index"))
                .collect(Collectors.toList());
    
        String entry = query + ": " + String.join(", ", topIndexes);
    
        
        boolean alreadyExists = existingQueries.stream().anyMatch(s -> s.startsWith(query + ":"));
        if (!alreadyExists) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(HISTORY_FILE, true))) {
                writer.write(entry);
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    

    public static List<String> getRecentQueries(int limit) {
        List<String> history = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(HISTORY_FILE));
            int start = Math.max(lines.size() - limit, 0);
            history = lines.subList(start, lines.size());
            Collections.reverse(history); 
        } catch (IOException e) {
            System.err.println("Failed to read search history: " + e.getMessage());
        }
        return history;
    }

    public static Set<Integer> getBoostedIndexesFromRecentQueries(int limit) {
        Set<Integer> boostedIndexes = new LinkedHashSet<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(HISTORY_FILE));
            int start = Math.max(lines.size() - limit, 0);
            List<String> recentLines = lines.subList(start, lines.size());
    
            for (String line : recentLines) {
                String[] parts = line.split(":");
                if (parts.length > 1) {
                    String[] indexes = parts[1].split(",");
                    for (String idx : indexes) {
                        try {
                            boostedIndexes.add(Integer.parseInt(idx.trim()));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read search history: " + e.getMessage());
        }
    
        return boostedIndexes;
    }

    public static Map<Integer, Integer> getBoostedIndexFrequencies(int limit) {
        Map<Integer, Integer> frequencyMap = new HashMap<>();
    
        try {
            List<String> lines = Files.readAllLines(Paths.get(HISTORY_FILE));
            int start = Math.max(lines.size() - limit, 0);
            List<String> recentLines = lines.subList(start, lines.size());
    
            for (String line : recentLines) {
                int colonIndex = line.indexOf(":");
                if (colonIndex == -1) continue;
    
                String indexesPart = line.substring(colonIndex + 1).trim();
                String[] indexTokens = indexesPart.split(",");
    
                for (String token : indexTokens) {
                    try {
                        int index = Integer.parseInt(token.trim());
                        frequencyMap.put(index, frequencyMap.getOrDefault(index, 0) + 1);
                    } catch (NumberFormatException e) {
                      
                    }
                }
            }
    
        } catch (IOException e) {
            System.err.println("Failed to read search history: " + e.getMessage());
        }
    
        return frequencyMap;
    }
    
}

    
