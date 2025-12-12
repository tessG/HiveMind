package io.github.tessG;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Message;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic workflow that works with any evaluation type
 */
public class GenericEvaluationWorkflow {
    
    private final String padletApiKey;
    private final String miroAccessToken;
    private final HttpClient httpClient;
    private final Gson gson;
    
    public GenericEvaluationWorkflow(String padletApiKey, String miroAccessToken) {
        this.padletApiKey = padletApiKey;
        this.miroAccessToken = miroAccessToken;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }
    
    /**
     * Execute workflow for any evaluation type
     */
   /* public String executeWorkflow(String padletId, String evaluationType) throws Exception {
        System.out.println("🔄 Starting workflow for: " + evaluationType);
        
        // Get configuration for this evaluation type
        EvaluationConfig config = EvaluationConfigFactory.getConfig(evaluationType);
        
        // Step 1: Fetch statements from Padlet
        System.out.println("📥 Fetching statements from Padlet...");
        List<String> statements = fetchPadletStatements(padletId);
        System.out.println("✅ Retrieved " + statements.size() + " statements");
        
        // Step 2: Analyze with Claude
        System.out.println("🤖 Analyzing with Claude API...");
        EvaluationInsights insights = analyzeWithClaude(statements, config);
        System.out.println("✅ Analysis complete");
        
        // Step 3: Create Miro board
        System.out.println("🎨 Creating Miro board...");
        GenericMiroBoardBuilder builder = new GenericMiroBoardBuilder(miroAccessToken);
        String boardId = builder.createInsightsBoard(
            insights, 
            config,
            config.getTitle() + " - " + java.time.LocalDate.now()
        );
        System.out.println("✅ Board created successfully!");

        String boardUrl = "https://miro.com/app/board/" + boardId;
        System.out.println("🔗 View at: " + boardUrl);
        
        return boardUrl;
    }*/
    public String executeWorkflow(String padletId, String evaluationType) throws Exception {
        System.out.println("🔄 Starting workflow for: " + evaluationType);

        // Get configuration
        EvaluationConfig config = EvaluationConfigFactory.getConfig(evaluationType);

        // Fetch statements from Padlet
        System.out.println("📥 Fetching statements from Padlet...");
        List<String> statements = fetchPadletStatements(padletId);
        System.out.println("✅ Retrieved " + statements.size() + " statements");

        // Generate HTML poster with Claude
        System.out.println("🎨 Generating poster with Claude API...");
        String htmlPoster = generatePosterWithClaude(statements, config);
        System.out.println("✅ Poster generated");

        // Save HTML to file
        String filename = "poster-" + evaluationType + "-" + java.time.LocalDate.now() + ".html";
        java.nio.file.Files.writeString(
                java.nio.file.Paths.get(filename),
                htmlPoster
        );

        System.out.println("💾 Saved to: " + filename);
        return filename;
    }

    private String generatePosterWithClaude(List<String> statements,
                                            EvaluationConfig config) throws Exception {
        AnthropicClient client = AnthropicOkHttpClient.fromEnv();

        MessageCreateParams params = MessageCreateParams.builder()
                .model("claude-sonnet-4-5-20250929")
                .maxTokens(8000)  // Increased for HTML generation
                .addUserMessage(buildAnalysisPrompt(statements, config))
                .build();

        Message message = client.messages().create(params);
        String response = String.valueOf(message.content().get(0).text());

        // Extract HTML (remove any markdown markers if present)
        String html = response;
        html = html.replaceAll("```html\\s*", "");
        html = html.replaceAll("```\\s*", "");
        html = html.trim();

        // Ensure it starts with DOCTYPE
        if (!html.startsWith("<!DOCTYPE")) {
            int docStart = html.indexOf("<!DOCTYPE");
            if (docStart > 0) {
                html = html.substring(docStart);
            }
        }

        return html;
    }
    private List<String> fetchPadletStatements(String padletId) throws Exception {
        String url = "https://api.padlet.dev/v1/boards/" + padletId + "?include=posts";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("X-API-KEY", padletApiKey)
            .header("Accept", "application/json")
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
    
    private EvaluationInsights analyzeWithClaude(List<String> statements, 
                                                 EvaluationConfig config) throws Exception {
        AnthropicClient client = AnthropicOkHttpClient.fromEnv();
        
        MessageCreateParams params = MessageCreateParams.builder()
            .model("claude-sonnet-4-5-20250929")
            .maxTokens(4000)
            .addUserMessage(buildAnalysisPrompt(statements, config))
            .build();
        
        Message message = client.messages().create(params);
        String analysisText = String.valueOf(message.content().get(0).text());
        
        return parseClaudeAnalysis(analysisText, config);
    }

    private String buildAnalysisPrompt(List<String> statements, EvaluationConfig config) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyser følgende udsagn fra studerende.\n\n");
        prompt.append("Kategoriser udsagnene i disse kategorier:\n");

        for (Category category : config.getCategories()) {
            prompt.append("- ").append(category.getName()).append(" (").append(category.getEmoji()).append(")\n");
        }

        prompt.append("\nUdsagn:\n");
        for (int i = 0; i < statements.size(); i++) {
            prompt.append((i + 1)).append(". ").append(statements.get(i)).append("\n");
        }

        prompt.append("\nLav en struktureret analyse og GENERER EN KOMPLET HTML POSTER.\n\n");
        prompt.append("Inkluder:\n");
        prompt.append("1. En overordnet headline der samler budskabet\n");
        prompt.append("2. Kategoriserede punkter under hver kategori\n");
        prompt.append("3. En sammenfatning\n");
        prompt.append("4. En nøgleindsigt\n");
        prompt.append("5. Find et humoristisk udsagn og placer det i sin egen fremhævet boks\n\n");

        prompt.append("VIGTIG: Generer en KOMPLET, STANDALONE HTML fil med:\n");
        prompt.append("- All CSS inline i <style> tags\n");
        prompt.append("- Moderne, professionelt design inspireret af den vedlagte stil\n");
        prompt.append("- Responsive layout der virker på alle skærme\n");
        prompt.append("- Farver: ");

        for (Category category : config.getCategories()) {
            prompt.append(category.getName()).append("=").append(category.getColor()).append(", ");
        }

        prompt.append("\n- Header color: ").append(config.getHeaderColor());
        prompt.append("\n- Summary color: ").append(config.getSummaryColor());
        prompt.append("\n\n");

        prompt.append("Design retningslinjer:\n");
        prompt.append("- Hver kategori som en kolonne med colored border cards\n");
        prompt.append("- Hvide kort med subtile skygger\n");
        prompt.append("- Category headers med emoji og farvet top border\n");
        prompt.append("- Gradient header sektion\n");
        prompt.append("- Summary box nederst med border i summary color\n");
        prompt.append("- Humoristisk udsagn i gul/orange highlighted box\n");
        prompt.append("- God spacing og læsbarhed\n");
        prompt.append("- Print-venlig (A4 eller US Letter)\n\n");

        prompt.append("Returner KUN den komplette HTML - ingen forklaring, ingen markdown markers.\n");
        prompt.append("Start direkte med <!DOCTYPE html>");

        return prompt.toString();
    }
    
    private EvaluationInsights parseClaudeAnalysis(String analysisText, 
                                                   EvaluationConfig config) {
        System.out.println("RAW Claude response: " + analysisText);
        
        // Extract JSON from markdown
        int jsonMarkerStart = analysisText.indexOf("```json");
        if (jsonMarkerStart != -1) {
            analysisText = analysisText.substring(jsonMarkerStart);
        }
        
        int jsonMarkerEnd = analysisText.lastIndexOf("```");
        if (jsonMarkerEnd != -1 && jsonMarkerEnd > jsonMarkerStart) {
            analysisText = analysisText.substring(0, jsonMarkerEnd);
        }
        
        analysisText = analysisText.replaceAll("```json\\s*", "");
        analysisText = analysisText.replaceAll("```", "");
        analysisText = analysisText.replaceAll("\\{citations=.*?type=text.*?\\}", "");
        analysisText = analysisText.replaceAll(", type=text, additionalProperties=\\{\\}\\}", "");
        
        String jsonText = analysisText.trim();
        System.out.println("Cleaned JSON: " + jsonText);
        
        try {
            JsonObject analysis = gson.fromJson(jsonText, JsonObject.class);
            
            String headline = analysis.get("headline").getAsString();
            String summary = analysis.get("summary").getAsString();
            String keyInsight = analysis.get("keyInsight").getAsString();
            String humorousStatement = analysis.has("humorousStatement")
                    ? analysis.get("humorousStatement").getAsString()
                    : null;
            // Extract items for each category dynamically
            Map<String, List<String>> categorizedItems = new HashMap<>();
            for (Category category : config.getCategories()) {
                JsonArray items = analysis.getAsJsonArray(category.getName());
                if (items != null) {
                    categorizedItems.put(category.getName(), jsonArrayToList(items));
                } else {
                    categorizedItems.put(category.getName(), new ArrayList<>());
                }
            }
            
            return new EvaluationInsights(headline, categorizedItems, summary, keyInsight,humorousStatement);
            
        } catch (Exception e) {
            System.err.println("Parse error. Cleaned text was:");
            System.err.println(jsonText);
            throw new RuntimeException("Failed to parse: " + e.getMessage(), e);
        }
    }
    
    private List<String> jsonArrayToList(JsonArray jsonArray) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            list.add(jsonArray.get(i).getAsString());
        }
        return list;
    }
    
    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        String padletApiKey = System.getenv("PADLET_API_KEY");
        String miroToken = System.getenv("MIRO_ACCESS_TOKEN");
        String padletId = "wmzx0ad7z03mqxia";//wmzx0ad7z03mqxia

        String evaluationType = "dare-share-care"; // or "delphi" or "retrospective"
        
        GenericEvaluationWorkflow workflow = new GenericEvaluationWorkflow(
            padletApiKey, 
            miroToken
        );
        
        try {
            String boardUrl = workflow.executeWorkflow(padletId, evaluationType);
            System.out.println("\n✨ Workflow completed successfully!");
            System.out.println("📊 Board URL: " + boardUrl);
            
        } catch (Exception e) {
            System.err.println("❌ Workflow failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
