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
        
        // Extract text properly to avoid debug output
        String response = String.valueOf(message.content().get(0).text());


        // Extract HTML (remove any markdown markers if present)
        String html = response;
        html = html.replaceAll("```html\\s*", "");
        html = html.replaceAll("```\\s*", "");
        html = html.trim();




// Cut off everything after </html> tag
        int htmlEnd = html.indexOf("</html>");
        if (htmlEnd != -1) {
            html = html.substring(0, htmlEnd + 7);  // +7 to include "</html>"
        }



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

        prompt.append("\n=== GENERER EN KOMPLET HTML POSTER ===\n\n");
        
        prompt.append("POSTER FORMAT:\n");
        prompt.append("- Width: EXACTLY 900px (not max-width!)\n");
        prompt.append("- Height: EXACTLY 1400px\n");
        prompt.append("- White background\n");
        prompt.append("- NO EXTRA WHITESPACE - fill the space!\n");
        prompt.append("- Margins: 20px on all sides\n\n");
        
        prompt.append("LAYOUT STRUKTUR - FØLG DISSE HØJDER PRÆCIST:\n\n");
        prompt.append("1. HEADER (100px høj):\n");
        prompt.append("   - Titel: 'Student Vibes' (Bangers, 42px, centered)\n");
        prompt.append("   - Subtitle: Nøgleord (14px, centered)\n");
        prompt.append("   - Quote badge: Top-right, absolut position\n");
        prompt.append("     * Kun citatet - INGEN titel\n");
        prompt.append("     * Sort baggrund, hvid Bangers tekst (16px)\n");
        prompt.append("     * 180px bred, padding 12px, roteret 4°\n\n");
        
        prompt.append("2. WORDCLOUD + BUBBLES (650px høj - IKKE MER!):\n");
        prompt.append("   \n");
        prompt.append("   WORDCLOUD (center, 300×300px):\n");
        prompt.append("   - Placeret i midten horisontalt og vertikalt\n");
        prompt.append("   - Top 5 ord: 45-60px BOLD i centrum\n");
        prompt.append("   - Medium (8 ord): 30-40px omkring centrum\n");
        prompt.append("   - Rest (12 ord): 18-20px yderkant\n");
        prompt.append("   - Total: ~25 ord\n");
        prompt.append("   - Farver: gul/rød/grøn med sort shadow\n");
        prompt.append("   - Tæt pakket, roterede ord\n");
        prompt.append("   \n");
        prompt.append("   SPEECH BUBBLES (12-14 stykker TOTAL):\n");
        prompt.append("   VIGTIGT: Lav FÆRRE, BEDRE statements!\n");
        prompt.append("   - Cluster lignende udsagn sammen\n");
        prompt.append("   - Hver bubble repræsenterer 2-4 originale udsagn\n");
        prompt.append("   - Skriv sammenfattende statements der dækker temaet\n");
        prompt.append("   - Eksempel: I stedet for 3 bubbles om \"mere tid\", lav ÉN:\n");
        prompt.append("     \"Studerende ønsker mere tid til fordybelse og færre deadlines\"\n");
        prompt.append("   \n");
        prompt.append("   Bubble placering (3×3 grid omkring wordcloud):\n");
        prompt.append("   - Top: 4-5 bubbles\n");
        prompt.append("   - Sides: 8-10 bubbles hver side\n");
        prompt.append("   - Bottom: 0 bubbles\n");
        prompt.append("   - Hver bubble:\n");
        prompt.append("     * Hvid baggrund, sort 2px border, border-radius 8px\n");
        prompt.append("     * Permanent Marker, 12px\n");
        prompt.append("     * 140-180px bred (variér!)\n");
        prompt.append("     * Padding: 12px\n");
        prompt.append("     * Rotation: -6° til +6°\n");
        prompt.append("     * Subtle box-shadow\n");
        prompt.append("   - Placer semantisk nær relateret ord\n");
        prompt.append("   - FYLD RUMMET - brug hele 650px højden!\n\n");
        
        prompt.append("3. SUMMARY + CATEGORIES (450px høj):\n");
        prompt.append("   \n");
        prompt.append("   A) SAMMENFATNING (200px):\n");
        prompt.append("   - Full-width box\n");
        prompt.append("   - Titel: \"📊 Sammenfatning\" (Permanent Marker, 22px)\n");
        prompt.append("   - 2-3 sætninger om overordnede tendenser\n");
        prompt.append("   - Grå border-left (4px, #6b7280)\n");
        prompt.append("   - Background: #f9fafb\n");
        prompt.append("   - Padding: 20px\n");
        prompt.append("   - Line-height: 1.6\n");
        prompt.append("   \n");
        prompt.append("   B) 3 KATEGORI KOLONNER (250px):\n");
        prompt.append("   - Side-by-side, equal width\n");
        prompt.append("     * VIGTIGT: max-height: 160px\n\n");
        prompt.append("   - Hver kolonne:\n");
        prompt.append("     * Emoji + kategori navn (18px bold)\n");
        prompt.append("     * 2-3 sætninger beskrivelse\n");
        prompt.append("     * Farvet top-border (4px)\n");
        prompt.append("     * Background hvid\n");
        prompt.append("     * Padding: 15px\n\n");



        prompt.append("4. KEY INSIGHT (100px høj):\n");
        prompt.append("   - Full-width\n");
        prompt.append("   - Gul baggrund (#fef3c7)\n");
        prompt.append("   - Orange border-left (4px, #f59e0b)\n");
        prompt.append("   - Titel: \"💡 Nøgleindsigt\" (Permanent Marker, 20px)\n");
        prompt.append("   - 2-3 sætninger om vigtigste observation\n");
        prompt.append("   - Padding: 20px\n");
        prompt.append("   - Line-height: 1.6\n\n");
        
        prompt.append("5. FOOTER (20px høj):\n");
        prompt.append("   - Full-width\n");
        prompt.append("   - Text: 'This poster was automatically generated using AI analysis of student feedback • [dagens dato]'\n");
        prompt.append("   - Font-size: 10px\n");
        prompt.append("   - Color: #999\n");
        prompt.append("   - Text-align: center\n");
        prompt.append("   - Border-top: 1px solid #eee\n");
        prompt.append("   - Padding: 5px 0\n\n");
        
        prompt.append("TECHNICAL CSS:\n");
        prompt.append("- .poster { width: 900px; height: 1400px; margin: 0 auto; padding: 20px; background: white; }\n");
        prompt.append("- Google Fonts: 'Bangers', 'Permanent Marker'\n");
        prompt.append("- NO max-width, use exact width\n");
        prompt.append("- Use CSS Grid for layout precision\n");
        prompt.append("- Box-shadows: 0 2px 4px rgba(0,0,0,0.1)\n\n");
        
        prompt.append("KRITISK - FJERN DEBUG TEXT:\n");
        prompt.append("- INKLUDER IKKE tekst som 'type=text, additionalProperties={}'\n");
        prompt.append("- INKLUDER IKKE API debug information\n");
        prompt.append("- KUN brugervenligt indhold i HTML\n\n");
        
        prompt.append("SPACE UTILIZATION:\n");
        prompt.append("- FYLD hele poster - brug alle 1400px height\n");
        prompt.append("- INGEN store whitespace områder\n");

        prompt.append("OUTPUT:\n");
        prompt.append("Returner KUN komplet HTML.\n");
        prompt.append("- Start med <!DOCTYPE html>\n");
        prompt.append("- INGEN markdown\n");
        prompt.append("- INGEN forklaring\n");
        prompt.append("- INGEN debug text\n");
        prompt.append("- Præcis 900×1400px poster\n");

        return prompt.toString();
    }
    

    

    

}
