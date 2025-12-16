package io.github.tessG;

import java.util.*;

/**
 * Refactored Generic workflow using service classes
 * Supports both DSC and Delphi evaluations from Padlet
 */
public class GenericEvaluationWorkflow {
    
    private final PadletParser padletParser;
    
    public GenericEvaluationWorkflow(String padletApiKey) {
        this.padletParser = new PadletParser(padletApiKey);
    }
    
    /**
     * Execute DSC workflow from Padlet (Dare-Share-Care)
     * Simple workflow without categories
     */
    public String executeDSCWorkflow(String padletId, String evaluationType) throws Exception {
        System.out.println("🔄 Starting DSC workflow from Padlet");
        
        // Get configuration
        EvaluationConfig config = EvaluationConfigFactory.getConfig(evaluationType);
        
        // STEP 1: Fetch statements from Padlet
        System.out.println("📥 Fetching statements from Padlet...");
        List<String> statements = padletParser.fetchStatements(padletId);
        System.out.println("✅ Retrieved " + statements.size() + " statements");
        
        // STEP 2: Analyze with Claude
        System.out.println("🤖 Analyzing with Claude...");
        Map<String, String> analysis = ClaudeAnalysisService.analyzeSummaryAndInsights(statements);
        
        // STEP 3: Generate poster (DSC format - to be implemented)
        System.out.println("🎨 Generating DSC poster...");

        String htmlPoster = PosterGenerator.generateDSCPoster(statements, config, analysis);
        System.out.println("✅ Poster generated");
        
        // STEP 4: Save to file
        String filename = "poster-" + evaluationType + "-" + java.time.LocalDate.now() + ".html";
        java.nio.file.Files.writeString(
            java.nio.file.Paths.get(filename),
            htmlPoster
        );
        
        System.out.println("💾 Saved to: " + filename);
        return filename;
    }
    
    /**
     * Execute Delphi workflow from Padlet
     * Full dashboard with categories, contradictions, suggestions
     */
    public String executeDelphiWorkflow(String padletId, String evaluationType) throws Exception {
        System.out.println("📄 Starting Delphi workflow from Padlet");
        
        // Get configuration
        EvaluationConfig config = EvaluationConfigFactory.getConfig(evaluationType);
        
        // STEP 1: Fetch categorized statements from Padlet
        System.out.println("📥 Fetching categorized statements from Padlet...");
        Map<String, List<Statement>> categorized = padletParser.fetchCategorizedStatements(padletId);
        
        if (categorized.isEmpty()) {
            throw new RuntimeException("No statements found in Padlet");
        }
        
        // STEP 2: Convert to list for analysis
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
        String filename = "poster-" + evaluationType + "-padlet-" + java.time.LocalDate.now() + ".html";
        java.nio.file.Files.writeString(
            java.nio.file.Paths.get(filename),
            htmlPoster
        );
        
        System.out.println("💾 Saved to: " + filename);
        return filename;
    }
    
    /**
     * Temporary DSC poster generation
     * TODO: Move to PosterGenerator.generateDSCPoster()
     */
    private String generateDSCPosterTemp(List<String> statements, EvaluationConfig config,
                                        Map<String, String> analysis) {
        return "<!DOCTYPE html><html><body>" +
               "<h1>DSC Poster - To Be Implemented</h1>" +
               "<p>Headline: " + analysis.get("headline") + "</p>" +
               "<p>Summary: " + analysis.get("summary") + "</p>" +
               "</body></html>";
    }



}
