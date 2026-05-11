package io.github.tessG;

import java.util.*;

/**
 * Service for graph data processing
 * Builds nodes, calculates similarities, positions for visualization
 * SHARED by both Delphi and DSC workflows
 */
public class GraphDataService {
    
    /**
     * Build nodes from categorized statements
     */
    public static List<Map<String, Object>> buildNodes(Map<String, List<Statement>> categorized) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        int id = 0;
        
        for (Map.Entry<String, List<Statement>> entry : categorized.entrySet()) {
            String category = entry.getKey();
            for (Statement stmt : entry.getValue()) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", id++);
                node.put("category", category);
                node.put("text", stmt.getText());
                node.put("weight", stmt.getWeight());
                node.put("comment", stmt.getComment());
                
                // Calculate box size based on text length
                int textLen = stmt.getText().length();
                if (textLen < 25) {
                    node.put("width", 180);
                    node.put("height", 90);
                } else if (textLen < 45) {
                    node.put("width", 200);
                    node.put("height", 108);
                } else if (textLen < 65) {
                    node.put("width", 215);
                    node.put("height", 124);
                } else {
                    node.put("width", 230);
                    node.put("height", 140);
                }
                
                nodes.add(node);
            }
        }
        
        return nodes;
    }
    
    /**
     * Calculate similarity edges between nodes
     */
    public static List<Map<String, Object>> calculateSimilarities(List<Map<String, Object>> nodes, double threshold) {
        List<Map<String, Object>> edges = new ArrayList<>();
        
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                String text1 = (String) nodes.get(i).get("text");
                String text2 = (String) nodes.get(j).get("text");
                double sim = calculateTextSimilarity(text1, text2);
                
                if (sim >= threshold) {
                    Map<String, Object> edge = new HashMap<>();
                    edge.put("source", i);
                    edge.put("target", j);
                    edge.put("similarity", sim);
                    edges.add(edge);
                }
            }
        }
        
        return edges;
    }
    
    /**
     * Position nodes for poster layout (3 categories: Keep, Stop, Start)
     * Uses non-overlapping x zones so groups can never cover each other:
     *   Keep Doing:  left   x=[20,  380]
     *   Stop Doing:  center x=[420, 780]  starting at vertical midpoint
     *   Start Doing: right  x=[820, 1180]
     * ySpacing is computed dynamically per group to fill the available height.
     */
    public static void positionNodesForPoster(List<Map<String, Object>> nodes) {
        List<Map<String, Object>> keepDoing = new ArrayList<>();
        List<Map<String, Object>> stopDoing = new ArrayList<>();
        List<Map<String, Object>> startDoing = new ArrayList<>();

        for (Map<String, Object> node : nodes) {
            String category = (String) node.get("category");
            if (category.equals("Keep Doing")) keepDoing.add(node);
            else if (category.equals("Stop Doing")) stopDoing.add(node);
            else if (category.equals("Start Doing")) startDoing.add(node);
        }

        // 2 columns per zone gives wider nodes and more readable text.
        // Zones are non-overlapping in x so groups can never cover each other:
        //   Keep Doing:  x=[20,  445]  (2 cols × 230px, xSpacing=215)
        //   Stop Doing:  x=[415, 840]  (2 cols × 230px, xSpacing=215, starts at y=midpoint)
        //   Start Doing: x=[815, 1240] (2 cols × 230px, xSpacing=215)
        // ySpacing is computed so the last row's bottom edge reaches yBottom exactly.
        final int cols = 2;
        final int xSpacing = 215;
        final int maxNodeHeight = 140;
        final int svgHeight = 1080;
        final int yTop = 80;
        final int yBottom = 1060;
        final int fullHeight = yBottom - yTop;       // 980px
        final int stopYStart = svgHeight / 2;        // 540
        final int stopAvail = yBottom - stopYStart;  // 520px

        // Keep Doing — left zone, full height
        int keepRows = Math.max(1, (int) Math.ceil((double) keepDoing.size() / cols));
        int keepYSpacing = keepRows > 1
                ? Math.min(200, (fullHeight - maxNodeHeight) / (keepRows - 1))
                : 0;
        for (int i = 0; i < keepDoing.size(); i++) {
            Map<String, Object> node = keepDoing.get(i);
            node.put("x", 20 + (i % cols) * xSpacing);
            node.put("y", yTop + (i / cols) * keepYSpacing);
        }

        // Start Doing — right zone, full height
        int startRows = Math.max(1, (int) Math.ceil((double) startDoing.size() / cols));
        int startYSpacing = startRows > 1
                ? Math.min(200, (fullHeight - maxNodeHeight) / (startRows - 1))
                : 0;
        for (int i = 0; i < startDoing.size(); i++) {
            Map<String, Object> node = startDoing.get(i);
            node.put("x", 815 + (i % cols) * xSpacing);
            node.put("y", yTop + (i / cols) * startYSpacing);
        }

        // Stop Doing — center zone, lower half
        int stopRows = Math.max(1, (int) Math.ceil((double) stopDoing.size() / cols));
        int stopYSpacing = stopRows > 1
                ? Math.min(200, (stopAvail - maxNodeHeight) / (stopRows - 1))
                : 0;
        for (int i = 0; i < stopDoing.size(); i++) {
            Map<String, Object> node = stopDoing.get(i);
            node.put("x", 415 + (i % cols) * xSpacing);
            node.put("y", stopYStart + (i / cols) * stopYSpacing);
        }
    }
    
    /**
     * Calculate text similarity using Levenshtein distance
     */
    private static double calculateTextSimilarity(String s1, String s2) {
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
    
    /**
     * Find which category a statement belongs to (for contradiction matching)
     */
    public static String findStatementCategory(String statementText, Map<String, List<Statement>> categorized) {
        for (Map.Entry<String, List<Statement>> entry : categorized.entrySet()) {
            for (Statement stmt : entry.getValue()) {
                if (stmt.getText().equals(statementText) || 
                    stmt.getText().contains(statementText) ||
                    statementText.contains(stmt.getText())) {
                    return entry.getKey();
                }
            }
        }
        return "Unknown";
    }
}
