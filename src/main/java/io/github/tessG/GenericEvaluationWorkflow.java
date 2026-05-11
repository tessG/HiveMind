package io.github.tessG;

import java.util.*;

public class GenericEvaluationWorkflow {

    private final PadletParser padletParser;

    public GenericEvaluationWorkflow(String padletApiKey) {
        this.padletParser = new PadletParser(padletApiKey);
    }

    public GenericEvaluationWorkflow() {
        this.padletParser = null;
    }

    public String executeDSCWorkflow(String padletId, String evaluationType) throws Exception {
        System.out.println("🔄 Starting DSC workflow from Padlet");

        EvaluationConfig config = EvaluationConfigFactory.getConfig(evaluationType);

        System.out.println("📥 Fetching statements from Padlet...");
        List<String> statements = padletParser.fetchStatements(padletId);
        System.out.println("✅ Retrieved " + statements.size() + " statements");

        System.out.println("🤖 Analyzing with Claude...");
        Map<String, String> analysis = ClaudeAnalysisService.analyzeSummaryAndInsights(statements);

        System.out.println("🎨 Generating DSC poster...");
        String htmlPoster = PosterGenerator.generateDSCPoster(statements, config, analysis);
        System.out.println("✅ Poster generated");

        return savePoster(htmlPoster, evaluationType);
    }

    public String executeDelphiFromPadlet(String padletId, String evaluationType) throws Exception {
        System.out.println("📄 Starting Delphi workflow from Padlet");

        System.out.println("📥 Fetching categorized statements from Padlet...");
        Map<String, List<Statement>> categorized = padletParser.fetchCategorizedStatements(padletId);

        if (categorized.isEmpty()) {
            throw new RuntimeException("No statements found in Padlet");
        }

        return runDelphiPipeline(categorized, evaluationType);
    }

    public String executeDSCFromCsv(String csvPath) throws Exception {
        System.out.println("🎨 Starting DSC workflow from CSV export");

        DelphiCsvParser parser = new DelphiCsvParser();
        List<String> statements = parser.parseDscExportCsv(csvPath);
        System.out.println("✅ Retrieved " + statements.size() + " statements");

        EvaluationConfig config = EvaluationConfigFactory.getConfig("dare-share-care");

        System.out.println("🤖 Analyzing with Claude...");
        Map<String, String> analysis = ClaudeAnalysisService.analyzeSummaryAndInsights(statements);

        System.out.println("🎨 Generating DSC poster...");
        String htmlPoster = PosterGenerator.generateDSCPoster(statements, config, analysis);

        return savePoster(htmlPoster, "dare-share-care");
    }

    public String executeDelphiFromCsv(String csvPath, String evaluationType) throws Exception {
        System.out.println("📄 Starting Delphi workflow from CSV");

        System.out.println("📥 Parsing CSV file...");
        DelphiCsvParser parser = new DelphiCsvParser();
        Map<String, List<Statement>> categorized = parser.parseCategorizedCsv(csvPath);

        if (categorized.isEmpty()) {
            throw new RuntimeException("No statements found in CSV file");
        }

        parser.printSummary(categorized);

        return runDelphiPipeline(categorized, evaluationType);
    }

    private String runDelphiPipeline(Map<String, List<Statement>> categorized, String evaluationType) throws Exception {
        EvaluationConfig config = EvaluationConfigFactory.getConfig(evaluationType);

        List<String> allStatements = new ArrayList<>();
        for (List<Statement> statements : categorized.values()) {
            for (Statement stmt : statements) {
                allStatements.add(stmt.getFullStatement());
            }
        }

        System.out.println("🤖 Analyzing with Claude for summary and insights...");
        Map<String, String> analysis = ClaudeAnalysisService.analyzeSummaryAndInsights(allStatements);

        System.out.println("⚡ Detecting contradictions...");
        List<Map<String, Object>> contradictions = ClaudeAnalysisService.detectContradictions(categorized);

        System.out.println("💡 Generating suggestions for teachers...");
        List<String> suggestions = ClaudeAnalysisService.generateSuggestions(allStatements);

        System.out.println("🔗 Calculating similarities...");
        List<Map<String, Object>> nodes = GraphDataService.buildNodes(categorized);
        List<Map<String, Object>> edges = GraphDataService.calculateSimilarities(nodes, 0.35);

        System.out.println("✅ Found " + edges.size() + " connections");

        GraphDataService.positionNodesForPoster(nodes);

        System.out.println("🎨 Generating dashboard poster...");
        String htmlPoster = PosterGenerator.generateDelphiDashboard(
                nodes, edges, config, analysis, contradictions, suggestions, categorized
        );
        System.out.println("✅ Poster generated");

        return savePoster(htmlPoster, evaluationType);
    }

    private String savePoster(String htmlPoster, String evaluationType) throws Exception {
        String filename = "poster-" + evaluationType + "-" + java.time.LocalDate.now() + ".html";
        java.nio.file.Files.writeString(
                java.nio.file.Paths.get(filename),
                htmlPoster
        );
        System.out.println("💾 Saved to: " + filename);
        return filename;
    }
}
