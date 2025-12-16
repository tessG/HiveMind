package io.github.tessG;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for Delphi evaluation CSV files
 * CSV format: Category,Statement,Comment
 * Now uses unified Statement model
 */
public class DelphiCsvParser {
    
    /**
     * Parse categorized Delphi CSV
     * Returns map of category name -> list of statements
     */
    public Map<String, List<Statement>> parseCategorizedCsv(String filepath) {
        Map<String, List<Statement>> categorizedMap = new HashMap<>();
        
        try (CSVReader reader = new CSVReader(new FileReader(filepath))) {
            List<String[]> rows = reader.readAll();
            
            // Skip header row (index 0)
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                
                if (row.length < 2 || row[0].isEmpty() || row[1].isEmpty()) {
                    continue;
                }
                
                String category = row[0].trim();
                String text = row[1].trim();
                String comment = row.length > 2 ? row[2].trim() : "";
                int weight = 0; // CSV doesn't have weights, default to 0
                
                Statement statement = new Statement(text, category, weight, comment);
                
                categorizedMap.computeIfAbsent(category, k -> new ArrayList<>()).add(statement);
            }
            
        } catch (IOException | CsvException e) {
            System.err.println("Error parsing Delphi CSV: " + e.getMessage());
            e.printStackTrace();
        }
        
        return categorizedMap;
    }
    
    /**
     * Parse and return all statements (for Claude analysis without categories)
     */
    public List<String> parseDelphiCsv(String filepath) {
        List<String> statements = new ArrayList<>();
        Map<String, List<Statement>> categorized = parseCategorizedCsv(filepath);
        
        for (List<Statement> catStatements : categorized.values()) {
            for (Statement stmt : catStatements) {
                statements.add(stmt.getFullStatement());
            }
        }
        
        return statements;
    }
    
    /**
     * Print summary of parsed statements
     */
    public void printSummary(Map<String, List<Statement>> categorized) {
        System.out.println("\n=== Parsed Delphi Statements ===");
        int total = 0;
        for (Map.Entry<String, List<Statement>> entry : categorized.entrySet()) {
            System.out.println("\n" + entry.getKey() + ": " + entry.getValue().size() + " statements");
            for (Statement stmt : entry.getValue()) {
                System.out.println("  - " + stmt.getText());
            }
            total += entry.getValue().size();
        }
        System.out.println("\nTotal: " + total + " statements\n");
    }
}
