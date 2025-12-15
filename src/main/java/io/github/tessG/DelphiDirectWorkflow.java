package io.github.tessG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Direct workflow: CSV → Similarity Graph → HTML Poster
 * No Padlet needed, no Claude API for clustering
 */
public class DelphiDirectWorkflow {
    
    /**
     * Generate HTML poster with similarity graph from CSV
     */
    public String generatePosterFromCsv(String csvFilePath, String evaluationType) throws Exception {
        System.out.println("📄 Starting Delphi workflow from CSV");
        
        // Get configuration
        EvaluationConfig config = EvaluationConfigFactory.getConfig(evaluationType);
        
        // Parse CSV
        System.out.println("📥 Parsing CSV file...");
        DelphiCsvParser parser = new DelphiCsvParser();
        Map<String, List<DelphiCsvParser.CategorizedStatement>> categorized = parser.parseCategorizedCsv(csvFilePath);
        
        if (categorized.isEmpty()) {
            throw new RuntimeException("No statements found in CSV file");
        }
        
        parser.printSummary(categorized);
        
        // Convert to list for Claude analysis
        List<String> allStatements = new ArrayList<>();
        for (List<DelphiCsvParser.CategorizedStatement> statements : categorized.values()) {
            for (DelphiCsvParser.CategorizedStatement stmt : statements) {
                allStatements.add(stmt.getFullStatement());
            }
        }
        
        // Get analysis from Claude (headline, summary, insights, funny statement)
        System.out.println("🤖 Analyzing with Claude for summary and insights...");
        Map<String, String> analysis = analyzeWithClaude(allStatements);
        
        // Build nodes and calculate similarities
        System.out.println("🔗 Calculating similarities...");
        List<Map<String, Object>> nodes = buildNodesFromCategorized(categorized);
        List<Map<String, Object>> edges = calculateSimilarities(nodes, 0.35);  // Lower threshold for more connections
        
        System.out.println("✅ Found " + edges.size() + " connections");
        
        // Position nodes for compact poster layout
        positionNodesForPoster(nodes);
        
        // Generate poster HTML with embedded graph
        System.out.println("🎨 Generating poster with similarity graph...");
        String htmlPoster = generatePosterWithGraph(nodes, edges, config, analysis);
        System.out.println("✅ Poster generated");
        
        // Save to file
        String filename = "poster-" + evaluationType + "-" + java.time.LocalDate.now() + ".html";
        java.nio.file.Files.writeString(
            java.nio.file.Paths.get(filename),
            htmlPoster
        );
        
        System.out.println("💾 Saved to: " + filename);
        return filename;
    }
    
    private Map<String, String> analyzeWithClaude(List<String> statements) throws Exception {
        com.anthropic.client.AnthropicClient client = com.anthropic.client.okhttp.AnthropicOkHttpClient.fromEnv();
        
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
        
        com.anthropic.models.messages.MessageCreateParams params = com.anthropic.models.messages.MessageCreateParams.builder()
            .model("claude-sonnet-4-5-20250929")
            .maxTokens(1000)
            .addUserMessage(prompt.toString())
            .build();
        
        com.anthropic.models.messages.Message message = client.messages().create(params);
        String response = String.valueOf(message.content().get(0).text());
        
        System.out.println("Raw Claude response: " + response);
        
        // Clean JSON response
        response = response.trim();
        response = response.replaceAll("```json\\s*", "");
        response = response.replaceAll("```\\s*", "");
        response = response.trim();
        
        // Find JSON object boundaries
        int jsonStart = response.indexOf('{');
        int jsonEnd = response.lastIndexOf('}');
        
        if (jsonStart == -1 || jsonEnd == -1) {
            throw new RuntimeException("No valid JSON found in response: " + response);
        }
        
        String jsonStr = response.substring(jsonStart, jsonEnd + 1);
        System.out.println("Cleaned JSON: " + jsonStr);
        
        com.google.gson.Gson gson = new com.google.gson.Gson();
        com.google.gson.JsonObject json = gson.fromJson(jsonStr, com.google.gson.JsonObject.class);
        
        Map<String, String> analysis = new HashMap<>();
        analysis.put("headline", json.get("headline").getAsString());
        analysis.put("summary", json.get("summary").getAsString());
        analysis.put("keyInsight", json.get("keyInsight").getAsString());
        analysis.put("funnyStatement", json.get("funnyStatement").getAsString());
        
        return analysis;
    }
    
    private List<Map<String, Object>> buildNodesFromCategorized(
            Map<String, List<DelphiCsvParser.CategorizedStatement>> categorized) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        int id = 0;
        
        for (Map.Entry<String, List<DelphiCsvParser.CategorizedStatement>> entry : categorized.entrySet()) {
            String category = entry.getKey();
            for (DelphiCsvParser.CategorizedStatement stmt : entry.getValue()) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", id++);
                node.put("category", category);
                node.put("text", stmt.getStatement());
                node.put("comment", stmt.getComment());
                
                // Calculate box size based on text length - SMALLER for poster
                int textLen = stmt.getStatement().length();
                if (textLen < 25) {
                    node.put("width", 80);
                    node.put("height", 45);
                } else if (textLen < 40) {
                    node.put("width", 90);
                    node.put("height", 50);
                } else if (textLen < 55) {
                    node.put("width", 100);
                    node.put("height", 60);
                } else {
                    node.put("width", 110);
                    node.put("height", 65);
                }
                
                nodes.add(node);
            }
        }
        
        return nodes;
    }
    
    private List<Map<String, Object>> calculateSimilarities(List<Map<String, Object>> nodes, double threshold) {
        List<Map<String, Object>> edges = new ArrayList<>();
        
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                String text1 = (String) nodes.get(i).get("text");
                String text2 = (String) nodes.get(j).get("text");
                double sim = calculateSimilarity(text1, text2);
                
                if (sim > threshold) {
                    Map<String, Object> edge = new HashMap<>();
                    edge.put("source", i);
                    edge.put("target", j);
                    edge.put("similarity", Math.round(sim * 1000.0) / 1000.0);
                    edges.add(edge);
                }
            }
        }
        
        return edges;
    }
    
    private double calculateSimilarity(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        
        if (s1.equals(s2)) return 1.0;
        
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i-1) == s2.charAt(j-1)) {
                    dp[i][j] = dp[i-1][j-1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i-1][j-1], Math.min(dp[i-1][j], dp[i][j-1]));
                }
            }
        }
        
        int maxLen = Math.max(s1.length(), s2.length());
        return 1.0 - ((double) dp[s1.length()][s2.length()] / maxLen);
    }
    
    private void positionNodesForPoster(List<Map<String, Object>> nodes) {
        // Compact layout for poster (max 900px wide)
        List<Map<String, Object>> keepDoing = new ArrayList<>();
        List<Map<String, Object>> stopDoing = new ArrayList<>();
        List<Map<String, Object>> startDoing = new ArrayList<>();
        
        for (Map<String, Object> node : nodes) {
            String category = (String) node.get("category");
            if (category.equals("Keep Doing")) keepDoing.add(node);
            else if (category.equals("Stop Doing")) stopDoing.add(node);
            else if (category.equals("Start Doing")) startDoing.add(node);
        }
        
        // Compact spacing for poster
        int keepCols = 3;
        int keepXStart = 40;
        int keepXSpacing = 110;
        int keepYStart = 80;
        int keepYSpacing = 75;
        
        for (int i = 0; i < keepDoing.size(); i++) {
            Map<String, Object> node = keepDoing.get(i);
            node.put("x", keepXStart + (i % keepCols) * keepXSpacing);
            node.put("y", keepYStart + (i / keepCols) * keepYSpacing);
        }
        
        // Start Doing - right side
        int startCols = 3;
        int startXStart = 580;
        int startXSpacing = 110;
        int startYStart = 80;
        int startYSpacing = 75;
        
        for (int i = 0; i < startDoing.size(); i++) {
            Map<String, Object> node = startDoing.get(i);
            node.put("x", startXStart + (i % startCols) * startXSpacing);
            node.put("y", startYStart + (i / startCols) * startYSpacing);
        }
        
        // Stop Doing - bottom center
        int stopCols = 3;
        int stopXStart = 330;
        int stopXSpacing = 110;
        int stopYStart = 530;
        int stopYSpacing = 75;
        
        for (int i = 0; i < stopDoing.size(); i++) {
            Map<String, Object> node = stopDoing.get(i);
            node.put("x", stopXStart + (i % stopCols) * stopXSpacing);
            node.put("y", stopYStart + (i / stopCols) * stopYSpacing);
        }
    }
    
    private String generatePosterWithGraph(List<Map<String, Object>> nodes, 
                                          List<Map<String, Object>> edges,
                                          EvaluationConfig config,
                                          Map<String, String> analysis) {
        // Convert to JSON
        StringBuilder nodesJson = new StringBuilder("[");
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) nodesJson.append(",");
            Map<String, Object> node = nodes.get(i);
            nodesJson.append("{")
                .append("\"id\":").append(node.get("id")).append(",")
                .append("\"category\":\"").append(escapeJson((String)node.get("category"))).append("\",")
                .append("\"text\":\"").append(escapeJson((String)node.get("text"))).append("\",")
                .append("\"width\":").append(node.get("width")).append(",")
                .append("\"height\":").append(node.get("height")).append(",")
                .append("\"x\":").append(node.get("x")).append(",")
                .append("\"y\":").append(node.get("y"))
                .append("}");
        }
        nodesJson.append("]");
        
        StringBuilder edgesJson = new StringBuilder("[");
        for (int i = 0; i < edges.size(); i++) {
            if (i > 0) edgesJson.append(",");
            Map<String, Object> edge = edges.get(i);
            edgesJson.append("{")
                .append("\"source\":").append(edge.get("source")).append(",")
                .append("\"target\":").append(edge.get("target")).append(",")
                .append("\"similarity\":").append(edge.get("similarity"))
                .append("}");
        }
        edgesJson.append("]");
        
        String graphDataJson = "{\"nodes\":" + nodesJson + ",\"edges\":" + edgesJson + "}";
        
        // Build poster HTML
        return "<!DOCTYPE html>\n" +
"<html>\n" +
"<head>\n" +
"    <meta charset=\"UTF-8\">\n" +
"    <title>" + config.getTitle() + "</title>\n" +
"    <link href=\"https://fonts.googleapis.com/css2?family=Bangers&family=Permanent+Marker&display=swap\" rel=\"stylesheet\">\n" +
"    <style>\n" +
                "        body { margin: 0; padding: 20px; font-family: Arial, sans-serif; background: #f5f5f5; display: flex; justify-content: center; }\n" +
                "        .poster { width: 900px; background: white; padding: 40px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); position: relative; }\n" +
                "        h1 { font-family: 'Bangers', cursive; font-size: 42px; text-align: center; margin: 0 0 10px 0; color: #333; line-height: 1.2; }\n" +
                "        .subtitle { text-align: center; font-size: 14px; color: #666; margin-bottom: 20px; }\n" +
                "        .student-wisdom { position: absolute; top: 40px; right: 40px; width: 220px; background: #000; color: #fff; padding: 15px; border: 3px solid #fff; box-shadow: 0 0 0 3px #000; transform: rotate(3deg); }\n" +
                "        .student-wisdom .label { font-family: 'Permanent Marker', cursive; font-size: 11px; text-align: center; border-bottom: 2px solid #fff; padding-bottom: 5px; margin-bottom: 8px; letter-spacing: 1px; }\n" +
                "        .student-wisdom .quote { font-family: 'Bangers', cursive; font-size: 18px; text-align: center; line-height: 1.3; }\n" +
                "        .graph-container { position: relative; margin: 30px 0; }\n" +
                "        svg { display: block; margin: 0 auto; }\n" +
                "        .edge { stroke: #999; stroke-width: 1; stroke-opacity: 0.25; fill: none; }\n" +
                "        .edge.strong { stroke-width: 2; stroke-opacity: 0.5; stroke: #555; }\n" +
                "        .node-box { cursor: pointer; transition: all 0.2s; stroke-width: 2; }\n" +
                "        .node-box.keep-doing { fill: #e6f7ed; stroke: #48bb78; }\n" +
                "        .node-box.stop-doing { fill: #fee; stroke: #f56565; }\n" +
                "        .node-box.start-doing { fill: #e6f2ff; stroke: #4299e1; }\n" +
                "        .node-box:hover { stroke-width: 3; filter: brightness(0.95); }\n" +
                "        .node-text { font-size: 8px; fill: #333; pointer-events: none; }\n" +
                "        .category-label { font-size: 20px; font-weight: bold; font-family: 'Permanent Marker', cursive; }\n" +
                "        .connections-info { text-align: center; font-size: 13px; color: #999; margin-top: 10px; font-style: italic; }\n" +
                "        .bottom-section { margin-top: 40px; display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }\n" +
                "        .summary-box { background: #f9f9f9; padding: 20px; border-left: 4px solid #48bb78; display: none; }\n" +
                "        .summary-box h3 { margin: 0 0 10px 0; font-family: 'Permanent Marker', cursive; color: #48bb78; font-size: 18px; }\n" +
                "        .summary-box p { margin: 0; line-height: 1.6; font-size: 14px; color: #333; }\n" +
                "        .insight-box { background: #fff3cd; padding: 20px; border-left: 4px solid #f59e0b; }\n" +
                "        .insight-box h3 { margin: 0 0 10px 0; font-family: 'Permanent Marker', cursive; color: #f59e0b; font-size: 18px; }\n" +
                "        .insight-box p { margin: 0; line-height: 1.6; font-size: 14px; color: #333; }\n" +
                "        .footer { margin-top: 30px; text-align: center; font-size: 10px; color: #999; padding: 15px; border-top: 1px solid #eee; }\n" +
                "        \n" +
                "        @media print { \n" +
                "            @page {\n" +
                "                size: A4 portrait;\n" +
                "                margin: 10mm;\n" +
                "            }\n" +
                "            body { \n" +
                "                background: white; \n" +
                "                padding: 0;\n" +
                "                margin: 0;\n" +
                "            }\n" +
                "            header {\n" +
                "                display: none !important;\n" +
                "            }\n" +
                "            .poster { \n" +
                "                box-shadow: none;\n" +
                "                page-break-inside: avoid;\n" +
                "                padding: 20px;\n" +
                "                width: 100%;\n" +
                "            }\n" +
                "            h1 { font-size: 36px; margin: 0 0 5px 0; }\n" +
                "            .subtitle { margin-bottom: 10px; }\n" +
                "            .graph-container { \n" +
                "                margin: 10px 0;\n" +
                "                max-height: 350px;\n" +
                "            }\n" +
                "            svg {\n" +
                "                max-height: 330px;\n" +
                "            }\n" +
                "            .bottom-section { \n" +
                "                margin-top: 20px;\n" +
                "                gap: 15px;\n" +
                "                grid-template-columns: 1fr;\n" +
                "            }\n" +
                "            .summary-box {\n" +
                "                display: none !important;\n" +
                "            }\n" +
                "            .insight-box { \n" +
                "                padding: 15px;\n" +
                "            }\n" +
                "            .insight-box h3 {\n" +
                "                font-size: 16px;\n" +
                "            }\n" +
                "            .insight-box p {\n" +
                "                font-size: 12px;\n" +
                "                line-height: 1.4;\n" +
                "            }\n" +
                "            .footer {\n" +
                "                margin-top: 15px;\n" +
                "                padding: 10px;\n" +
                "            }\n" +
                "            .student-wisdom {\n" +
                "                width: 180px;\n" +
                "                padding: 12px;\n" +
                "            }\n" +
                "            .student-wisdom .quote {\n" +
                "                font-size: 16px;\n" +
                "            }\n" +
                "        }\n" +
                "    </style>" +
"</head>\n" +
"<body>\n" +
"    <div class=\"poster\">\n" +
"        <div class=\"student-wisdom\">\n" +
"            <div class=\"label\">STUDENT WISDOM</div>\n" +
"            <div class=\"quote\">\"" + escapeHtml(analysis.get("funnyStatement")) + "\"</div>\n" +
"        </div>\n" +
"        \n" +
"        <h1>" + escapeHtml(analysis.get("headline")) + "</h1>\n" +
"        <div class=\"subtitle\">Netværk af forbundne student-udsagn</div>\n" +
"        \n" +
"        <div class=\"graph-container\">\n" +
"            <svg id=\"graph\" width=\"900\" height=\"700\"></svg>\n" +
"            <div class=\"connections-info\">" + edges.size() + " forbindelser mellem " + nodes.size() + " udsagn</div>\n" +
"        </div>\n" +
"        \n" +
"        <div class=\"bottom-section\">\n" +
"            <div class=\"summary-box\">\n" +
"                <h3>📊 Sammenfatning</h3>\n" +
"                <p>" + escapeHtml(analysis.get("summary")) + "</p>\n" +
"            </div>\n" +
"            <div class=\"insight-box\">\n" +
"                <h3>💡 Nøgleindsigt</h3>\n" +
"                <p>" + escapeHtml(analysis.get("keyInsight")) + "</p>\n" +
"            </div>\n" +
"        </div>\n" +
"        \n" +
"        <div class=\"footer\">\n" +
"            This poster was automatically generated using AI analysis of student feedback • " + java.time.LocalDate.now() + "\n" +
"        </div>\n" +
"    </div>\n" +
"    \n" +
"    <script>\n" +
"        const graphData = " + graphDataJson + ";\n" +
"        const svg = document.getElementById('graph');\n" +
"        const ns = 'http://www.w3.org/2000/svg';\n" +
"        \n" +
"        // Draw edges\n" +
"        graphData.edges.forEach(edge => {\n" +
"            const source = graphData.nodes[edge.source];\n" +
"            const target = graphData.nodes[edge.target];\n" +
"            const line = document.createElementNS(ns, 'line');\n" +
"            line.setAttribute('class', edge.similarity > 0.55 ? 'edge strong' : 'edge');\n" +
"            line.setAttribute('x1', source.x + source.width/2);\n" +
"            line.setAttribute('y1', source.y + source.height/2);\n" +
"            line.setAttribute('x2', target.x + target.width/2);\n" +
"            line.setAttribute('y2', target.y + target.height/2);\n" +
"            svg.appendChild(line);\n" +
"        });\n" +
"        \n" +
"        // Draw nodes\n" +
"        graphData.nodes.forEach(node => {\n" +
"            const g = document.createElementNS(ns, 'g');\n" +
"            const rect = document.createElementNS(ns, 'rect');\n" +
"            rect.setAttribute('class', `node-box ${node.category.toLowerCase().replace(' ', '-')}`);\n" +
"            rect.setAttribute('x', node.x);\n" +
"            rect.setAttribute('y', node.y);\n" +
"            rect.setAttribute('width', node.width);\n" +
"            rect.setAttribute('height', node.height);\n" +
"            rect.setAttribute('rx', 3);\n" +
"            g.appendChild(rect);\n" +
"            \n" +
"            const maxCharsPerLine = Math.floor(node.width / 5.5);\n" +
"            const words = node.text.split(' ');\n" +
"            let lines = [];\n" +
"            let currentLine = words[0];\n" +
"            for (let i = 1; i < words.length; i++) {\n" +
"                const testLine = currentLine + ' ' + words[i];\n" +
"                if (testLine.length < maxCharsPerLine) currentLine = testLine;\n" +
"                else { lines.push(currentLine); currentLine = words[i]; }\n" +
"            }\n" +
"            lines.push(currentLine);\n" +
"            const maxLines = Math.floor(node.height / 10);\n" +
"            if (lines.length > maxLines) { lines = lines.slice(0, maxLines - 1); lines.push('...'); }\n" +
"            const startY = node.y + (node.height - lines.length * 10) / 2 + 8;\n" +
"            lines.forEach((line, i) => {\n" +
"                const text = document.createElementNS(ns, 'text');\n" +
"                text.setAttribute('class', 'node-text');\n" +
"                text.setAttribute('x', node.x + 4);\n" +
"                text.setAttribute('y', startY + (i * 10));\n" +
"                text.textContent = line;\n" +
"                g.appendChild(text);\n" +
"            });\n" +
"            svg.appendChild(g);\n" +
"        });\n" +
"        \n" +
"        // Category labels\n" +
"        const labels = [\n" +
"            { text: '✅ KEEP DOING', x: 180, y: 50, color: '#48bb78' },\n" +
"            { text: '⭐ START DOING', x: 720, y: 50, color: '#4299e1' },\n" +
"            { text: '🛑 STOP DOING', x: 450, y: 500, color: '#f56565' }\n" +
"        ];\n" +
"        labels.forEach(label => {\n" +
"            const text = document.createElementNS(ns, 'text');\n" +
"            text.setAttribute('class', 'category-label');\n" +
"            text.setAttribute('x', label.x);\n" +
"            text.setAttribute('y', label.y);\n" +
"            text.setAttribute('fill', label.color);\n" +
"            text.setAttribute('text-anchor', 'middle');\n" +
"            text.textContent = label.text;\n" +
"            svg.appendChild(text);\n" +
"        });\n" +
"    </script>\n" +
"</body>\n" +
"</html>";
    }
    
    private String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
