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
        prompt.append("1. En titel 'Student Vibes' og en subtitle sætning som anvender 3 vigtige nøgleord\n");
        prompt.append("2. En wordcloud (MINIMUM 20 ORD) på baggrund af alle udsagnene. Hvid baggrund, Gul, Rød eller Grøn font med sort outline/slagsskygge\n");
        prompt.append("3. rundt om wordclouden placeres ALLE udsagn i talebobler, træk pil hen til det ord i wordclouden som udsagnet relaterer til.\n ");
        prompt.append("VIGTIGT: Undgå at udsagn og ord overlapper\n");
        prompt.append("4. En sammenfatning\n");
        prompt.append("5. En nøgleindsigt\n");
        prompt.append("6. Find et humoristisk udsagn og fremhæv med stor skrift. \n\n");
        prompt.append("7. Nederst kort sammenfatning for hver af de tre kategorier \n\n");
        prompt.append(" Positioner alle elementer så de ikke overlapper\n\n");
        prompt.append("  VIGTIG: Generer en KOMPLET, STANDALONE HTML fil med:\n");
        prompt.append("-  All CSS inline i <style> tags\n");
        prompt.append("-  urban aesthetics look/hip hop \n");

     /*   prompt.append("-  Farver: ");

        for (Category category : config.getCategories()) {
            prompt.append(category.getName()).append("=").append(category.getColor()).append(", ");
        }

        prompt.append("\n- Header color: ").append(config.getHeaderColor());
        prompt.append("\n- Summary color: ").append(config.getSummaryColor());
        prompt.append("\n\n");
*/
        prompt.append("Design retningslinjer:\n");
        prompt.append("- Hvide kort med subtile skygger\n");
        prompt.append("- Category headers med emoji og farvet top border\n");
        prompt.append("- Summary box nederst med border i summary color\n");
        prompt.append("- Humoristisk udsagn placeres i header ved siden af titel, i 'parental advisory explicit lyrics'-stil badge. Positioner skævt til højre for titel. Overskrift 'Student Wisdom' \n");
        prompt.append("- God spacing og læsbarhed mellem alle elementer\n");
        prompt.append("- Brug ikoner/illustrationer der matcher urban aesthetics/hip hop temaet\n");
        prompt.append("- Print-venlig (A4)\n\n");
        prompt.append(" DESIGN STYLE (match this exactly):\n");
        prompt.append("- Infographic poster layout \n");
        prompt.append("- 3 cards with category summaries on white background, colored header bar, icons/illustrations\n");
        prompt.append("- Subtle shadows on cards\n");
        prompt.append("- No gradients\n");
        prompt.append("- Icons (ingen microfoner) and simple graphics in each section\n");
        prompt.append("- Color-coded sections with consistent palette. black, gray, yellow, red, green\n ");
        prompt.append("- Use comic book style font for ALL the statements\n ");
        prompt.append("- Place statements near wordcloud in speechbubles. Use comic book style font\n ");
        prompt.append("- cool colors\n");
        prompt.append("- Large readable headers, body text in paragraphs\n");
        prompt.append("- Bottom section spans full width\n\n");
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
    

}
