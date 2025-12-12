package io.github.tessG;
/**
 * Represents a category in an evaluation (e.g., DARE, Continue Doing, etc.)
 */
public class Category {
    private String name;
    private String emoji;
    private String color;
    
    public Category(String name, String emoji, String color) {
        this.name = name;
        this.emoji = emoji;
        this.color = color;
    }
    
    public String getName() {
        return name;
    }
    
    public String getEmoji() {
        return emoji;
    }
    
    public String getColor() {
        return color;
    }
    
    public String getDisplayName() {
        return emoji + " " + name;
    }
}
