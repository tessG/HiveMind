package io.github.tessG;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * Miro board builder using shapes to create a card-based design matching the HTML
 */
public class MiroBoardBuilder {

    private static final String MIRO_API_BASE = "https://api.miro.com/v2";
    private final String accessToken;
    private final HttpClient httpClient;
    private final Gson gson;

    // Color scheme matching HTML design
    private static final String COLOR_DARE = "#f56565";
    private static final String COLOR_SHARE = "#48bb78";
    private static final String COLOR_CARE = "#4299e1";
    private static final String COLOR_HEADER = "#667eea";
    private static final String COLOR_SUMMARY = "#764ba2";

    // Layout - card-based design
    private static final double START_X = -1000;
    private static final double COLUMN_WIDTH = 700;
    private static final double START_Y = 400;
    private static final double CARD_HEIGHT = 180;
    private static final double CARD_SPACING = 20;
    private static final double CARD_WIDTH = 600;

    public MiroBoardBuilder(String accessToken) {
        this.accessToken = accessToken;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    /**
     * Creates the insights board with card design
     */
    public String createInsightsBoard(StudentInsights insights, String boardName) throws Exception {
        String boardId = createBoard(boardName);
        System.out.println("✅ Created board: " + boardId);

        // Calculate total dimensions for the frame
        double maxItems = Math.max(Math.max(
                        insights.getDareItems().size(),
                        insights.getShareItems().size()),
                insights.getCareItems().size());
        double totalHeight = 800 + (maxItems * (CARD_HEIGHT + CARD_SPACING));

        createBackgroundFrame(boardId, 0, totalHeight/2 - 200, 3200, totalHeight);

        // Create header
        createHeaderShape(boardId, insights.getMainHeadline(), 0, -100);
        createTextItem(boardId, "Klassens værdier og fælles mål", 0, 50, 2500, 50);

        // Create three columns with card backgrounds
        createCardColumn(boardId, "🔥 DARE", insights.getDareItems(), START_X, START_Y, COLOR_DARE);
        createCardColumn(boardId, "🤝 SHARE", insights.getShareItems(), START_X + COLUMN_WIDTH, START_Y, COLOR_SHARE);
        createCardColumn(boardId, "💙 CARE", insights.getCareItems(), START_X + (COLUMN_WIDTH * 2), START_Y, COLOR_CARE);

        // Create summary section
        maxItems = Math.max(Math.max(
                        insights.getDareItems().size(),
                        insights.getShareItems().size()),
                insights.getCareItems().size());
        double summaryY = START_Y + (maxItems * (CARD_HEIGHT + CARD_SPACING)) + 300;

        createSummarySection(boardId, insights.getSummary(), insights.getKeyInsight(), summaryY);

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

    private void createHeaderShape(String boardId, String content, double x, double y) throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("content", "<p><strong>" + content + "</strong></p>");
        data.addProperty("shape", "rectangle");

        JsonObject style = new JsonObject();
        style.addProperty("fillColor", COLOR_HEADER);
        style.addProperty("color", "#ffffff");
        style.addProperty("fontSize", "80");
        style.addProperty("textAlign", "center");
        style.addProperty("textAlignVertical", "middle");
        style.addProperty("borderWidth", "1.1");

        JsonObject geometry = new JsonObject();
        geometry.addProperty("width", 3000);
        geometry.addProperty("height", 220);

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

    private void createCardColumn(String boardId, String title, List<String> items,
                                  double x, double y, String borderColor) throws Exception {
        // Column header as shape
        JsonObject headerData = new JsonObject();
        headerData.addProperty("content", "<p><strong>" + title + "</strong></p>");
        headerData.addProperty("shape", "rectangle");

        JsonObject headerStyle = new JsonObject();
        headerStyle.addProperty("fillColor", "#ffffff");
        headerStyle.addProperty("fillOpacity", "1.0");
        headerStyle.addProperty("color", "#2d3748");
        headerStyle.addProperty("fontSize", "28");
        headerStyle.addProperty("textAlign", "center");
        headerStyle.addProperty("textAlignVertical", "middle");
        headerStyle.addProperty("borderColor", borderColor);
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
            createCard(boardId, item, x, currentY, borderColor);
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

    private void createSummarySection(String boardId, String summary, String keyInsight, double y) throws Exception {
        // Summary title
        createTextItem(boardId, "📝 Sammenfatning", 0, y - 50, 3000, 70);

        // Summary box
        createCard(boardId, summary, 0, y + 50, COLOR_SUMMARY);

        // Key insight box
        createCard(boardId, "💡 " + keyInsight, 0, y + 270, COLOR_SUMMARY);
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
    private void createBackgroundFrame(String boardId, double x, double y, double width, double height) throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("title", "Student Insights");
        data.addProperty("type", "freeform");

        JsonObject style = new JsonObject();
        style.addProperty("fillColor", "#f7fafc");  // Light gray background

        JsonObject geometry = new JsonObject();
        geometry.addProperty("width", width);
        geometry.addProperty("height", height);

        JsonObject position = new JsonObject();
        position.addProperty("x", x);
        position.addProperty("y", y);

        JsonObject requestBody = new JsonObject();
        requestBody.add("data", data);
        requestBody.add("style", style);
        requestBody.add("geometry", geometry);
        requestBody.add("position", position);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MIRO_API_BASE + "/boards/" + boardId + "/frames"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 201) {
            throw new RuntimeException("Failed to create frame: " + response.body());
        }
    }
}