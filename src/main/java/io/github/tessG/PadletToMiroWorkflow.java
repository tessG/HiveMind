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
import java.util.Arrays;
import java.util.List;


/**
 * Complete workflow: Padlet → Claude Analysis → Miro Board
 * This integrates all three services
 */
public class PadletToMiroWorkflow {
    
    private final String padletApiKey;

    private final String miroAccessToken;
    private final HttpClient httpClient;
    private final Gson gson;
    
    public PadletToMiroWorkflow(String padletApiKey,String miroAccessToken) {
        this.padletApiKey = padletApiKey;
        this.miroAccessToken = miroAccessToken;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }
    
    /**
     * Complete workflow execution
     */
    public String executeWorkflow(String padletId) throws Exception {
        System.out.println("🔄 Starting workflow...");
        
        // Step 1: Fetch statements from Padlet
        System.out.println("📥 Fetching statements from Padlet...");
        List<String> statements = fetchPadletStatements(padletId);

        System.out.println("✅ Retrieved " + statements.size() + " statements");
        
        // Step 2: Analyze with Claude
        System.out.println("🤖 Analyzing with Claude API...");
        StudentInsights insights = analyzeWithClaude(statements);
        System.out.println("✅ Analysis complete");
        
        // Step 3: Create Miro board
        System.out.println("🎨 Creating Miro board...");
        MiroBoardBuilder builder = new MiroBoardBuilder(miroAccessToken);
        String boardId = builder.createInsightsBoard(
            insights, 
            "Student Insights - " + java.time.LocalDate.now()
        );
        System.out.println("✅ Board created successfully!");
        
        String boardUrl = "https://miro.com/app/board/" + boardId;
        System.out.println("🔗 View at: " + boardUrl);
        
        return boardUrl;
    }
    
    /**
     * Fetch statements from Padlet
     * Note: Adjust based on actual Padlet API structure
     */
    private List<String> fetchPadletStatements(String padletId) throws Exception {
        // Padlet API endpoint (adjust based on actual API)
        String url = "https://api.padlet.com/v1/padlets/" + padletId + "/posts";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + padletApiKey)
            .header("Accept", "application/json")
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch from Padlet: " + response.body());
        }
        
        // Parse response and extract statement text
        JsonObject responseBody = gson.fromJson(response.body(), JsonObject.class);
        JsonArray posts = responseBody.getAsJsonArray("posts");
        
        List<String> statements = new ArrayList<>();
        for (int i = 0; i < posts.size(); i++) {
            JsonObject post = posts.get(i).getAsJsonObject();
            String text = post.get("body").getAsString();
            statements.add(text);
        }
        
        return statements;
    }
    
    /**
     * Analyze statements using Claude API
     */
    private StudentInsights analyzeWithClaude(List<String> statements) throws Exception {
        // Use Anthropic SDK
        AnthropicClient client = AnthropicOkHttpClient.fromEnv();

        MessageCreateParams params = MessageCreateParams.builder()
                .model("claude-sonnet-4-5-20250929")
                .maxTokens(4000)
                .addUserMessage(buildAnalysisPrompt(statements))
                .build();

        Message message = client.messages().create(params);
        String analysisText = String.valueOf(message.content().get(0).text());

        // Parse the response
        return parseClaudeAnalysis(analysisText);
    }
    
    /**
     * Build the analysis prompt for Claude
     */
    private String buildAnalysisPrompt(List<String> statements) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyser følgende udsagn fra studerende om deres læring og klassemiljø.\n\n");
        prompt.append("Kategoriser udsagnene i tre kategorier:\n");
        prompt.append("- DARE (Vove/Turde): Ting der kræver mod og udfordrer komfortzonen\n");
        prompt.append("- SHARE (Dele): Ting der handler om at dele viden og hjælpe hinanden\n");
        prompt.append("- CARE (Omsorg): Ting der handler om at tage sig af hinanden\n\n");
        prompt.append("Udsagn:\n");
        
        for (int i = 0; i < statements.size(); i++) {
            prompt.append((i + 1)).append(". ").append(statements.get(i)).append("\n");
        }
        
        prompt.append("\nLav en struktureret analyse med:\n");
        prompt.append("1. En overordnet headline der samler budskabet\n");
        prompt.append("2. Kategoriserede punkter under DARE, SHARE og CARE\n");
        prompt.append("3. En sammenfatning\n");
        prompt.append("4. En nøgleindsigt\n\n");
        prompt.append("Format output as JSON with this structure:\n");
        prompt.append("{\n");
        prompt.append("  \"headline\": \"...\",\n");
        prompt.append("  \"dare\": [\"...\", \"...\"],\n");
        prompt.append("  \"share\": [\"...\", \"...\"],\n");
        prompt.append("  \"care\": [\"...\", \"...\"],\n");
        prompt.append("  \"summary\": \"...\",\n");
        prompt.append("  \"keyInsight\": \"...\"\n");
        prompt.append("}");
        
        return prompt.toString();
    }
    
    /**
     * Parse Claude's analysis into StudentInsights object
     */
    private StudentInsights parseClaudeAnalysis(String analysisText) {
        // Extract JSON from Claude's response (it might include explanation text)
        int jsonStart = analysisText.indexOf("{");
        int jsonEnd = analysisText.lastIndexOf("}") + 1;
        String jsonText = analysisText.substring(jsonStart, jsonEnd);
        
        JsonObject analysis = gson.fromJson(jsonText, JsonObject.class);
        
        String headline = analysis.get("headline").getAsString();
        List<String> dare = jsonArrayToList(analysis.getAsJsonArray("dare"));
        List<String> share = jsonArrayToList(analysis.getAsJsonArray("share"));
        List<String> care = jsonArrayToList(analysis.getAsJsonArray("care"));
        String summary = analysis.get("summary").getAsString();
        String keyInsight = analysis.get("keyInsight").getAsString();
        
        return new StudentInsights(headline, dare, share, care, summary, keyInsight);
    }
    
    /**
     * Helper to convert JsonArray to List<String>
     */
    private List<String> jsonArrayToList(JsonArray jsonArray) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            list.add(jsonArray.get(i).getAsString());
        }
        return list;
    }


/*
    public static void main(String[] args) {
        String miroToken = System.getenv("MIRO_ACCESS_TOKEN");

        System.out.println("Token: " + (miroToken != null ? "Found" : "NOT FOUND"));
        System.out.println("Token length: " + (miroToken != null ? miroToken.length() : 0));


        // Create sample insights
        StudentInsights insights = createSampleInsights();

        // Create board
        MiroBoardBuilder builder = new MiroBoardBuilder(miroToken);
        try {
            String boardId = builder.createInsightsBoard(insights, "HiveMind Test");
            System.out.println("🔗 https://miro.com/app/board/" + boardId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/


    /**
     * Main method for testing
*/
    public static void main(String[] args) {
        // Configuration - replace with your actual API keys
        String padletApiKey = "YOUR_PADLET_API_KEY";
        String claudeApiKey = System.getenv("MIRO_ACCESS_TOKEN");
        String miroAccessToken = System.getenv("MIRO_ACCESS_TOKEN");

        String padletId = "wmzx0ad7z03mqxia";//wmzx0ad7z03mqxia
        
        PadletToMiroWorkflow workflow = new PadletToMiroWorkflow(padletApiKey, miroAccessToken);
        
        try {
            String boardUrl = workflow.executeWorkflow(padletId);
            System.out.println("\n✨ Workflow completed successfully!");
            System.out.println("📊 Board URL: " + boardUrl);
            
        } catch (Exception e) {
            System.err.println("❌ Workflow failed: " + e.getMessage());
            e.printStackTrace();
        }
    }





    /**
     * Creates sample insights data structure
     * In your real application, this would come from Claude API analysis
     */
    private static StudentInsights createSampleInsights() {
        String headline = "Vær ikke bange for at spørge om hjælp – og skab et trygt " +
                "fællesskab hvor vi deler viden og tør fejle sammen";

        List<String> dareItems = Arrays.asList(
                "Vær mere åben og social i håbet om at få et bedre kammeratskab",
                "Blive bedre til at udfordre os selv, og hop ud i nye ting",
                "Mod til at spørge om hjælp",
                "Nysgerrighed over for hinanden",
                "Hoppe ud af vores komfortzone",
                "Slippe tvivlen og tro på egen kunnen",
                "Tage mere initiativ til at skubbe gruppen i gang",
                "Skabe et miljø, hvor man tør fejle uden at blive dømt",
                "Åbensindet tilgang til opgaverne",
                "Blive bedre til at kode generelt"
        );

        List<String> shareItems = Arrays.asList(
                "Dele vores fremskridt og deltage i en fælles rejse for udvikling",
                "Hjælp dine klassekammerater, både med skolearbejde og det sociale",
                "Tilbyde sin hjælp og dele sin viden",
                "Dele sine kompetencer og usikkerheder",
                "Have indsigt i hinandens styrker og udviklingsområder",
                "Arbejde sammen mod fælles mål",
                "Dele ens tankergang for opgaveløsning",
                "Dele vores viden og noter med hinanden",
                "Begynde gruppearbejde med forventningsafstemning",
                "Opnå højeste kompetencer ved at dele med andre"
        );

        List<String> careItems = Arrays.asList(
                "Tage os af hinanden og vise omsorg",
                "Vise tillid til klassekammerater",
                "Skabe et trygt fællesskab",
                "Spørge om andre har brug for hjælp",
                "Huske at fejre andres succes (med kage!)",
                "Være åben og tolerant",
                "Tålmodighed",
                "Støtte hinanden i opgaveløsningen",
                "Sikre inddragelse af alle gruppemedlemmer",
                "Sørge for alle er inkluderede",
                "Finde rytmen med hinanden",
                "Overholde aftaler i gruppen",
                "Lyt til sine medstuderende"
        );

        String summary = "Udsagnene afspejler et stærkt ønske om at skabe et trygt, " +
                "inkluderende og udviklende læringsmiljø. Der er en gennemgående " +
                "erkendelse af, at personlig udvikling kræver mod til at turde, " +
                "vilje til at dele og evne til at vise omsorg for hinanden.";

        String keyInsight = "Nøgleindsigt: Fællesskabet vokser når vi tør være sårbare, " +
                "deler vores viden generøst, og passer på hinanden gennem hele rejsen.";

        return new StudentInsights(headline, dareItems, shareItems, careItems,
                summary, keyInsight);
    }
}
