package io.github.tessG;

import java.util.List;

/**
 * Data structure to hold the analyzed student insights
 */
public class StudentInsights {
    private String mainHeadline;
    private List<String> dareItems;
    private List<String> shareItems;
    private List<String> careItems;
    private String summary;
    private String keyInsight;

    public StudentInsights(String mainHeadline, 
                          List<String> dareItems, 
                          List<String> shareItems, 
                          List<String> careItems,
                          String summary,
                          String keyInsight) {
        this.mainHeadline = mainHeadline;
        this.dareItems = dareItems;
        this.shareItems = shareItems;
        this.careItems = careItems;
        this.summary = summary;
        this.keyInsight = keyInsight;
    }

    public String getMainHeadline() {
        return mainHeadline;
    }

    public List<String> getDareItems() {
        return dareItems;
    }

    public List<String> getShareItems() {
        return shareItems;
    }

    public List<String> getCareItems() {
        return careItems;
    }

    public String getSummary() {
        return summary;
    }

    public String getKeyInsight() {
        return keyInsight;
    }
}
