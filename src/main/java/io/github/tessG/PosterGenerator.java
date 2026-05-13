package io.github.tessG;



import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Message;

import java.nio.file.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
/**
 * Service for generating poster HTML
 * Separate methods for different poster types (Delphi dashboard, DSC, etc.)
 * Pure HTML generation - NO preview wrapper styling
 */
public class PosterGenerator {

    private static final String MODEL = "claude-sonnet-4-5-20250929";

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
        html.append("<h2>Svar</h2>\n");
        html.append("<svg id=\"similarityGraph\" width=\"1200\" height=\"1080\"></svg>\n");
        html.append("<div class=\"connections-info\">").append(edges.size()).append(" forbindelser mellem ").append(nodes.size()).append(" udsagn</div>\n");
        html.append("</div>\n");
        
        // Contradictory Graph
        html.append("<div class=\"contradictory-graph\">\n");
        html.append("<h2>⚡ Modsætninger & Spændinger</h2>\n");
        html.append("<svg id=\"contradictionGraph\" width=\"1200\" height=\"1100\"></svg>\n");
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


        // Cache DSC poster by content hash so Claude is not called again for the same data
        String cacheKey = computeDscCacheKey(statements, config.getType());
        Path cacheFile = Paths.get("cache", cacheKey + "-dsc-poster.html");
        if (Files.exists(cacheFile)) {
            System.out.println("📦 DSC poster cache hit");
            return Files.readString(cacheFile);
        }

        AnthropicClient client = AnthropicOkHttpClient.fromEnv();

        MessageCreateParams params = MessageCreateParams.builder()
                .model(MODEL)
                .maxTokens(8000)
                .addUserMessage(buildDSCPrompt(statements, headline, funnyStatement, summary, keyInsight, config))
                .build();

        Message message = client.messages().create(params);

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

        try {
            Files.createDirectories(Paths.get("cache"));
            Files.writeString(cacheFile, html);
            System.out.println("💾 DSC poster cached");
        } catch (Exception e) {
            System.err.println("DSC poster cache write failed: " + e.getMessage());
        }

        return html;
    }

    private static String computeDscCacheKey(List<String> statements, String evaluationType) {
        try {
            List<String> sorted = new ArrayList<>(statements);
            Collections.sort(sorted);
            String input = "dsc-poster:" + evaluationType + ":" + String.join("|", sorted);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString().substring(0, 32);
        } catch (Exception e) {
            return String.valueOf(statements.hashCode());
        }
    }

    private static String buildDSCPrompt(List<String> statements,String headline, String funnyStatement,String summary,String keyInsight, EvaluationConfig config) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generer en plakat med følgende \n\n");
        prompt.append("Make it good for both screen and for print (A3)\n");
        prompt.append("COLOUR PALETTE - use only these colours (do NOT use purple, orange, or yellow accents):\n");
        prompt.append("  - Deep navy: #1B3A5C\n");
        prompt.append("  - Teal accent: #4AB5BE\n");
        prompt.append("  - Sky light (backgrounds, gradients): #E0F4F7\n");
        prompt.append("  - Sky medium: #87CEEB\n");
        prompt.append("  - Wordcloud background: linear-gradient(135deg, #E0F4F7 0%, #87CEEB 100%)\n");
        prompt.append("  - Key insight box: linear-gradient(135deg, #E0F4F7 0%, #B8DDE8 100%), border #4AB5BE, text #1B3A5C\n");
        prompt.append("  - Dare category keeps #dc2626 (red), Share keeps #2563eb (blue), Care keeps #16a34a (green)\n");
        prompt.append("  - Backgrounds stay white or #f9fafb\n\n");
        prompt.append("layout:\n");
        prompt.append(" ------------------------------------------------------------ \n");
        prompt.append("| title og subtitle          |     funny statement           |\n");
        prompt.append("|----------------------------------------------------------- |\n");
        prompt.append("| statements  |    statements      |  statements             |\n");
        prompt.append("| statements  |    WORDCLOUD       |  statements             |\n");
        prompt.append("| statements  |    statements      |  statements             |\n");
        prompt.append("-----------------------------------------------------------  |\n");
        prompt.append("|          SUMMARY                  |      KEY INSIGHT       |\n");
        prompt.append("|___________________________________________________________ |\n");
        prompt.append("| Dare summary |    Share summary      |  Care summary        |\n");
        prompt.append("|___________________________________________________________ |\n");

        prompt.append("1. HEADER :\n");
        prompt.append("   - Titel: 'Student Vibes' "+headline+"  DO NOT INCLUDE ANY ICONS!!\n");
        prompt.append("   - Subtitle: sætning der indfanger essensen af statements\n");
        prompt.append("   - Quote badge:"+funnyStatement+" Top-right, absolut position\n");
        prompt.append("     * Kun citatet - INGEN titel\n");
        prompt.append("     * Sort baggrund, hvid Bangers tekst (16px)\n");
        prompt.append("     * 180px bred, padding 12px, roteret 4°\n\n");
        prompt.append("2. WORDCLOUD omgivet af udvalgte repræsentative statements placeret i talebobler :\n");
        prompt.append("   - Eksempel: I stedet for 3 bubbles om \"mod til at fejle\", lav ÉN:\n");
        prompt.append("     \"Studerende ønsker et miljø hvor man tør at fejle \"\n");
        prompt.append("   \n statements: ");
        for (int i = 0; i < statements.size(); i++) {
            prompt.append((i + 1)).append(". ").append(statements.get(i)).append("\n");
        }
        prompt.append("   \n");

        prompt.append("   - Hver bubble:\n");
        prompt.append("     * Hvid baggrund, sort 2px border, border-radius 8px\n");
        prompt.append("     * Permanent Marker, 12px\n");
        prompt.append("     * 140-180px bred (variér!)\n");
        prompt.append("     * Padding: 12px\n");
        prompt.append("     * Rotation: -6° til +6°\n");
        prompt.append("     * Subtle box-shadow\n");
        prompt.append("     * cursor: pointer; z-index: 2; transition: box-shadow 0.2s;\n");
        prompt.append("     * On hover: z-index: 100; box-shadow: 0 10px 28px rgba(0,0,0,0.22); (brings overlapping bubbles to front)\n");
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
        prompt.append("4.  3 KATEGORI KOLONNER (dare, share, care)):\n");
        prompt.append("   - Side-by-side, equal width\n");
        prompt.append("   - Hver kolonne:\n");
        prompt.append("     * Kategori navn \n");
        prompt.append("     * Farvet top-border (4px)\n");
        prompt.append("     * Background hvid\n");
        prompt.append("     * Padding: 10px\n\n");
        prompt.append("   \n");
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
"    @media screen { .poster-wrapper { zoom: 0.41; } }\n" +
"    .poster { width: 2480px; height: 1754px; background: white; padding: 30px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);\n" +
"        display: grid; grid-template-columns: 1140px 1240px; grid-template-rows: 200px 1194px 260px; gap: 20px; }\n" +
"    .header { grid-column: 1 / -1; grid-row: 1; display: grid; grid-template-columns: 1600px 820px; gap: 20px; align-items: center; }\n" +
"    .title-section h1 { font-family: 'Bangers', cursive; font-size: 64px; color: #333; line-height: 1.1; margin-bottom: 10px; }\n" +
"    .title-section .subtitle { font-size: 22px; color: #666; }\n" +
"    .controversial-statement { background: #000; color: #fff; padding: 25px; border: 4px solid #fff;\n" +
"        box-shadow: 0 0 0 4px #000, 0 4px 12px rgba(0,0,0,0.3); transform: rotate(-2deg); }\n" +
"    .controversial-statement .label { font-family: 'Permanent Marker', cursive; font-size: 14px; text-align: center;\n" +
"        border-bottom: 2px solid #fff; padding-bottom: 8px; margin-bottom: 12px; letter-spacing: 2px; }\n" +
"    .controversial-statement .quote { font-family: 'Bangers', cursive; font-size: 24px; text-align: center; line-height: 1.3; }\n" +
"    .similarity-graph { grid-column: 1; grid-row: 2; border: 2px solid #e2e8f0; border-radius: 8px; padding: 20px; background: #fff; }\n" +
"    .similarity-graph h2 { font-family: 'Permanent Marker', cursive; font-size: 28px; margin-bottom: 15px; color: #4AB5BE; }\n" +
"    .contradictory-graph { grid-column: 2; grid-row: 2; border: 2px solid #fed7d7; border-radius: 8px; padding: 20px;\n" +
"        background: #fff; display: flex; flex-direction: column; }\n" +
"    .contradictory-graph h2 { font-family: 'Permanent Marker', cursive; font-size: 28px; margin-bottom: 15px; color: #e53e3e; }\n" +
"    .summary { grid-column: 1; grid-row: 3; background: #f9fafb; border-left: 6px solid #6b7280; border-radius: 8px; padding: 25px;max-height: fit-content; }\n" +
"    .summary h3 { font-family: 'Permanent Marker', cursive; font-size: 26px; color: #4a5568; margin-bottom: 12px; }\n" +
"    .summary p { font-size: 16px; line-height: 1.6; color: #333; }\n" +
"    .key-insight { grid-column: 2; grid-row: 3; background: #E0F4F7; border-left: 6px solid #4AB5BE; border-radius: 8px; padding: 25px;max-height: fit-content;  }\n" +
"    .key-insight h3 { font-family: 'Permanent Marker', cursive; font-size: 26px; color: #2C6E7A; margin-bottom: 12px; }\n" +
"    .key-insight p { font-size: 18px; line-height: 1.6; color: #333; }\n" +
"    svg { display: block; }\n" +
"    .edge { stroke: #cbd5e0; stroke-width: 1.5; stroke-opacity: 0.3; fill: none; }\n" +
"    .edge.strong { stroke-width: 2.5; stroke-opacity: 0.5; stroke: #718096; }\n" +
"    .contradiction-edge { stroke: #e53e3e; stroke-width: 3; stroke-dasharray: 8,6; fill: none; opacity: 0.7; }\n" +
"    .contradiction-edge.high-tension { stroke-width: 4; opacity: 0.9; stroke: #c53030; }\n" +
"    .node-box { cursor: pointer; transition: all 0.2s; stroke-width: 2.5; }\n" +
"    .node-box.keep-doing { fill: #e6f7ed; stroke: #48bb78; }\n" +
"    .node-box.stop-doing { fill: #fee; stroke: #f56565; }\n" +
"    .node-box.start-doing { fill: #E0F4F7; stroke: #4AB5BE; }\n" +
"    .node-box:hover { stroke-width: 4; filter: brightness(0.95); }\n" +
"    .node-text { font-size: 23px; fill: #333; pointer-events: none; font-weight: 500; }\n" +
"    .contradiction-node-text { font-size: 16px; fill: #1a202c; pointer-events: none; font-weight: 600; }\n" +
"    .category-label { font-size: 24px; font-weight: bold; font-family: 'Permanent Marker', cursive; }\n" +
"    .tension-label { font-size: 14px; fill: #e53e3e; font-weight: bold; font-family: 'Permanent Marker', cursive; }\n" +
"    .connections-info { text-align: center; font-size: 14px; color: #718096; margin-top: 10px; font-style: italic; }\n" +
"    @media print { body { background: white; margin: 0; padding: 0; } .poster { box-shadow: none; } }\n" +
"</style>\n";
    }
    
    private static String getDelphiDashboardJS(String graphDataJson, String contradictionsJson) {

        return "<script>\n" +
"const similarityData = " + graphDataJson + ";\n" +
"const contradictionData = " + contradictionsJson + ";\n" +
"const ns = 'http://www.w3.org/2000/svg';\n" +
"const simSvg = document.getElementById('similarityGraph');\n" +
"const contraSvg = document.getElementById('contradictionGraph');\n" +
"function catConfig(cat) {\n" +
"    const c = cat.toLowerCase();\n" +
"    if (c.includes('keep')) return { fill: '#e6f7ed', stroke: '#48bb78', text: '#276749', bg: 'rgba(72,187,120,0.07)' };\n" +
"    if (c.includes('stop')) return { fill: '#fff5f5', stroke: '#f56565', text: '#c53030', bg: 'rgba(245,101,101,0.07)' };\n" +
"    return { fill: '#E0F4F7', stroke: '#4AB5BE', text: '#2C6E7A', bg: 'rgba(74,181,190,0.07)' };\n" +
"}\n" +
"function wrapSvgText(el, text, cx, cy, maxHW, lineH, chW) {\n" +
"    chW = chW || 5.9;\n" +
"    const words = text.split(' ');\n" +
"    const lines = [];\n" +
"    let cur = '';\n" +
"    words.forEach(w => {\n" +
"        const test = cur ? cur + ' ' + w : w;\n" +
"        if (test.length * chW > maxHW * 2 && cur) { lines.push(cur); cur = w; }\n" +
"        else cur = test;\n" +
"    });\n" +
"    if (cur) lines.push(cur);\n" +
"    const startY = cy - ((lines.length - 1) * lineH) / 2;\n" +
"    lines.forEach((l, i) => {\n" +
"        const ts = document.createElementNS(ns, 'tspan');\n" +
"        ts.setAttribute('x', cx); ts.setAttribute('y', startY + i * lineH);\n" +
"        ts.textContent = l;\n" +
"        el.appendChild(ts);\n" +
"    });\n" +
"}\n" +
"const svgW = parseInt(simSvg.getAttribute('width'));\n" +
"const svgH = parseInt(simSvg.getAttribute('height'));\n" +
"simSvg.style.overflow = 'visible';\n" +
"const r = 44;\n" +
"const availR = Math.min(svgW * 0.17, svgH * 0.21);\n" +
"const clusters = [\n" +
"    { key: 'keep', cx: svgW * 0.22, cy: svgH * 0.27 },\n" +
"    { key: 'start', cx: svgW * 0.78, cy: svgH * 0.27 },\n" +
"    { key: 'stop', cx: svgW * 0.50, cy: svgH * 0.74 }\n" +
"];\n" +
"const catGroups = { keep: [], stop: [], start: [] };\n" +
"similarityData.nodes.forEach(node => {\n" +
"    const c = node.category.toLowerCase();\n" +
"    if (c.includes('keep')) catGroups.keep.push(node);\n" +
"    else if (c.includes('stop')) catGroups.stop.push(node);\n" +
"    else catGroups.start.push(node);\n" +
"});\n" +
"clusters.forEach(cluster => {\n" +
"    const nodes = catGroups[cluster.key];\n" +
"    const n = nodes.length;\n" +
"    if (!n) { cluster.visR = 0; return; }\n" +
"    if (n === 1) { nodes[0].cx = cluster.cx; nodes[0].cy = cluster.cy; cluster.visR = r + 16; return; }\n" +
"    let rings;\n" +
"    if (n <= 7) rings = [n];\n" +
"    else if (n <= 14) { const n0 = Math.ceil(n * 0.62); rings = [n0, n - n0]; }\n" +
"    else { const n0 = Math.ceil(n * 0.52); const n1 = Math.ceil((n - n0) * 0.65); rings = [n0, n1, n - n0 - n1]; }\n" +
"    const rfracs = [0.72, 0.43, 0.18];\n" +
"    let idx = 0;\n" +
"    rings.forEach((count, ri) => {\n" +
"        const ringR = count <= 1 ? 0 : availR * rfracs[ri];\n" +
"        for (let i = 0; i < count && idx < n; i++, idx++) {\n" +
"            const a = count <= 1 ? 0 : (2 * Math.PI * i / count) - Math.PI / 2;\n" +
"            nodes[idx].cx = cluster.cx + ringR * Math.cos(a);\n" +
"            nodes[idx].cy = cluster.cy + ringR * Math.sin(a);\n" +
"        }\n" +
"    });\n" +
"    cluster.visR = availR + r + 12;\n" +
"});\n" +
"similarityData.edges.forEach(edge => {\n" +
"    const src = similarityData.nodes[edge.source];\n" +
"    const tgt = similarityData.nodes[edge.target];\n" +
"    if (src.cx == null || tgt.cx == null) return;\n" +
"    const ln = document.createElementNS(ns, 'line');\n" +
"    ln.setAttribute('x1', src.cx); ln.setAttribute('y1', src.cy);\n" +
"    ln.setAttribute('x2', tgt.cx); ln.setAttribute('y2', tgt.cy);\n" +
"    ln.setAttribute('stroke', '#a0aec0');\n" +
"    ln.setAttribute('stroke-width', edge.similarity > 0.55 ? '1.5' : '0.8');\n" +
"    ln.setAttribute('stroke-opacity', '0.22');\n" +
"    simSvg.appendChild(ln);\n" +
"});\n" +
"clusters.forEach(cluster => {\n" +
"    if (!cluster.visR) return;\n" +
"    const cfg = catConfig(cluster.key);\n" +
"    const bg = document.createElementNS(ns, 'circle');\n" +
"    bg.setAttribute('cx', cluster.cx); bg.setAttribute('cy', cluster.cy);\n" +
"    bg.setAttribute('r', cluster.visR);\n" +
"    bg.setAttribute('fill', cfg.bg);\n" +
"    bg.setAttribute('stroke', cfg.stroke);\n" +
"    bg.setAttribute('stroke-width', '2');\n" +
"    bg.setAttribute('stroke-dasharray', '8 5');\n" +
"    bg.setAttribute('stroke-opacity', '0.5');\n" +
"    simSvg.appendChild(bg);\n" +
"});\n" +
"const popR = r * 2.8;\n" +
"const popG = document.createElementNS(ns, 'g');\n" +
"popG.style.cursor = 'default';\n" +
"popG.style.display = 'none';\n" +
"const popBg = document.createElementNS(ns, 'circle');\n" +
"popBg.setAttribute('cx', svgW / 2); popBg.setAttribute('cy', svgH / 2);\n" +
"popBg.setAttribute('r', popR);\n" +
"popBg.setAttribute('stroke-width', '4');\n" +
"popG.appendChild(popBg);\n" +
"const popTxt = document.createElementNS(ns, 'text');\n" +
"popTxt.setAttribute('text-anchor', 'middle');\n" +
"popTxt.setAttribute('font-size', '23');\n" +
"popTxt.setAttribute('font-weight', '600');\n" +
"popTxt.setAttribute('pointer-events', 'none');\n" +
"popG.appendChild(popTxt);\n" +
"simSvg.appendChild(popG);\n" +
"let hideTimer = null;\n" +
"let activeCircle = null;\n" +
"function cancelHide() { clearTimeout(hideTimer); }\n" +
"function scheduleHide() {\n" +
"    hideTimer = setTimeout(() => {\n" +
"        popG.style.display = 'none';\n" +
"        if (activeCircle) {\n" +
"            activeCircle.setAttribute('stroke-width', '2.5');\n" +
"            activeCircle.setAttribute('opacity', '1');\n" +
"            activeCircle = null;\n" +
"        }\n" +
"    }, 140);\n" +
"}\n" +
"popG.addEventListener('mouseenter', cancelHide);\n" +
"popG.addEventListener('mouseleave', scheduleHide);\n" +
"similarityData.nodes.forEach(node => {\n" +
"    if (node.cx == null) return;\n" +
"    const cfg = catConfig(node.category);\n" +
"    const g = document.createElementNS(ns, 'g');\n" +
"    g.style.cursor = 'pointer';\n" +
"    const circle = document.createElementNS(ns, 'circle');\n" +
"    circle.setAttribute('cx', node.cx); circle.setAttribute('cy', node.cy);\n" +
"    circle.setAttribute('r', r);\n" +
"    circle.setAttribute('fill', cfg.fill);\n" +
"    circle.setAttribute('stroke', cfg.stroke);\n" +
"    circle.setAttribute('stroke-width', '2.5');\n" +
"    g.appendChild(circle);\n" +
"    const abbr = node.text.length > 35 ? node.text.substring(0, 32) + '\\u2026' : node.text;\n" +
"    const shortEl = document.createElementNS(ns, 'text');\n" +
"    shortEl.setAttribute('text-anchor', 'middle');\n" +
"    shortEl.setAttribute('fill', cfg.text);\n" +
"    shortEl.setAttribute('font-size', '11');\n" +
"    shortEl.setAttribute('font-weight', '500');\n" +
"    shortEl.setAttribute('pointer-events', 'none');\n" +
"    wrapSvgText(shortEl, abbr, node.cx, node.cy, r * 0.88, 14);\n" +
"    g.appendChild(shortEl);\n" +
"    simSvg.appendChild(g);\n" +
"    g.addEventListener('mouseenter', () => {\n" +
"        cancelHide();\n" +
"        if (activeCircle && activeCircle !== circle) {\n" +
"            activeCircle.setAttribute('stroke-width', '2.5');\n" +
"            activeCircle.setAttribute('opacity', '1');\n" +
"        }\n" +
"        activeCircle = circle;\n" +
"        simSvg.appendChild(g);\n" +
"        circle.setAttribute('stroke-width', '5');\n" +
"        circle.setAttribute('opacity', '0.7');\n" +
"        popBg.setAttribute('fill', cfg.fill);\n" +
"        popBg.setAttribute('stroke', cfg.stroke);\n" +
"        popTxt.setAttribute('fill', cfg.text);\n" +
"        while (popTxt.firstChild) popTxt.removeChild(popTxt.firstChild);\n" +
"        wrapSvgText(popTxt, node.text, svgW / 2, svgH / 2, popR * 0.82, 30, 12.5);\n" +
"        popG.style.display = '';\n" +
"        simSvg.appendChild(popG);\n" +
"    });\n" +
"    g.addEventListener('mouseleave', scheduleHide);\n" +
"});\n" +
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
                "        const startY = 60;\n" +
                "        const verticalSpacing = 120;\n" +
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
                "            leftG.addEventListener('mouseenter', () => contraSvg.appendChild(leftG));\n" +
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
                "            rightG.addEventListener('mouseenter', () => contraSvg.appendChild(rightG));\n" +
                "            contraSvg.appendChild(rightG);\n" +
                "        });\n" +
                "        \n" +




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
