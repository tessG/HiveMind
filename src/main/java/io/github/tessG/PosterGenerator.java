package io.github.tessG;



import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Message;

import java.util.List;
import java.util.Map;
/**
 * Service for generating poster HTML
 * Separate methods for different poster types (Delphi dashboard, DSC, etc.)
 * Pure HTML generation - NO preview wrapper styling
 */
public class PosterGenerator {
    
    /**
     * Generate Delphi A3 Landscape Dashboard
     * Dimensions: 2480px x 1754px (A3 landscape at 150ppi)
     */
    public static String generateDelphiDashboard(
            List<Map<String, Object>> nodes,
            List<Map<String, Object>> edges,
            EvaluationConfig config,
            Map<String, String> analysis,
            List<Map<String, Object>> contradictions,
            List<String> suggestions,
            Map<String, List<Statement>> categorized) {
        
        // Convert data to JSON
        String graphDataJson = buildGraphDataJson(nodes, edges);
        String contradictionsJson = buildContradictionsJson(contradictions, categorized);
        
        // Build HTML
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>").append(escapeHtml(config.getTitle())).append(" - Dashboard</title>\n");
        html.append("<link href=\"https://fonts.googleapis.com/css2?family=Bangers&family=Permanent+Marker&display=swap\" rel=\"stylesheet\">\n");
        
        // Add CSS
        html.append(getDelphiDashboardCSS());
        
        html.append("</head>\n<body>\n");
        html.append("<div class=\"poster-wrapper\">\n");
        html.append("<div class=\"poster\">\n");
        
        // Header
        html.append(buildDelphiHeader(analysis));
        
        // Similarity Graph
        html.append("<div class=\"similarity-graph\">\n");
        html.append("<h2>🔗 Forbindelser mellem udsagn</h2>\n");
        html.append("<svg id=\"similarityGraph\" width=\"1200\" height=\"800\"></svg>\n");
        html.append("<div class=\"connections-info\">").append(edges.size()).append(" forbindelser mellem ").append(nodes.size()).append(" udsagn</div>\n");
        html.append("</div>\n");
        
        // Contradictory Graph
        html.append("<div class=\"contradictory-graph\">\n");
        html.append("<h2>⚡ Modsætninger & Spændinger</h2>\n");
        html.append("<svg id=\"contradictionGraph\" width=\"1200\" height=\"1544\"></svg>\n");
        html.append("</div>\n");
        
        // Summary
        html.append("<div class=\"summary\">\n");
        html.append("<h3>📊 Sammenfatning</h3>\n");
        html.append("<p>").append(escapeHtml(analysis.get("summary"))).append("</p>\n");
        html.append("</div>\n");
        
        // Key Insight
        html.append("<div class=\"key-insight\">\n");
        html.append("<h3>💡 Nøgleindsigt</h3>\n");
        html.append("<p>").append(escapeHtml(analysis.get("keyInsight"))).append("</p>\n");
        html.append("</div>\n");
        
        html.append("</div>\n</div>\n");
        
        // Add JavaScript
        html.append(getDelphiDashboardJS(graphDataJson, contradictionsJson));
        
        html.append("</body>\n</html>");
        
        return html.toString();
    }
    
    /**
     * Generate DSC Poster (existing format)
     * Keep current dimensions and style
     */
    public static String generateDSCPoster(
            List<Map<String, Object>> nodes,
            List<Map<String, Object>> edges,
            EvaluationConfig config,
            Map<String, String> analysis) {
        
        // TODO: Extract from GenericEvaluationWorkflow
        // For now, return placeholder
        return  "<!DOCTYPE html><html><body>" +
                "<h1>DSC Poster - To Be Implemented</h1>" +
                "<p>Headline: " + analysis.get("headline") + "</p>" +
                "<p>Summary: " + analysis.get("summary") + "</p>" +
                "</body></html>";
    }

    public static String generateDSCPoster(
            List<String> statements,
            EvaluationConfig config,
            Map<String, String> analysis) throws Exception {
       String headline = analysis.get("headline");
      String summary =  analysis.get("summary");
      String funnyStatement =  analysis.get("funnyStatement");
       String keyInsight = analysis.get("keyInsight");
        //test om det jeg får tilbage fra analysen er godt nok. Bed om claude om at designe en html plakat (fiks prompten til kun at fokusere på hvordan plakaten skal se ud)
   /*      String html =  "<!DOCTYPE html><html><body>" +
                 "<h1>DSC Poster - To Be Implemented</h1>" +
                 "<p>Headline: " + analysis.get("headline") + "</p>" +
                 "<p>Summary: " + analysis.get("summary") + "</p>"+
                "<p>Funny statement: " + analysis.get("funnyStatement") + "</p>"+
                 "<p>Nøgleindsigt: " + analysis.get("keyInsight") + "</p>"+

                 "<h2>Statements:</h2>";

        for (String s: statements) {
            html += "<p>" + s + "</p\n";
        }

        html +=  "</body></html>";*/


         //To have claude generate the HTML call this:
        AnthropicClient client = AnthropicOkHttpClient.fromEnv();

        MessageCreateParams params = MessageCreateParams.builder()
                .model("claude-sonnet-4-5-20250929")
                .maxTokens(8000)  // Increased for HTML generation
                .addUserMessage(buildDSCPrompt(statements,headline,funnyStatement,summary,keyInsight, config))
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

    private static String buildDSCPrompt(List<String> statements,String headline, String funnyStatement,String summary,String keyInsight, EvaluationConfig config) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generer en plakat med følgende indhold\n\n");
        prompt.append("Titel:\n"+headline+"\n");
        prompt.append("Sjovt udsagn:\n"+funnyStatement+"\n");

        prompt.append("Wordcloud på baggrund af udsagn/statements:\n");
        prompt.append("\nUdsagn:\n");
        for (int i = 0; i < statements.size(); i++) {
            prompt.append((i + 1)).append(". ").append(statements.get(i)).append("\n");
        }
        prompt.append("Kategoriser udsagnene i disse kategorier:\n");
        for (Category category : config.getCategories()) {
            prompt.append("- ").append(category.getName()).append(" (").append(category.getEmoji()).append(")\n");
        }
        prompt.append("Sammenfatning:\n"+summary+"\n");
        prompt.append("Nøgleindsigt:\n"+keyInsight+"\n");


        prompt.append("POSTER FORMAT:\n");
        prompt.append("For print: landscape A3\n");
        prompt.append("On screen: scale to fit\n");

        prompt.append("layout:\n");
        prompt.append(" ------------------------------------------------------------ \n");
        prompt.append("| title og subtitle          |         funny statement |\n");
        prompt.append("|----------------------------------------------------------- |\n");
        prompt.append("| statements  |    statements      |  statements             |\n");
        prompt.append("| statements  |    WORDCLOUD       |  statements             |\n");
        prompt.append("| statements  |    statements      |  statements             |\n");
        prompt.append("-----------------------------------------------------------  |\n");
        prompt.append("|          SUMMARY                  |      KEY INSIGHT       |\n");
        prompt.append("|___________________________________________________________ |\n");
        prompt.append("| Dare summary |    Share summary      |  Care summary        |\n");
        prompt.append("|___________________________________________________________ |\n");
        prompt.append("|                FOOTER                                     |\n");
        prompt.append("|___________________________________________________________ |\n");
        prompt.append("1. HEADER :\n");
        prompt.append("   - Titel: 'Student Vibes' "+headline+"\n");
        prompt.append("   - Subtitle: sætning der indfanger essensen af statements\n");
        prompt.append("   - Quote badge:"+funnyStatement+" Top-right, absolut position\n");
        prompt.append("     * Kun citatet - INGEN titel\n");
        prompt.append("     * Sort baggrund, hvid Bangers tekst (16px)\n");
        prompt.append("     * 180px bred, padding 12px, roteret 4°\n\n");
        prompt.append("2. WORDCLOUD + BUBBLES :\n");
        prompt.append("   \n");
        prompt.append("   WORDCLOUD på baggrund af udsagn (center, 300×300px):\n");
        prompt.append("   - Placeret i midten horisontalt og vertikalt\n");
        prompt.append("   - Top 5 ord: 45-60px BOLD i centrum\n");
        prompt.append("   - Medium (8 ord): 30-40px omkring centrum\n");
        prompt.append("   - Rest (12 ord): 18-20px yderkant\n");
        prompt.append("   - Total: ~25 ord\n");
        prompt.append("   - Farver: gul/rød/grøn med sort shadow\n");
        prompt.append("   - Tæt pakket, roterede ord\n");
        prompt.append("   - Top ord placeres i centrum af skyen\n");
        prompt.append("   - mere bred end høj, bubler må gerne dække ord i periferien\n");
        prompt.append("   \n");
        prompt.append("   SPEECH BUBBLES rundt om wordcloud(12-14 stykker TOTAL):\n");
        prompt.append("   - Cluster lignende udsagn sammen\n");
        prompt.append("   - Hver bubble repræsenterer 2-4 originale udsagn\n");
        prompt.append("   - Skriv sammenfattende udsagn der dækker temaet\n");
        prompt.append("   - Eksempel: I stedet for 3 bubbles om \"mod til at fejle\", lav ÉN:\n");
        prompt.append("     \"Studerende ønsker et miljø hvor man tør at fejle \"\n");
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
        prompt.append("   \n");
        prompt.append("   SAMMENFATNING (200px):\n");
        prompt.append("   - height: fit to content\n");
        prompt.append("   - Titel: \"📊 Sammenfatning\" (Permanent Marker, 22px)\n");
        prompt.append("   - 2-3 sætninger der sammenfatter udsagn\n");
        prompt.append("   - Grå border-left (4px, #6b7280)\n");
        prompt.append("   - Background: #f9fafb\n");
        prompt.append("   - Padding: 20px\n");
        prompt.append("   - Line-height: 1.6\n");
        prompt.append("3. KEY INSIGHT (height: fit to content):\n");
        prompt.append("   - Gul baggrund (#fef3c7)\n");
        prompt.append("   - Orange border-left (4px, #f59e0b)\n");
        prompt.append("   - Titel: \"💡 Nøgleindsigt\" (Permanent Marker, 20px)\n");
        prompt.append("   - 2-3 sætninger om vigtigste observation\n");
        prompt.append("   - Padding: 20px\n");
        prompt.append("   - Line-height: 1.6\n\n");
        prompt.append("   \n");
        prompt.append("4.  3 KATEGORI KOLONNER (dare, share, care)):\n");
        prompt.append("   - Side-by-side, equal width\n");
        prompt.append("   - Hver kolonne:\n");
        prompt.append("     * Emoji + kategori navn \n");
        prompt.append("     * Farvet top-border (4px)\n");
        prompt.append("     * Background hvid\n");
        prompt.append("     * Padding: 15px\n\n");
        prompt.append("   \n");
        prompt.append("5. FOOTER (20px høj):\n");
        prompt.append("   - Full-width\n");
        prompt.append("   - Text: 'This poster was automatically generated using AI analysis of student feedback • [dagens dato]'\n");
        prompt.append("   - Font-size: 10px\n");
        prompt.append("   - Color: #999\n");
        prompt.append("   - Text-align: center\n");
        prompt.append("   - Border-top: 1px solid #eee\n");
        prompt.append("   - Padding: 5px 0\n\n");
        prompt.append("OUTPUT:\n");
        prompt.append("Returner KUN komplet HTML.\n");
        prompt.append("- Start med <!DOCTYPE html>\n");
        prompt.append("- INGEN markdown\n");
        prompt.append("- INGEN forklaring\n");
        prompt.append("- INGEN debug text\n");

        return prompt.toString();
    }





    
    // ============= HELPER METHODS =============
    
    private static String buildDelphiHeader(Map<String, String> analysis) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"header\">\n");
        html.append("<div class=\"title-section\">\n");
        html.append("<h1>").append(escapeHtml(analysis.get("headline"))).append("</h1>\n");
        html.append("</div>\n");
        html.append("<div class=\"controversial-statement\">\n");
        html.append("<div class=\"label\">STUDENT WISDOM</div>\n");
        html.append("<div class=\"quote\">\"").append(escapeHtml(analysis.get("funnyStatement"))).append("\"</div>\n");
        html.append("</div>\n");
        html.append("</div>\n");
        return html.toString();
    }
    
    private static String buildGraphDataJson(List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
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
        
        return "{\"nodes\":" + nodesJson + ",\"edges\":" + edgesJson + "}";
    }
    
    private static String buildContradictionsJson(List<Map<String, Object>> contradictions, 
                                                   Map<String, List<Statement>> categorized) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < contradictions.size(); i++) {
            if (i > 0) json.append(",");
            Map<String, Object> contra = contradictions.get(i);
            String stmt1 = (String) contra.get("statement1");
            String stmt2 = (String) contra.get("statement2");
            
            String cat1 = GraphDataService.findStatementCategory(stmt1, categorized);
            String cat2 = GraphDataService.findStatementCategory(stmt2, categorized);
            
            json.append("{")
                .append("\"statement1\":\"").append(escapeJson(stmt1)).append("\",")
                .append("\"statement2\":\"").append(escapeJson(stmt2)).append("\",")
                .append("\"category1\":\"").append(escapeJson(cat1)).append("\",")
                .append("\"category2\":\"").append(escapeJson(cat2)).append("\",")
                .append("\"tension\":").append(contra.get("tension")).append(",")
                .append("\"theme\":\"").append(escapeJson((String)contra.get("theme"))).append("\"")
                .append("}");
        }
        json.append("]");
        return json.toString();
    }
    
    private static String getDelphiDashboardCSS() {
        return "<style>\n" +
"    * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
"    body { margin: 0; padding: 20px; font-family: Arial, sans-serif; background: #f5f5f5; }\n" +
"    .poster-wrapper { transform: scale(0.41); transform-origin: top left; }\n" +
"    .poster { width: 2480px; height: 1754px; background: white; padding: 30px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);\n" +
"        display: grid; grid-template-columns: 1140px 1240px; grid-template-rows: 200px 1280px 320px; gap: 20px; }\n" +
"    .header { grid-column: 1 / -1; grid-row: 1; display: grid; grid-template-columns: 1600px 820px; gap: 20px; align-items: center; }\n" +
"    .title-section h1 { font-family: 'Bangers', cursive; font-size: 64px; color: #333; line-height: 1.1; margin-bottom: 10px; }\n" +
"    .title-section .subtitle { font-size: 22px; color: #666; }\n" +
"    .controversial-statement { background: #000; color: #fff; padding: 25px; border: 4px solid #fff;\n" +
"        box-shadow: 0 0 0 4px #000, 0 4px 12px rgba(0,0,0,0.3); transform: rotate(-2deg); }\n" +
"    .controversial-statement .label { font-family: 'Permanent Marker', cursive; font-size: 14px; text-align: center;\n" +
"        border-bottom: 2px solid #fff; padding-bottom: 8px; margin-bottom: 12px; letter-spacing: 2px; }\n" +
"    .controversial-statement .quote { font-family: 'Bangers', cursive; font-size: 24px; text-align: center; line-height: 1.3; }\n" +
"    .similarity-graph { grid-column: 1; grid-row: 2; border: 2px solid #e2e8f0; border-radius: 8px; padding: 20px; background: #fff; }\n" +
"    .similarity-graph h2 { font-family: 'Permanent Marker', cursive; font-size: 28px; margin-bottom: 15px; color: #4299e1; }\n" +
"    .contradictory-graph { grid-column: 2; grid-row: 2; border: 2px solid #fed7d7; border-radius: 8px; padding: 20px;\n" +
"        background: #fff; display: flex; flex-direction: column; }\n" +
"    .contradictory-graph h2 { font-family: 'Permanent Marker', cursive; font-size: 28px; margin-bottom: 15px; color: #e53e3e; }\n" +
"    .summary { grid-column: 1; grid-row: 3; background: #f9fafb; border-left: 6px solid #6b7280; border-radius: 8px; padding: 25px;max-height: fit-content; }\n" +
"    .summary h3 { font-family: 'Permanent Marker', cursive; font-size: 26px; color: #4a5568; margin-bottom: 12px; }\n" +
"    .summary p { font-size: 16px; line-height: 1.6; color: #333; }\n" +
"    .key-insight { grid-column: 2; grid-row: 3; background: #fff3cd; border-left: 6px solid #f59e0b; border-radius: 8px; padding: 25px;max-height: fit-content;  }\n" +
"    .key-insight h3 { font-family: 'Permanent Marker', cursive; font-size: 26px; color: #f59e0b; margin-bottom: 12px; }\n" +
"    .key-insight p { font-size: 18px; line-height: 1.6; color: #333; }\n" +
"    svg { display: block; }\n" +
"    .edge { stroke: #cbd5e0; stroke-width: 1.5; stroke-opacity: 0.3; fill: none; }\n" +
"    .edge.strong { stroke-width: 2.5; stroke-opacity: 0.5; stroke: #718096; }\n" +
"    .contradiction-edge { stroke: #e53e3e; stroke-width: 3; stroke-dasharray: 8,6; fill: none; opacity: 0.7; }\n" +
"    .contradiction-edge.high-tension { stroke-width: 4; opacity: 0.9; stroke: #c53030; }\n" +
"    .node-box { cursor: pointer; transition: all 0.2s; stroke-width: 2.5; }\n" +
"    .node-box.keep-doing { fill: #e6f7ed; stroke: #48bb78; }\n" +
"    .node-box.stop-doing { fill: #fee; stroke: #f56565; }\n" +
"    .node-box.start-doing { fill: #e6f2ff; stroke: #4299e1; }\n" +
"    .node-box:hover { stroke-width: 4; filter: brightness(0.95); }\n" +
"    .node-text { font-size: 16px; fill: #333; pointer-events: none; font-weight: 500; }\n" +
"    .contradiction-node-text { font-size: 16px; fill: #1a202c; pointer-events: none; font-weight: 600; }\n" +
"    .category-label { font-size: 24px; font-weight: bold; font-family: 'Permanent Marker', cursive; }\n" +
"    .tension-label { font-size: 14px; fill: #e53e3e; font-weight: bold; font-family: 'Permanent Marker', cursive; }\n" +
"    .connections-info { text-align: center; font-size: 14px; color: #718096; margin-top: 10px; font-style: italic; }\n" +
"    @media print { body { background: white; margin: 0; padding: 0; } .poster-wrapper { transform: scale(1); } .poster { box-shadow: none; } }\n" +
"</style>\n";
    }
    
    private static String getDelphiDashboardJS(String graphDataJson, String contradictionsJson) {

        return "<script>\n" +
"const similarityData = " + graphDataJson + ";\n" +
"const contradictionData = " + contradictionsJson + ";\n" +
"const ns = 'http://www.w3.org/2000/svg';\n" +
"const simSvg = document.getElementById('similarityGraph');\n" +
"const contraSvg = document.getElementById('contradictionGraph');\n" +
 "// Draw edges\n" +
                "        similarityData.edges.forEach(edge => {\n" +
                "            const source = similarityData.nodes[edge.source];\n" +
                "            const target = similarityData.nodes[edge.target];\n" +
                "            const line = document.createElementNS(ns, 'line');\n" +
                "            line.setAttribute('class', edge.similarity > 0.55 ? 'edge strong' : 'edge');\n" +
                "            line.setAttribute('x1', source.x + source.width/2);\n" +
                "            line.setAttribute('y1', source.y + source.height/2);\n" +
                "            line.setAttribute('x2', target.x + target.width/2);\n" +
                "            line.setAttribute('y2', target.y + target.height/2);\n" +
                "            simSvg.appendChild(line);\n" +
                "        });\n" +
                "        \n" +
                "        // Draw nodes\n" +
                "        similarityData.nodes.forEach(node => {\n" +
                "            const g = document.createElementNS(ns, 'g');\n" +
                "            const rect = document.createElementNS(ns, 'rect');\n" +
                "            rect.setAttribute('class', `node-box ${node.category.toLowerCase().replace(' ', '-')}`);\n" +
                "            rect.setAttribute('x', node.x);\n" +
                "            rect.setAttribute('y', node.y);\n" +
                "            rect.setAttribute('width', node.width);\n" +
                "            rect.setAttribute('height', node.height);\n" +
                "            rect.setAttribute('rx', 4);\n" +
                "            g.appendChild(rect);\n" +
                "            \n" +
                "            const maxCharsPerLine = Math.floor(node.width / 7);\n" +
                "            const words = node.text.split(' ');\n" +
                "            let lines = [];\n" +
                "            let currentLine = words[0];\n" +
                "            for (let i = 1; i < words.length; i++) {\n" +
                "                const testLine = currentLine + ' ' + words[i];\n" +
                "                if (testLine.length < maxCharsPerLine) currentLine = testLine;\n" +
                "                else { lines.push(currentLine); currentLine = words[i]; }\n" +
                "            }\n" +
                "            lines.push(currentLine);\n" +
                "            const maxLines = Math.floor(node.height / 14);\n" +
                "            if (lines.length > maxLines) { lines = lines.slice(0, maxLines - 1); lines.push('...'); }\n" +
                "            const startY = node.y + (node.height - lines.length * 14) / 2 + 12;\n" +
                "            lines.forEach((line, i) => {\n" +
                "                const text = document.createElementNS(ns, 'text');\n" +
                "                text.setAttribute('class', 'node-text');\n" +
                "                text.setAttribute('x', node.x + 6);\n" +
                "                text.setAttribute('y', startY + (i * 14));\n" +
                "                text.textContent = line;\n" +
                "                g.appendChild(text);\n" +
                "            });\n" +
                "            simSvg.appendChild(g);\n" +
                "        });\n" +
                "        \n" +
                "        // CONTRADICTION GRAPH\n" +
                "       // const contraSvg = document.getElementById('contradictionGraph');\n" +
                "        \n" +
                "        // Helper function to get category color class\n" +
                "        function getCategoryClass(category) {\n" +
                "            return category.toLowerCase().replace(' ', '-');\n" +
                "        }\n" +
                "        \n" +
                "        // Position nodes for contradiction graph (left vs right layout)\n" +
                "        const nodeWidth = 180;\n" +
                "        const nodeHeight = 90;\n" +
                "        const leftX = 100;\n" +
                "        const rightX = 1235 - nodeWidth - 100;\n" +
                "        const startY = 100;\n" +
                "        const verticalSpacing = 140;\n" +
                "        \n" +
                "        contradictionData.forEach((contra, index) => {\n" +
                "            const y = startY + (index * verticalSpacing);\n" +
                "            \n" +
                "            // Draw edge\n" +
                "            const line = document.createElementNS(ns, 'line');\n" +
                "            line.setAttribute('class', contra.tension > 0.7 ? 'contradiction-edge high-tension' : 'contradiction-edge');\n" +
                "            line.setAttribute('x1', leftX + nodeWidth);\n" +
                "            line.setAttribute('y1', y + nodeHeight/2);\n" +
                "            line.setAttribute('x2', rightX);\n" +
                "            line.setAttribute('y2', y + nodeHeight/2);\n" +
                "            contraSvg.appendChild(line);\n" +
                "            \n" +
                "            // Draw theme label\n" +
                "            const midX = (leftX + nodeWidth + rightX) / 2;\n" +
                "            const themeLabel = document.createElementNS(ns, 'text');\n" +
                "            themeLabel.setAttribute('class', 'tension-label');\n" +
                "            themeLabel.setAttribute('x', midX);\n" +
                "            themeLabel.setAttribute('y', y + nodeHeight/2 - 8);\n" +
                "            themeLabel.setAttribute('text-anchor', 'middle');\n" +
                "            themeLabel.textContent = contra.theme;\n" +
                "            contraSvg.appendChild(themeLabel);\n" +
                "            \n" +
                "            const scoreLabel = document.createElementNS(ns, 'text');\n" +
                "            scoreLabel.setAttribute('class', 'tension-label');\n" +
                "            scoreLabel.setAttribute('x', midX);\n" +
                "            scoreLabel.setAttribute('y', y + nodeHeight/2 + 10);\n" +
                "            scoreLabel.setAttribute('text-anchor', 'middle');\n" +
                "            scoreLabel.setAttribute('font-size', '12');\n" +
                "            scoreLabel.textContent = `⚡ ${contra.tension.toFixed(2)}`;\n" +
                "            contraSvg.appendChild(scoreLabel);\n" +
                "            \n" +
                "            // Left node\n" +
                "            const leftG = document.createElementNS(ns, 'g');\n" +
                "            const leftRect = document.createElementNS(ns, 'rect');\n" +
                "            leftRect.setAttribute('class', `node-box ${getCategoryClass(contra.category1)}`);\n" +
                "            leftRect.setAttribute('x', leftX);\n" +
                "            leftRect.setAttribute('y', y);\n" +
                "            leftRect.setAttribute('width', nodeWidth);\n" +
                "            leftRect.setAttribute('height', nodeHeight);\n" +
                "            leftRect.setAttribute('rx', 6);\n" +
                "            leftG.appendChild(leftRect);\n" +
                "            \n" +
                "            // Wrap left text\n" +
                "            const leftWords = contra.statement1.split(' ');\n" +
                "            let leftLines = [];\n" +
                "            let currentLine = leftWords[0];\n" +
                "            for (let i = 1; i < leftWords.length; i++) {\n" +
                "                if ((currentLine + ' ' + leftWords[i]).length < 22) {\n" +
                "                    currentLine += ' ' + leftWords[i];\n" +
                "                } else {\n" +
                "                    leftLines.push(currentLine);\n" +
                "                    currentLine = leftWords[i];\n" +
                "                }\n" +
                "            }\n" +
                "            leftLines.push(currentLine);\n" +
                "            if (leftLines.length > 4) { leftLines = leftLines.slice(0, 3); leftLines.push('...'); }\n" +
                "            \n" +
                "            const leftStartY = y + (nodeHeight - leftLines.length * 18) / 2 + 14;\n" +
                "            leftLines.forEach((line, i) => {\n" +
                "                const text = document.createElementNS(ns, 'text');\n" +
                "                text.setAttribute('class', 'contradiction-node-text');\n" +
                "                text.setAttribute('x', leftX + 10);\n" +
                "                text.setAttribute('y', leftStartY + (i * 18));\n" +
                "                text.textContent = line;\n" +
                "                leftG.appendChild(text);\n" +
                "            });\n" +
                "            contraSvg.appendChild(leftG);\n" +
                "            \n" +
                "            // Right node\n" +
                "            const rightG = document.createElementNS(ns, 'g');\n" +
                "            const rightRect = document.createElementNS(ns, 'rect');\n" +
                "            rightRect.setAttribute('class', `node-box ${getCategoryClass(contra.category2)}`);\n" +
                "            rightRect.setAttribute('x', rightX);\n" +
                "            rightRect.setAttribute('y', y);\n" +
                "            rightRect.setAttribute('width', nodeWidth);\n" +
                "            rightRect.setAttribute('height', nodeHeight);\n" +
                "            rightRect.setAttribute('rx', 6);\n" +
                "            rightG.appendChild(rightRect);\n" +
                "            \n" +
                "            // Wrap right text\n" +
                "            const rightWords = contra.statement2.split(' ');\n" +
                "            let rightLines = [];\n" +
                "            currentLine = rightWords[0];\n" +
                "            for (let i = 1; i < rightWords.length; i++) {\n" +
                "                if ((currentLine + ' ' + rightWords[i]).length < 22) {\n" +
                "                    currentLine += ' ' + rightWords[i];\n" +
                "                } else {\n" +
                "                    rightLines.push(currentLine);\n" +
                "                    currentLine = rightWords[i];\n" +
                "                }\n" +
                "            }\n" +
                "            rightLines.push(currentLine);\n" +
                "            if (rightLines.length > 4) { rightLines = rightLines.slice(0, 3); rightLines.push('...'); }\n" +
                "            \n" +
                "            const rightStartY = y + (nodeHeight - rightLines.length * 18) / 2 + 14;\n" +
                "            rightLines.forEach((line, i) => {\n" +
                "                const text = document.createElementNS(ns, 'text');\n" +
                "                text.setAttribute('class', 'contradiction-node-text');\n" +
                "                text.setAttribute('x', rightX + 10);\n" +
                "                text.setAttribute('y', rightStartY + (i * 18));\n" +
                "                text.textContent = line;\n" +
                "                rightG.appendChild(text);\n" +
                "            });\n" +
                "            contraSvg.appendChild(rightG);\n" +
                "        });\n" +
                "        \n" +
                "        // Category labels for similarity graph\n" +
                "        const labels = [\n" +
                "            { text: '✅ KEEP DOING', x: 200, y: 60, color: '#48bb78' },\n" +
                "            { text: '⭐ START DOING', x: 800, y: 60, color: '#4299e1' },\n" +
                "            { text: '🛑 STOP DOING', x: 500, y: 600, color: '#f56565' }\n" +
                "        ];\n" +
                "        labels.forEach(label => {\n" +
                "            const text = document.createElementNS(ns, 'text');\n" +
                "            text.setAttribute('class', 'category-label');\n" +
                "            text.setAttribute('x', label.x);\n" +
                "            text.setAttribute('y', label.y);\n" +
                "            text.setAttribute('fill', label.color);\n" +
                "            text.setAttribute('text-anchor', 'middle');\n" +
                "            text.textContent = label.text;\n" +
                "            simSvg.appendChild(text);\n" +
                "        });\n" +




"</script>\n";
    }
    
    private static String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
    
    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
