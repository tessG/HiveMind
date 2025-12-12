package io.github.tessG;
import java.util.List;

/**
 * Configuration for an evaluation type
 */
public class EvaluationConfig {
    private String type;
    private String title;
    private List<Category> categories;
    private String headerColor;
    private String summaryColor;
    
    public EvaluationConfig(String type, String title, List<Category> categories, 
                           String headerColor, String summaryColor) {
        this.type = type;
        this.title = title;
        this.categories = categories;
        this.headerColor = headerColor;
        this.summaryColor = summaryColor;
    }
    
    public String getType() {
        return type;
    }
    
    public String getTitle() {
        return title;
    }
    
    public List<Category> getCategories() {
        return categories;
    }
    
    public String getHeaderColor() {
        return headerColor;
    }
    
    public String getSummaryColor() {
        return summaryColor;
    }
    
    public int getCategoryCount() {
        return categories.size();
    }
}
