package io.github.tessG;
import java.util.Arrays;
import java.util.List;

/**
 * Factory for creating predefined evaluation configurations
 * UPDATED to support proper Delphi Keep/Stop/Start format
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
    
    /**
     * UPDATED: Proper Keep/Stop/Start Delphi configuration
     */
    public static EvaluationConfig getDelphi() {
        List<Category> categories = Arrays.asList(
            new Category("Keep Doing", "✅", "#48bb78"),      // Green - continue
            new Category("Stop Doing", "🛑", "#f56565"),      // Red - discontinue
            new Category("Start Doing", "⭐", "#4299e1")      // Blue - new actions
        );
        
        return new EvaluationConfig(
            "delphi",
            "Student Evaluering - Keep, Stop, Start",
            categories,
            "#805ad5",  // Purple header
            "#6b46c1"   // Darker purple summary
        );
    }
    
    /**
     * Alternative 4-category Delphi if needed
     */
    public static EvaluationConfig getDelphiFourCategory() {
        List<Category> categories = Arrays.asList(
            new Category("Continue Doing", "✅", "#48bb78"),  // Green
            new Category("Do More", "🔼", "#4299e1"),         // Blue
            new Category("Stop Doing", "🛑", "#f56565"),      // Red
            new Category("Begin Doing", "⭐", "#ecc94b")      // Yellow
        );
        
        return new EvaluationConfig(
            "delphi-4cat",
            "Evaluering og forbedringspunkter",
            categories,
            "#805ad5",  
            "#6b46c1"   
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
            case "keep-stop-start":
                return getDelphi();
            case "delphi-4":
            case "delphi-four":
                return getDelphiFourCategory();
            case "retrospective":
            case "retro":
                return getRetro();
            default:
                throw new IllegalArgumentException("Unknown evaluation type: " + type);
        }
    }
}
