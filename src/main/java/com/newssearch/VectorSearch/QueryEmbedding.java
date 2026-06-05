package com.newssearch.VectorSearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class QueryEmbedding {

    private static final String PYTHON = "python";

    /**
     * * Calls the Python script, passing the query as one arg.
     * Returns the normalized embedding as float].
     */
    public static float[] getQueryEmbedding(String query) throws IOException, InterruptedException {

        // Smart Path Resolution: Check nested folder first, then fallback to relative
        // path
        Path scriptPath = Paths.get("CNNSearchEngine/src/main/resources/embedding_query.py");
        if (!Files.exists(scriptPath)) {
            scriptPath = Paths.get("src/main/resources/embedding_query.py");
        }

        // Ensure the script actually exists before running the process
        if (!Files.exists(scriptPath)) {
            throw new java.io.FileNotFoundException(
                    "Could not find embedding_query.py at: " + scriptPath.toAbsolutePath());
        }

        ProcessBuilder pb = new ProcessBuilder(
                PYTHON,
                scriptPath.toAbsolutePath().toString(),
                query);

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