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
 */
public class DelphiCsvParser {
    
    public static class CategorizedStatement {
        private String category;
        private String statement;
        private String comment;
        
        public CategorizedStatement(String category, String statement, String comment) {
            this.category = category;
            this.statement = statement;
            this.comment = comment;
        }
        
        public String getCategory() { return category; }
        public String getStatement() { return statement; }
        public String getComment() { return comment; }
        
        public String getFullStatement() {
            if (comment != null && !comment.trim().isEmpty()) {
                return statement + " (Kommentar: " + comment + ")";
            }
            return statement;
        }
    }
    
    /**
     * Parse categorized Delphi CSV
     * Returns map of category name -> list of statements
     */
    public Map<String, List<CategorizedStatement>> parseCategorizedCsv(String filepath) {
        Map<String, List<CategorizedStatement>> categorizedMap = new HashMap<>();
        
        try (CSVReader reader = new CSVReader(new FileReader(filepath))) {
            List<String[]> rows = reader.readAll();
            
            // Skip header row (index 0)
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                
                if (row.length < 2 || row[0].isEmpty() || row[1].isEmpty()) {
                    continue;
                }
                
                String category = row[0].trim();
                String statement = row[1].trim();
                String comment = row.length > 2 ? row[2].trim() : "";
                
                CategorizedStatement catStatement = new CategorizedStatement(category, statement, comment);
                
                categorizedMap.computeIfAbsent(category, k -> new ArrayList<>()).add(catStatement);
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
        Map<String, List<CategorizedStatement>> categorized = parseCategorizedCsv(filepath);
        
        for (List<CategorizedStatement> catStatements : categorized.values()) {
            for (CategorizedStatement cs : catStatements) {
                statements.add(cs.getFullStatement());
            }
        }
        
        return statements;
    }
    
    /**
     * Print summary of parsed statements
     */
    public void printSummary(Map<String, List<CategorizedStatement>> categorized) {
        System.out.println("\n=== Parsed Delphi Statements ===");
        int total = 0;
        for (Map.Entry<String, List<CategorizedStatement>> entry : categorized.entrySet()) {
            System.out.println("\n" + entry.getKey() + ": " + entry.getValue().size() + " statements");
            for (CategorizedStatement cs : entry.getValue()) {
                System.out.println("  - " + cs.getStatement());
            }
            total += entry.getValue().size();
        }
        System.out.println("\nTotal: " + total + " statements\n");
    }
}
