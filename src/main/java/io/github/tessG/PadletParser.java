package io.github.tessG;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * Parser for Padlet evaluation data
 * Fetches from Padlet API and returns unified Statement model
 */
public class PadletParser {
    
    private final String padletApiKey;
    private final HttpClient httpClient;
    private final Gson gson;
    
    public PadletParser(String padletApiKey) {
        this.padletApiKey = padletApiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }
    
    /**
     * Fetch categorized statements from Padlet with sections and weights
     * Returns same structure as DelphiCsvParser for unified processing
     */
    public Map<String, List<Statement>> fetchCategorizedStatements(String padletId) throws Exception {
        String url = "https://api.padlet.dev/v1/boards/" + padletId + "?include=posts,sections";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-API-KEY", padletApiKey)
                .header("Accept", "application/vnd.api+json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch from Padlet: " + response.body());
        }

        JsonObject responseBody = gson.fromJson(response.body(), JsonObject.class);
        JsonArray included = responseBody.getAsJsonArray("included");

        // First, build a map of section IDs to section names
        Map<String, String> sectionMap = new HashMap<>();
        if (included != null) {
            for (int i = 0; i < included.size(); i++) {
                JsonObject item = included.get(i).getAsJsonObject();
                if (item.get("type").getAsString().equals("section")) {
                    String sectionId = item.get("id").getAsString();
                    JsonObject attributes = item.getAsJsonObject("attributes");
                    String sectionName = attributes.get("headline").getAsString();
                    sectionMap.put(sectionId, sectionName);
                }
            }
        }

        // Now process posts
        Map<String, List<Statement>> categorized = new HashMap<>();
        
        if (included != null) {
            for (int i = 0; i < included.size(); i++) {
                JsonObject post = included.get(i).getAsJsonObject();

                if (post.get("type").getAsString().equals("post")) {
                    JsonObject attributes = post.getAsJsonObject("attributes");
                    
                    // Extract body
                    JsonObject content = attributes.getAsJsonObject("content");
                    String bodyHtml = content.get("bodyHtml").getAsString();
                    String plainText = bodyHtml.replaceAll("<[^>]*>", " ").trim();
                    
                    // Extract section (category)
                    JsonObject relationships = post.getAsJsonObject("relationships");
                    String category = "Uncategorized";
                    
                    if (relationships != null && relationships.has("section")) {
                        JsonObject section = relationships.getAsJsonObject("section");
                        JsonObject sectionData = section.getAsJsonObject("data");
                        if (sectionData != null && !sectionData.isJsonNull()) {
                            String sectionId = sectionData.get("id").getAsString();
                            category = sectionMap.getOrDefault(sectionId, "Uncategorized");
                        }
                    }
                    
                    // Extract weight (score, upvotes, downvotes)
                    int score = attributes.has("score") ? attributes.get("score").getAsInt() : 0;
                    int upvotes = attributes.has("upvotes") ? attributes.get("upvotes").getAsInt() : 0;
                    int downvotes = attributes.has("downvotes") ? attributes.get("downvotes").getAsInt() : 0;
                    
                    // Calculate total weight
                    int weight = score + upvotes - downvotes;
                    
                    // Create comment with weight info
                    String comment = "Weight: " + weight + " (Score: " + score + ", ↑" + upvotes + ", ↓" + downvotes + ")";
                    
                    // Create Statement
                    Statement statement = new Statement(plainText, category, weight, comment);
                    
                    // Add to categorized map
                    categorized.computeIfAbsent(category, k -> new ArrayList<>()).add(statement);
                }
            }
        }

        System.out.println("📊 Fetched from Padlet:");
        for (Map.Entry<String, List<Statement>> entry : categorized.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue().size() + " statements");
        }

        return categorized;
    }
    
    /**
     * Fetch simple list of statements (for DSC workflow without categories)
     */
    public List<String> fetchStatements(String padletId) throws Exception {
        String url = "https://api.padlet.dev/v1/boards/" + padletId + "?include=posts";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-API-KEY", padletApiKey)
                .header("Accept", "application/vnd.api+json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch from Padlet: " + response.body());
        }

        JsonObject responseBody = gson.fromJson(response.body(), JsonObject.class);
        JsonArray included = responseBody.getAsJsonArray("included");

        List<String> statements = new ArrayList<>();
        if (included != null) {
            for (int i = 0; i < included.size(); i++) {
                JsonObject post = included.get(i).getAsJsonObject();

                if (post.get("type").getAsString().equals("post")) {
                    JsonObject attributes = post.getAsJsonObject("attributes");
                    JsonObject content = attributes.getAsJsonObject("content");
                    String bodyHtml = content.get("bodyHtml").getAsString();

                    String plainText = bodyHtml.replaceAll("<[^>]*>", " ").trim();
                    statements.add(plainText);
                }
            }
        }

        return statements;
    }
}
