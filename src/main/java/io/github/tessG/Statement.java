package io.github.tessG;

/**
 * Unified model for evaluation statements
 * Used by both Delphi and DSC workflows
 */
public class Statement {
    private String text;
    private String category;
    private int weight;
    private String comment;
    
    public Statement(String text, String category, int weight, String comment) {
        this.text = text;
        this.category = category;
        this.weight = weight;
        this.comment = comment;
    }
    
    // Constructor with default weight
    public Statement(String text, String category, String comment) {
        this(text, category, 0, comment);
    }
    
    // Constructor with no comment
    public Statement(String text, String category, int weight) {
        this(text, category, weight, "");
    }
    
    // Simple constructor
    public Statement(String text, String category) {
        this(text, category, 0, "");
    }
    
    public String getText() {
        return text;
    }
    
    public String getCategory() {
        return category;
    }
    
    public int getWeight() {
        return weight;
    }
    
    public String getComment() {
        return comment;
    }
    
    /**
     * Get full statement including comment if present
     */
    public String getFullStatement() {
        if (comment != null && !comment.trim().isEmpty()) {
            return text + " (" + comment + ")";
        }
        return text;
    }
    
    @Override
    public String toString() {
        return "Statement{" +
                "category='" + category + '\'' +
                ", text='" + text + '\'' +
                ", weight=" + weight +
                '}';
    }
}
