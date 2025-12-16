# Student Evaluation System - Clean & Simple

## Overview

Three simple workflows, all generating HTML posters:

```
1. Dare-Share-Care:  Padlet → Claude → HTML
2. Delphi (Padlet):  Padlet → Claude → HTML  
3. Delphi (CSV):     CSV → Claude → HTML
```

## Quick Start

### Setup
```bash
export ANTHROPIC_API_KEY='your-key'
export PADLET_API_KEY='your-key'  # Only needed for options 1 & 2
```

### Run
```bash
mvn exec:java
```

## The Three Workflows

### Option 1: Dare-Share-Care (from Padlet)
- **Input**: Padlet board ID where students posted
- **Output**: HTML poster with DARE/SHARE/CARE analysis
- **Use case**: Current production workflow

### Option 2: Delphi (from Padlet)
- **Input**: Padlet board ID where students posted
- **Categories**: Keep Doing / Stop Doing / Start Doing
- **Output**: HTML poster with categorized analysis
- **Use case**: When students post Delphi evals to Padlet

### Option 3: Delphi (from CSV - no Padlet)
- **Input**: CSV file with pre-categorized statements
- **Categories**: Keep Doing / Stop Doing / Start Doing
- **Output**: HTML poster
- **Use case**: When you have evaluations in CSV format

## CSV Format for Option 3

File: `delphi-categorized.csv`

```csv
Category,Statement,Comment
Keep Doing,Tillade sjov og humor,
Stop Doing,Holde mange pauser,
Start Doing,Mere forklaring for hver linje kode,Optional comment here
```

## Project Files

### Add These to Your Project

Drop in `src/main/java/io/github/tessG/`:

1. ✅ **Main.java** (REPLACE existing)
2. ✅ **EvaluationConfigFactory.java** (REPLACE existing)  
3. ✅ **DelphiCsvParser.java** (NEW)
4. ✅ **DelphiDirectWorkflow.java** (NEW)

### Keep These Existing Files

- Category.java
- EvaluationConfig.java
- EvaluationInsights.java
- GenericEvaluationWorkflow.java
- GenericMiroBoardBuilder.java (not used, but keep it)

### One Small Edit Needed

In `GenericEvaluationWorkflow.java`, delete the `main` method at the bottom (around line 300-330). Everything else stays the same.

## Usage Examples

### Example 1: Dare-Share-Care from Padlet
```bash
mvn exec:java
# Choose: 1
# Enter Padlet ID: abc123xyz
# Get: poster-dare-share-care-2025-12-12.html
```

### Example 2: Delphi from Padlet (Future)
```bash
mvn exec:java
# Choose: 2
# Enter Padlet ID: xyz789abc
# Choose format: 1 (Keep/Stop/Start)
# Get: poster-delphi-2025-12-12.html
```

### Example 3: Delphi from CSV (Current Testing)
```bash
mvn exec:java
# Choose: 3
# Enter CSV path: delphiposts.csv
# Choose format: 1 (Keep/Stop/Start)
# Get: poster-delphi-2025-12-12.html
```

## Delphi Category Formats

### 3-Category (Default)
- ✅ **Keep Doing** - Green - Working well
- 🛑 **Stop Doing** - Red - Discontinue
- ⭐ **Start Doing** - Blue - New initiatives

### 4-Category (Optional)
- ✅ **Continue Doing** - Green
- 🔼 **Do More** - Blue
- 🛑 **Stop Doing** - Red
- ⭐ **Begin Doing** - Yellow

## HTML Poster Features

All workflows generate the same high-quality output:
- Urban/hip-hop aesthetic
- Wordcloud (20+ words)
- Speech bubbles with statements
- Category summaries
- Humorous highlight in "Parental Advisory" style
- Print-friendly A4 format

## Architecture

```
┌─────────────────────────────────────┐
│           User Input                 │
│  (Padlet ID or CSV file)            │
└────────────┬────────────────────────┘
             │
    ┌────────┴─────────┐
    │                  │
    v                  v
┌──────────┐    ┌─────────────────┐
│ Padlet   │    │ CSV Parser      │
│ Fetcher  │    │ (Option 3 only) │
└────┬─────┘    └────────┬────────┘
     │                   │
     └─────────┬─────────┘
               v
    ┌──────────────────────┐
    │   Claude API         │
    │   - Analyzes         │
    │   - Categorizes      │
    │   - Generates HTML   │
    └──────────┬───────────┘
               v
    ┌──────────────────────┐
    │   HTML Poster        │
    │   (saved to disk)    │
    └──────────────────────┘
```

## Setting Up Delphi Evaluations

### For Padlet-based (Option 2)
1. Create a Padlet board manually
2. Add 3 sections: "✅ Keep Doing", "🛑 Stop Doing", "⭐ Start Doing"
3. Give students the link
4. Students post their evaluations
5. Run option 2 with the board ID

### For CSV-based (Option 3)
1. Collect evaluations however you want
2. Organize into CSV format (see `delphi-categorized.csv`)
3. Run option 3 with the CSV file

## Cost Estimates

- ~$0.02-0.05 per analysis
- Budget ~$5 for a semester of evaluations

## Troubleshooting

**"Missing ANTHROPIC_API_KEY"**
```bash
export ANTHROPIC_API_KEY='your-key-here'
```

**"Failed to fetch from Padlet"**
- Verify PADLET_API_KEY is correct
- Check board ID is correct
- Ensure board is not private/restricted

**CSV Parsing Errors**
- Check file is UTF-8 encoded
- Verify CSV has header: `Category,Statement,Comment`
- Remove any BOM characters

**HTML Looks Broken**
- Open in different browser
- Check console output for Claude API errors
- Try regenerating (sometimes Claude varies output slightly)

## Production Workflow

**Current state:**
- Option 1 (DSC): Production ready ✅
- Option 2 (Delphi Padlet): Ready when students have Padlet ✅
- Option 3 (Delphi CSV): For testing or when CSV is easier ✅

**Typical semester flow:**
1. Create Padlet boards for each evaluation type
2. Students post throughout semester
3. You run analysis when needed
4. Share HTML posters with team/students

## Notes

- All three workflows use the same `GenericEvaluationWorkflow` engine
- Delphi can come from Padlet OR CSV - same quality output
- No Miro integration - pure HTML output
- Easy to extend with new evaluation types

## Next Steps

1. **Add the 4 files** to your project
2. **Delete old main** from GenericEvaluationWorkflow.java
3. **Test option 3** with delphi-categorized.csv
4. **Create Padlet boards** for production use
5. **Run evaluations** throughout semester
