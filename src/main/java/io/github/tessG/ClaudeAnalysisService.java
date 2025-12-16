package io.github.tessG;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.util.*;

/**
 * Service for all Claude API analysis calls
 * Shared by both Delphi and DSC workflows
 */
public class ClaudeAnalysisService {
    
    /**
     * Analyze statements for summary, headline, key insight, funny statement
     * SHARED by both Delphi and DSC
     */
    public static Map<String, String> analyzeSummaryAndInsights(List<String> statements) throws Exception {
        AnthropicClient client = AnthropicOkHttpClient.fromEnv();
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyser disse student-evalueringer og giv mig:\n\n");
        prompt.append("Udsagn:\n");
        for (int i = 0; i < statements.size(); i++) {
            prompt.append((i + 1)).append(". ").append(statements.get(i)).append("\n");
        }
        prompt.append("\nReturner KUN valid JSON (ingen forklaring, ingen markdown):\n");
        prompt.append("{\n");
        prompt.append("  \"headline\": \"En kort sætning (8-12 ord) der fanger essensen af feedbacken\",\n");
        prompt.append("  \"summary\": \"2-3 sætninger der opsummerer hovedtendenserne\",\n");
        prompt.append("  \"keyInsight\": \"En vigtig indsigt eller observation\",\n");
        prompt.append("  \"funnyStatement\": \"Det mest kontroversielle, sjove eller provokerende udsagn fra listen\"\n");
        prompt.append("}\n\n");
        prompt.append("Start direkte med { og slut med }");
        
        MessageCreateParams params = MessageCreateParams.builder()
            .model("claude-sonnet-4-5-20250929")
            .maxTokens(1000)
            .addUserMessage(prompt.toString())
            .build();
        
        Message message = client.messages().create(params);
        String response = String.valueOf(message.content().get(0).text());
        
        // Clean response
        response = cleanJsonResponse(response);
        
        // Parse JSON
        Gson gson = new GsonBuilder().setLenient().create();
        JsonObject json = gson.fromJson(response, JsonObject.class);
        
        Map<String, String> analysis = new HashMap<>();
        analysis.put("headline", json.get("headline").getAsString());
        analysis.put("summary", json.get("summary").getAsString());
        analysis.put("keyInsight", json.get("keyInsight").getAsString());
        analysis.put("funnyStatement", json.get("funnyStatement").getAsString());
        
        return analysis;
    }
    
    /**
     * Detect contradictions between statements
     * DELPHI ONLY
     */
    public static List<Map<String, Object>> detectContradictions(
            Map<String, List<Statement>> categorized) throws Exception {
        
        AnthropicClient client = AnthropicOkHttpClient.fromEnv();
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyser disse student-evalueringer for MODSÆTNINGER og SPÆNDINGER.\n\n");
        prompt.append("Find udsagn der trækker i modsatte retninger eller skaber dilemmaer.\n\n");
        
        prompt.append("Udsagn:\n");
        int counter = 1;
        for (Map.Entry<String, List<Statement>> entry : categorized.entrySet()) {
            for (Statement stmt : entry.getValue()) {
                prompt.append(counter++).append(". ").append(stmt.getText()).append("\n");
            }
        }
        
        prompt.append("\nFind 5-8 modsætningspar hvor:\n");
        prompt.append("- Udsagnene trækker i forskellige retninger\n");
        prompt.append("- De skaber undervisningsmæssige dilemmaer\n");
        prompt.append("- De repræsenterer fundamentale spændinger\n\n");
        
        prompt.append("Returner KUN valid JSON (ingen forklaring, ingen markdown):\n");
        prompt.append("{\n");
        prompt.append("  \"contradictions\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"statement1\": \"Præcis tekst fra udsagn A\",\n");
        prompt.append("      \"statement2\": \"Præcis tekst fra udsagn B\",\n");
        prompt.append("      \"tension\": 0.85,\n");
        prompt.append("      \"theme\": \"Kort tema-navn\",\n");
        prompt.append("      \"explanation\": \"Kort forklaring\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        prompt.append("Start direkte med { og slut med }");
        
        MessageCreateParams params = MessageCreateParams.builder()
            .model("claude-sonnet-4-5-20250929")
            .maxTokens(2000)
            .addUserMessage(prompt.toString())
            .build();
        
        Message message = client.messages().create(params);
        String response = String.valueOf(message.content().get(0).text());
        
        response = cleanJsonResponse(response);
        
        Gson gson = new GsonBuilder().setLenient().create();
        JsonObject json = gson.fromJson(response, JsonObject.class);
        JsonArray contradictionsArray = json.getAsJsonArray("contradictions");
        
        List<Map<String, Object>> contradictions = new ArrayList<>();
        for (int i = 0; i < contradictionsArray.size(); i++) {
            JsonObject contra = contradictionsArray.get(i).getAsJsonObject();
            
            Map<String, Object> contradiction = new HashMap<>();
            contradiction.put("statement1", contra.get("statement1").getAsString());
            contradiction.put("statement2", contra.get("statement2").getAsString());
            contradiction.put("tension", contra.get("tension").getAsDouble());
            contradiction.put("theme", contra.get("theme").getAsString());
            contradiction.put("explanation", contra.get("explanation").getAsString());
            
            contradictions.add(contradiction);
        }
        
        return contradictions;
    }
    
    /**
     * Generate teacher suggestions/recommendations
     * DELPHI ONLY (but could be used for DSC too)
     */
    public static List<String> generateSuggestions(List<String> statements) throws Exception {
        AnthropicClient client = AnthropicOkHttpClient.fromEnv();
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Baseret på disse student-evalueringer, generer konkrete anbefalinger til underviserne.\n\n");
        prompt.append("Udsagn:\n");
        for (int i = 0; i < statements.size(); i++) {
            prompt.append((i + 1)).append(". ").append(statements.get(i)).append("\n");
        }
        
        prompt.append("\nGenerer 4-6 KONKRETE, HANDLINGSORIENTEREDE anbefalinger.\n\n");
        
        prompt.append("Returner KUN valid JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"suggestions\": [\n");
        prompt.append("    \"Konkret anbefaling 1\",\n");
        prompt.append("    \"Konkret anbefaling 2\"\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        prompt.append("Start direkte med { og slut med }");
        
        MessageCreateParams params = MessageCreateParams.builder()
            .model("claude-sonnet-4-5-20250929")
            .maxTokens(1500)
            .addUserMessage(prompt.toString())
            .build();
        
        Message message = client.messages().create(params);
        String response = String.valueOf(message.content().get(0).text());
        
        response = cleanJsonResponse(response);
        
        Gson gson = new GsonBuilder().setLenient().create();
        JsonObject json = gson.fromJson(response, JsonObject.class);
        JsonArray suggestionsArray = json.getAsJsonArray("suggestions");
        
        List<String> suggestions = new ArrayList<>();
        for (int i = 0; i < suggestionsArray.size(); i++) {
            suggestions.add(suggestionsArray.get(i).getAsString());
        }
        
        return suggestions;
    }
    
    /**
     * Clean Claude JSON response - remove Optional wrapper, markdown, etc.
     */
    private static String cleanJsonResponse(String response) {
        // Extract from Optional wrapper if present
        if (response.startsWith("Optional[")) {
            int textStart = response.indexOf("text=");
            if (textStart != -1) {
                response = response.substring(textStart + 5);
                int lastBracket = response.lastIndexOf("}]");
                if (lastBracket != -1) {
                    response = response.substring(0, lastBracket);
                }
            }
        }
        
        response = response.trim();
        response = response.replaceAll("```json\\s*", "");
        response = response.replaceAll("```\\s*", "");
        response = response.replaceAll(", type=text.*", "");
        response = response.trim();
        
        // Find JSON boundaries
        int jsonStart = response.indexOf('{');
        if (jsonStart > 0) {
            response = response.substring(jsonStart);
        }
        
        // Count braces to find real end
        int braceCount = 0;
        int jsonEnd = -1;
        for (int i = 0; i < response.length(); i++) {
            if (response.charAt(i) == '{') braceCount++;
            if (response.charAt(i) == '}') {
                braceCount--;
                if (braceCount == 0) {
                    jsonEnd = i;
                    break;
                }
            }
        }
        if (jsonEnd != -1 && jsonEnd < response.length() - 1) {
            response = response.substring(0, jsonEnd + 1);
        }
        
        return response;
    }
}
