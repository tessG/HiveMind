package io.github.tessG;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Generic data structure for evaluation insights
 * Works for any number of categories
 */
public class EvaluationInsights {
    private String headline;
    private Map<String, List<String>> categorizedItems;
    private String summary;
    private String keyInsight;
    private String humorousStatement;  // ADD THIS

    public EvaluationInsights(String headline, 
                             Map<String, List<String>> categorizedItems,
                             String summary, 
                             String keyInsight,
                              String humorousStatement) {
        this.headline = headline;
        this.categorizedItems = categorizedItems;
        this.summary = summary;
        this.keyInsight = keyInsight;
        this.humorousStatement = humorousStatement;
    }
    
    public String getHeadline() {
        return headline;
    }
    
    public Map<String, List<String>> getCategorizedItems() {
        return categorizedItems;
    }
    
    public List<String> getItemsForCategory(String categoryName) {
        return categorizedItems.get(categoryName);
    }
    
    public String getSummary() {
        return summary;
    }
    
    public String getKeyInsight() {
        return keyInsight;
    }
    
    // Helper method for easy access
    public int getCategoryCount() {
        return categorizedItems.size();
    }
    
    // Helper for getting max items across all categories (for layout calculations)
    public int getMaxItemsInAnyCategory() {
        return categorizedItems.values().stream()
            .mapToInt(List::size)
            .max()
            .orElse(0);
    }

    public String getHumorousStatement() {
        return humorousStatement;
    }
}
