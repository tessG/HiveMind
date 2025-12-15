package io.github.tessG;

import java.util.Scanner;

/**
 * Main application with interactive menu for evaluation workflows
 */
public class Main {
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // Check environment variables
        String padletApiKey = System.getenv("PADLET_API_KEY");
        String anthropicKey = System.getenv("ANTHROPIC_API_KEY");
        
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║   Student Evaluation Manager           ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        // Check API keys
        System.out.println("Environment check:");
        System.out.println("  Anthropic API: " + (anthropicKey != null ? "✅" : "❌ Missing"));
        System.out.println("  Padlet API: " + (padletApiKey != null ? "✅" : "❌ Missing"));
        System.out.println();
        
        while (true) {
            System.out.println("═══════════════════════════════════════");
            System.out.println("Select workflow:");
            System.out.println("═══════════════════════════════════════");
            System.out.println("1. Dare-Share-Care (from Padlet)");
            System.out.println("2. Delphi (from Padlet)");
            System.out.println("3. Delphi (from CSV - direct to HTML)");
            System.out.println("4. Delphi Similarity Graph (from CSV)");
            System.out.println("5. Exit");
            System.out.println("═══════════════════════════════════════");
            System.out.print("Choice: ");
            
            String choice = scanner.nextLine().trim();
            
            try {
                switch (choice) {
                    case "1":
                        runDareShareCareWorkflow(padletApiKey, scanner);
                        break;
                        
                    case "2":
                        runDelphiWorkflow(padletApiKey, scanner);
                        break;
                        
                    case "3":
                        runDelphiDirectFromCsv(scanner);
                        break;
                        
                    case "4":
                        runDelphiSimilarityGraph(scanner);
                        break;
                        
                    case "5":
                        System.out.println("\n👋 Goodbye!");
                        return;
                        
                    default:
                        System.out.println("❌ Invalid choice. Please try again.\n");
                }
            } catch (Exception e) {
                System.err.println("❌ Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private static void runDareShareCareWorkflow(String padletApiKey, Scanner scanner) throws Exception {
        if (padletApiKey == null) {
            System.out.println("❌ Missing PADLET_API_KEY");
            return;
        }
        
        System.out.print("Enter Padlet ID: ");
        String padletId = scanner.nextLine().trim();
        
        if (padletId.isEmpty()) {
            System.out.println("❌ Padlet ID cannot be empty");
            return;
        }
        
        System.out.println("\n🚀 Starting Dare-Share-Care workflow...\n");
        
        GenericEvaluationWorkflow workflow = new GenericEvaluationWorkflow(padletApiKey, null);
        String result = workflow.executeWorkflow(padletId, "dare-share-care");
        
        System.out.println("\n✨ Workflow completed!");
        System.out.println("📄 HTML file: " + result);
        System.out.println();
    }
    
    private static void runDelphiWorkflow(String padletApiKey, Scanner scanner) throws Exception {
        if (padletApiKey == null) {
            System.out.println("❌ Missing PADLET_API_KEY");
            return;
        }
        
        System.out.print("Enter Padlet ID: ");
        String padletId = scanner.nextLine().trim();
        
        if (padletId.isEmpty()) {
            System.out.println("❌ Padlet ID cannot be empty");
            return;
        }
        
        System.out.println("\nSelect Delphi format:");
        System.out.println("1. Keep/Stop/Start (3 categories)");
        System.out.println("2. Continue/More/Stop/Begin (4 categories)");
        System.out.print("Choice: ");
        
        String formatChoice = scanner.nextLine().trim();
        String evaluationType = formatChoice.equals("2") ? "delphi-4" : "delphi";
        
        System.out.println("\n🚀 Starting Delphi workflow...\n");
        
        GenericEvaluationWorkflow workflow = new GenericEvaluationWorkflow(padletApiKey, null);
        String result = workflow.executeWorkflow(padletId, evaluationType);
        
        System.out.println("\n✨ Workflow completed!");
        System.out.println("📄 HTML file: " + result);
        System.out.println();
    }
    
    private static void runDelphiDirectFromCsv(Scanner scanner) throws Exception {
        System.out.println("\n📊 Delphi Evaluation - Direct from CSV");
        System.out.println("Generates HTML poster directly without using Padlet");
        System.out.println();
        
        System.out.print("Enter CSV file path: ");
        String csvPath = scanner.nextLine().trim();
        
        if (csvPath.isEmpty()) {
            System.out.println("❌ File path cannot be empty");
            return;
        }
        
        System.out.println("\nSelect Delphi format:");
        System.out.println("1. Keep/Stop/Start (3 categories)");
        System.out.println("2. Continue/More/Stop/Begin (4 categories)");
        System.out.print("Choice: ");
        
        String formatChoice = scanner.nextLine().trim();
        String evaluationType = formatChoice.equals("2") ? "delphi-4" : "delphi";
        
        System.out.println();
        
        DelphiDirectWorkflow workflow = new DelphiDirectWorkflow();
        String filename = workflow.generatePosterFromCsv(csvPath, evaluationType);
        
        System.out.println("\n✨ Poster generated!");
        System.out.println("📄 File: " + filename);
        System.out.println("💡 Open in browser to view");
        System.out.println();
    }
    
    private static void runDelphiSimilarityGraph(Scanner scanner) throws Exception {
        System.out.println("\n🔗 Delphi Similarity Graph");
        System.out.println("Generates network graph showing connections between statements");
        System.out.println();
        
        System.out.print("Enter CSV file path: ");
        String csvPath = scanner.nextLine().trim();
        
        if (csvPath.isEmpty()) {
            System.out.println("❌ File path cannot be empty");
            return;
        }
        
        System.out.println();
        
        DelphiDirectWorkflow workflow = new DelphiDirectWorkflow();
        String filename = workflow.generatePosterFromCsv(csvPath, "delphi");
        
        System.out.println("\n✨ Similarity graph generated!");
        System.out.println("📄 File: " + filename);
        System.out.println("💡 Open in browser to view");
        System.out.println();
    }
}
