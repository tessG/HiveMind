package io.github.tessG;

import java.util.*;

/**
 * Refactored Delphi workflow using service classes
 * Processes CSV files to generate dashboard posters
 */
public class DelphiDirectWorkflow {
    
    /**
     * Generate poster from CSV file
     */
    public String generatePosterFromCsv(String csvFilePath, String evaluationType) throws Exception {
        System.out.println("📄 Starting Delphi workflow from CSV");
        
        // Get configuration
        EvaluationConfig config = EvaluationConfigFactory.getConfig(evaluationType);
        
        // STEP 1: Parse CSV using DelphiCsvParser
        System.out.println("📥 Parsing CSV file...");
        DelphiCsvParser parser = new DelphiCsvParser();
        Map<String, List<Statement>> categorized = parser.parseCategorizedCsv(csvFilePath);
        
        if (categorized.isEmpty()) {
            throw new RuntimeException("No statements found in CSV file");
        }
        
        parser.printSummary(categorized);
        
        // STEP 2: Convert to list for Claude analysis
        List<String> allStatements = new ArrayList<>();
        for (List<Statement> statements : categorized.values()) {
            for (Statement stmt : statements) {
                allStatements.add(stmt.getFullStatement());
            }
        }
        
        // STEP 3: Analyze with Claude using ClaudeAnalysisService
        System.out.println("🤖 Analyzing with Claude for summary and insights...");
        Map<String, String> analysis = ClaudeAnalysisService.analyzeSummaryAndInsights(allStatements);
        
        System.out.println("⚡ Detecting contradictions...");
        List<Map<String, Object>> contradictions = ClaudeAnalysisService.detectContradictions(categorized);
        
        System.out.println("💡 Generating suggestions for teachers...");
        List<String> suggestions = ClaudeAnalysisService.generateSuggestions(allStatements);
        
        // STEP 4: Build graph data using GraphDataService
        System.out.println("🔗 Calculating similarities...");
        List<Map<String, Object>> nodes = GraphDataService.buildNodes(categorized);
        List<Map<String, Object>> edges = GraphDataService.calculateSimilarities(nodes, 0.35);
        
        System.out.println("✅ Found " + edges.size() + " connections");
        
        // Position nodes for poster layout
        GraphDataService.positionNodesForPoster(nodes);
        
        // STEP 5: Generate poster HTML using PosterGenerator
        System.out.println("🎨 Generating dashboard poster...");
        String htmlPoster = PosterGenerator.generateDelphiDashboard(
            nodes, edges, config, analysis, contradictions, suggestions, categorized
        );
        System.out.println("✅ Poster generated");
        
        // STEP 6: Save to file
        String filename = "poster-" + evaluationType + "-" + java.time.LocalDate.now() + ".html";
        java.nio.file.Files.writeString(
            java.nio.file.Paths.get(filename),
            htmlPoster
        );
        
        System.out.println("💾 Saved to: " + filename);
        return filename;
    }
}
