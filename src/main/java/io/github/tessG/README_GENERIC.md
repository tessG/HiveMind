# Generic Evaluation System

A flexible system for analyzing student feedback from Padlet and creating visual Miro boards. Supports multiple evaluation types out of the box.

## Supported Evaluation Types

### 1. Dare-Share-Care
- **DARE** 🔥 (Red): Things that require courage
- **SHARE** 🤝 (Green): Knowledge sharing
- **CARE** 💙 (Blue): Community support

### 2. Delphi Evaluation
- **Continue Doing** ✅ (Green): What's working well
- **Do More** 🔼 (Blue): What should be increased
- **Stop Doing** 🛑 (Red): What should be discontinued
- **Begin Doing** ⭐ (Yellow): New initiatives to start

### 3. Sprint Retrospective
- **Start** ▶️ (Green): New practices to begin
- **Stop** ⏹️ (Red): Practices to discontinue
- **Continue** ➡️ (Blue): Practices to maintain

## Architecture

```
GenericEvaluationWorkflow
├── Fetches statements from Padlet
├── Uses EvaluationConfig to get category definitions
├── Analyzes with Claude AI (dynamic prompts)
├── Creates Miro board (dynamic layout)
└── Returns Miro board URL
```

### Key Classes

**EvaluationConfig**
- Defines evaluation type, categories, and colors
- Configuration-driven approach

**Category**
- Represents a single category (name, emoji, color)

**EvaluationInsights**
- Generic data structure using Map<String, List<String>>
- Works with any number of categories

**GenericMiroBoardBuilder**
- Dynamically creates N columns based on config
- Adapts layout to category count

**EvaluationConfigFactory**
- Pre-defined configs for common evaluation types
- Easy to add new types

## Usage

### Basic Usage

```java
String padletApiKey = System.getenv("PADLET_API_KEY");
String miroToken = System.getenv("MIRO_ACCESS_TOKEN");

GenericEvaluationWorkflow workflow = new GenericEvaluationWorkflow(
    padletApiKey, 
    miroToken
);

// For Dare-Share-Care
String boardUrl = workflow.executeWorkflow(padletId, "dare-share-care");

// For Delphi
String boardUrl = workflow.executeWorkflow(padletId, "delphi");

// For Retrospective
String boardUrl = workflow.executeWorkflow(padletId, "retrospective");
```

### Adding a New Evaluation Type

Add to `EvaluationConfigFactory.java`:

```java
public static EvaluationConfig getRoseThorn() {
    List<Category> categories = Arrays.asList(
        new Category("Rose", "🌹", "#e53e3e"),      // Red
        new Category("Thorn", "🌵", "#dd6b20"),     // Orange
        new Category("Bud", "🌱", "#38a169")        // Green
    );
    
    return new EvaluationConfig(
        "rose-thorn-bud",
        "What went well, challenges, and opportunities",
        categories,
        "#742a2a",  // Header color
        "#9c4221"   // Summary color
    );
}
```

Then add to `getConfig()` switch statement:

```java
case "rose-thorn-bud":
    return getRoseThorn();
```

### Custom Evaluation Type at Runtime

```java
List<Category> customCategories = Arrays.asList(
    new Category("Strengths", "💪", "#48bb78"),
    new Category("Weaknesses", "🔻", "#f56565"),
    new Category("Opportunities", "🎯", "#4299e1"),
    new Category("Threats", "⚠️", "#ed8936")
);

EvaluationConfig swotConfig = new EvaluationConfig(
    "swot",
    "SWOT Analysis",
    customCategories,
    "#2d3748",
    "#4a5568"
);

// Use directly without factory
EvaluationInsights insights = analyzeWithClaude(statements, swotConfig);
String boardId = builder.createInsightsBoard(insights, swotConfig, "SWOT Board");
```

## Migration from Old Code

### Old (StudentInsights)
```java
StudentInsights insights = new StudentInsights(
    headline,
    dareItems,
    shareItems,
    careItems,
    summary,
    keyInsight
);
```

### New (EvaluationInsights)
```java
Map<String, List<String>> items = new HashMap<>();
items.put("DARE", dareItems);
items.put("SHARE", shareItems);
items.put("CARE", careItems);

EvaluationInsights insights = new EvaluationInsights(
    headline,
    items,
    summary,
    keyInsight
);
```

## Color Scheme Guidelines

- **Red (#f56565)**: Stop, risks, challenges
- **Green (#48bb78)**: Continue, successes, growth
- **Blue (#4299e1)**: Learning, development, future
- **Yellow (#ecc94b)**: New ideas, opportunities
- **Purple (#805ad5)**: Leadership, vision, reflection
- **Orange (#ed8936)**: Energy, action, urgency

## API Requirements

- **Padlet API Key**: Get from Padlet developer portal
- **Claude API Key**: Set as `ANTHROPIC_API_KEY` environment variable
- **Miro Access Token**: Get from Miro app settings

## Dependencies

```xml
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
<dependency>
    <groupId>com.anthropic</groupId>
    <artifactId>anthropic-java</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Future Enhancements

- [ ] Web UI for selecting evaluation type
- [ ] Custom category builder interface
- [ ] Template library for common evaluation types
- [ ] Multi-language support
- [ ] Export to PDF/PowerPoint
- [ ] Historical trend analysis
- [ ] Real-time collaboration during Padlet collection

## License

MIT License - Use for educational purposes
