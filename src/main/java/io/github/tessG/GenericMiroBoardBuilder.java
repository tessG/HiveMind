package io.github.tessG;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * Generic Miro board builder that works with any evaluation configuration
 */
public class GenericMiroBoardBuilder {
    
    private static final String MIRO_API_BASE = "https://api.miro.com/v2";
    private final String accessToken;
    private final HttpClient httpClient;
    private final Gson gson;
    
    // Layout constants
    private static final double START_Y = 400;
    private static final double CARD_HEIGHT = 180;
    private static final double CARD_SPACING = 20;
    private static final double CARD_WIDTH = 600;
    
    public GenericMiroBoardBuilder(String accessToken) {
        this.accessToken = accessToken;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }
    
    /**
     * Creates an insights board for any evaluation type
     */
    public String createInsightsBoard(EvaluationInsights insights, 
                                     EvaluationConfig config,
                                     String boardName) throws Exception {
        String boardId = createBoard(boardName);
        System.out.println("✅ Created board: " + boardId);
        
        // Calculate layout based on number of categories
        int categoryCount = config.getCategoryCount();
        double columnWidth = 700;
        double totalWidth = categoryCount * columnWidth;
        double startX = -(totalWidth / 2) + (columnWidth / 2);
        
        // Create header
        createHeaderShape(boardId, insights.getHeadline(), 0, -100, config.getHeaderColor());
        createTextItem(boardId, config.getTitle(), 0, 50, totalWidth * 0.8, 20);
        
        // Create columns dynamically
        for (int i = 0; i < config.getCategories().size(); i++) {
            Category category = config.getCategories().get(i);
            double x = startX + (i * columnWidth);
            List<String> items = insights.getItemsForCategory(category.getName());
            
            if (items != null && !items.isEmpty()) {
                createCardColumn(boardId, category, items, x, START_Y);
            }
        }
        
        // Create summary section
        double summaryY = START_Y + (insights.getMaxItemsInAnyCategory() * (CARD_HEIGHT + CARD_SPACING)) + 300;
        createSummarySection(boardId, insights.getSummary(), insights.getKeyInsight(), 
                           summaryY, config.getSummaryColor());
        if (insights.getHumorousStatement() != null && !insights.getHumorousStatement().isEmpty()) {
            createHumorousCard(boardId, insights.getHumorousStatement(), summaryY + 500);
        }
        System.out.println("✅ Board complete!");
        return boardId;
    }
    
    private String createBoard(String name) throws Exception {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("name", name);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(MIRO_API_BASE + "/boards"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 201) {
            throw new RuntimeException("Failed to create board: " + response.body());
        }
        
        return gson.fromJson(response.body(), JsonObject.class).get("id").getAsString();
    }
    
    private void createHeaderShape(String boardId, String content, double x, double y, 
                                   String color) throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("content", "<p><strong>" + content + "</strong></p>");
        data.addProperty("shape", "rectangle");
        
        JsonObject style = new JsonObject();
        style.addProperty("fillColor", color);
        style.addProperty("fillOpacity", "0.9");
        style.addProperty("color", "#ffffff");
        style.addProperty("fontSize", "32");
        style.addProperty("textAlign", "center");
        style.addProperty("textAlignVertical", "middle");
        style.addProperty("borderWidth", "0");
        
        JsonObject geometry = new JsonObject();
        geometry.addProperty("width", 3000);
        geometry.addProperty("height", 200);
        
        JsonObject position = new JsonObject();
        position.addProperty("x", x);
        position.addProperty("y", y);
        
        JsonObject requestBody = new JsonObject();
        requestBody.add("data", data);
        requestBody.add("style", style);
        requestBody.add("geometry", geometry);
        requestBody.add("position", position);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(MIRO_API_BASE + "/boards/" + boardId + "/shapes"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 201) {
            throw new RuntimeException("Failed to create header: " + response.body());
        }
    }
    
    private void createCardColumn(String boardId, Category category, List<String> items, 
                                  double x, double y) throws Exception {
        // Column header
        JsonObject headerData = new JsonObject();
        headerData.addProperty("content", "<p><strong>" + category.getDisplayName() + "</strong></p>");
        headerData.addProperty("shape", "rectangle");
        
        JsonObject headerStyle = new JsonObject();
        headerStyle.addProperty("fillColor", "#ffffff");
        headerStyle.addProperty("fillOpacity", "1.0");
        headerStyle.addProperty("color", "#2d3748");
        headerStyle.addProperty("fontSize", "28");
        headerStyle.addProperty("textAlign", "center");
        headerStyle.addProperty("textAlignVertical", "middle");
        headerStyle.addProperty("borderColor", category.getColor());
        headerStyle.addProperty("borderWidth", "4");
        headerStyle.addProperty("borderStyle", "normal");
        
        JsonObject headerGeometry = new JsonObject();
        headerGeometry.addProperty("width", CARD_WIDTH);
        headerGeometry.addProperty("height", 100);
        
        JsonObject headerPosition = new JsonObject();
        headerPosition.addProperty("x", x);
        headerPosition.addProperty("y", y - 100);
        
        JsonObject headerBody = new JsonObject();
        headerBody.add("data", headerData);
        headerBody.add("style", headerStyle);
        headerBody.add("geometry", headerGeometry);
        headerBody.add("position", headerPosition);
        
        HttpRequest headerRequest = HttpRequest.newBuilder()
            .uri(URI.create(MIRO_API_BASE + "/boards/" + boardId + "/shapes"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(headerBody)))
            .build();
        
        httpClient.send(headerRequest, HttpResponse.BodyHandlers.ofString());
        
        // Create cards for each item
        double currentY = y + 50;
        for (String item : items) {
            createCard(boardId, item, x, currentY, category.getColor());
            currentY += CARD_HEIGHT + CARD_SPACING;
        }
    }
    
    private void createCard(String boardId, String content, double x, double y, 
                           String borderColor) throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("content", "<p>" + content + "</p>");
        data.addProperty("shape", "rectangle");
        
        JsonObject style = new JsonObject();
        style.addProperty("fillColor", "#ffffff");
        style.addProperty("fillOpacity", "1.0");
        style.addProperty("color", "#4a5568");
        style.addProperty("fontSize", "14");
        style.addProperty("textAlign", "left");
        style.addProperty("textAlignVertical", "middle");
        style.addProperty("borderColor", borderColor);
        style.addProperty("borderWidth", "3");
        style.addProperty("borderStyle", "normal");
        
        JsonObject geometry = new JsonObject();
        geometry.addProperty("width", CARD_WIDTH);
        geometry.addProperty("height", CARD_HEIGHT);
        
        JsonObject position = new JsonObject();
        position.addProperty("x", x);
        position.addProperty("y", y);
        
        JsonObject requestBody = new JsonObject();
        requestBody.add("data", data);
        requestBody.add("style", style);
        requestBody.add("geometry", geometry);
        requestBody.add("position", position);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(MIRO_API_BASE + "/boards/" + boardId + "/shapes"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 201) {
            throw new RuntimeException("Failed to create card: " + response.body());
        }
    }
    
    private void createSummarySection(String boardId, String summary, String keyInsight, 
                                     double y, String color) throws Exception {
        createTextItem(boardId, "📝 Sammenfatning", 0, y - 50, 800, 24);
        createCard(boardId, summary, 0, y + 50, color);
        createCard(boardId, "💡 " + keyInsight, 0, y + 270, color);
    }
    
    private void createTextItem(String boardId, String content, double x, double y, 
                               double width, int fontSize) throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("content", content);
        
        JsonObject style = new JsonObject();
        style.addProperty("fontSize", String.valueOf(fontSize));
        style.addProperty("textAlign", "center");
        
        JsonObject geometry = new JsonObject();
        geometry.addProperty("width", width);
        
        JsonObject position = new JsonObject();
        position.addProperty("x", x);
        position.addProperty("y", y);
        
        JsonObject requestBody = new JsonObject();
        requestBody.add("data", data);
        requestBody.add("style", style);
        requestBody.add("geometry", geometry);
        requestBody.add("position", position);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(MIRO_API_BASE + "/boards/" + boardId + "/texts"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 201) {
            throw new RuntimeException("Failed to create text: " + response.body());
        }
    }
    private void createHumorousCard(String boardId, String statement, double y) throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("content", "<p><strong>😄 " + statement + "</strong></p>");
        data.addProperty("shape", "rectangle");

        JsonObject style = new JsonObject();
        style.addProperty("fillColor", "#fef5e7");  // Light yellow
        style.addProperty("fillOpacity", "1.0");
        style.addProperty("color", "#744210");
        style.addProperty("fontSize", "18");
        style.addProperty("textAlign", "center");
        style.addProperty("textAlignVertical", "middle");
        style.addProperty("borderColor", "#f59e0b");  // Orange border
        style.addProperty("borderWidth", "4");
        style.addProperty("borderStyle", "normal");

        JsonObject geometry = new JsonObject();
        geometry.addProperty("width", 800);
        geometry.addProperty("height", 150);

        JsonObject position = new JsonObject();
        position.addProperty("x", 0);
        position.addProperty("y", y);

        JsonObject requestBody = new JsonObject();
        requestBody.add("data", data);
        requestBody.add("style", style);
        requestBody.add("geometry", geometry);
        requestBody.add("position", position);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MIRO_API_BASE + "/boards/" + boardId + "/shapes"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 201) {
            throw new RuntimeException("Failed to create humorous card: " + response.body());
        }
    }
}
