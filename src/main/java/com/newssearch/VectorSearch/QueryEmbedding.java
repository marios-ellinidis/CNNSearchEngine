package com.newssearch.VectorSearch;



import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.document.Document;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class QueryEmbedding {

   
    private static final String PYTHON = "python";  
    private static final Path SCRIPT = Paths.get("src/main/resources/embedding_query.py");

    /** 
     * Calls the Python script, passing the query as one arg.
     * Returns the normalized embedding as float[].
     */

    
    public static float[] getQueryEmbedding(String query) throws IOException, InterruptedException {
       
        ProcessBuilder pb = new ProcessBuilder(
            PYTHON,
            SCRIPT.toAbsolutePath().toString(),
            query
        );
       
        
        pb.redirectErrorStream(true);

        Process proc = pb.start();
       
      
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                 new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
          
        }

        int exit = proc.waitFor();
      
        if (exit != 0) {
            throw new RuntimeException(
                "Error in embedding script (exit code " + exit + "): " + out);
        }

        ObjectMapper mapper = new ObjectMapper();
      
        JsonNode root = mapper.readTree(out.toString());
       
        if (root.has("error")) {
            throw new RuntimeException("Embedding script error: " + root.get("error").asText());
        }

        JsonNode arr = root.get("embedding");
       
        float[] embedding = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            embedding[i] = (float) arr.get(i).asDouble();
        }
        return embedding;   
        
    }
}

