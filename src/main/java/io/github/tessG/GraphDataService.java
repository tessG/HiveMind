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
        
        // Keep Doing - top left
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
        
        // Start Doing - top right
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
