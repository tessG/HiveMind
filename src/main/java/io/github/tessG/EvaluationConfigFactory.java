package io.github.tessG;
import java.util.Arrays;
import java.util.List;

/**
 * Factory for creating predefined evaluation configurations
 */
public class EvaluationConfigFactory {
    
    public static EvaluationConfig getDareShareCare() {
        List<Category> categories = Arrays.asList(
            new Category("DARE", "🔥", "#f56565"),
            new Category("SHARE", "🤝", "#48bb78"),
            new Category("CARE", "💙", "#4299e1")
        );
        
        return new EvaluationConfig(
            "dare-share-care",
            "Klassens værdier og fælles mål",
            categories,
            "#667eea",  // Header color
            "#764ba2"   // Summary color
        );
    }
    
    public static EvaluationConfig getDelphi() {
        List<Category> categories = Arrays.asList(
            new Category("Continue Doing", "✅", "#48bb78"),  // Green
            new Category("Do More", "🔼", "#4299e1"),         // Blue
            new Category("Stop Doing", "🛑", "#f56565"),      // Red
            new Category("Begin Doing", "⭐", "#ecc94b")      // Yellow
        );
        
        return new EvaluationConfig(
            "delphi",
            "Evaluering og forbedringspunkter",
            categories,
            "#805ad5",  // Purple header
            "#6b46c1"   // Darker purple summary
        );
    }
    
    public static EvaluationConfig getRetro() {
        List<Category> categories = Arrays.asList(
            new Category("Start", "▶️", "#48bb78"),
            new Category("Stop", "⏹️", "#f56565"),
            new Category("Continue", "➡️", "#4299e1")
        );
        
        return new EvaluationConfig(
            "retrospective",
            "Sprint Retrospective",
            categories,
            "#2d3748",
            "#4a5568"
        );
    }
    
    public static EvaluationConfig getConfig(String type) {
        switch (type.toLowerCase()) {
            case "dare-share-care":
            case "dsc":
                return getDareShareCare();
            case "delphi":
                return getDelphi();
            case "retrospective":
            case "retro":
                return getRetro();
            default:
                throw new IllegalArgumentException("Unknown evaluation type: " + type);
        }
    }
}
