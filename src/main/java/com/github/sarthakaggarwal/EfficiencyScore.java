package com.github.sarthakaggarwal;

import java.util.ArrayList;
import java.util.List;

/**
 * Build efficiency score with detailed breakdown.
 */
public class EfficiencyScore {
    public double score = 0;
    public double timeScore = 0;
    public double memoryScore = 0;
    public double cpuScore = 0;
    public double consistencyScore = 0;
    public List<String> suggestions = new ArrayList<>();

    public EfficiencyScore() {
    }

    public EfficiencyScore(double score, double timeScore, double memoryScore, 
                         double cpuScore, double consistencyScore, List<String> suggestions) {
        this.score = score;
        this.timeScore = timeScore;
        this.memoryScore = memoryScore;
        this.cpuScore = cpuScore;
        this.consistencyScore = consistencyScore;
        this.suggestions = suggestions;
    }

    public String getLetterGrade() {
        if (score >= 90) return "A+";
        if (score >= 85) return "A";
        if (score >= 80) return "A-";
        if (score >= 75) return "B+";
        if (score >= 70) return "B";
        if (score >= 65) return "B-";
        if (score >= 60) return "C+";
        if (score >= 55) return "C";
        if (score >= 50) return "C-";
        if (score >= 45) return "D+";
        if (score >= 40) return "D";
        return "F";
    }

    public String getScoreDescription() {
        if (score >= 85) return "Excellent - Highly optimized build";
        if (score >= 70) return "Good - Well-performing build";
        if (score >= 55) return "Average - Room for improvement";
        if (score >= 40) return "Below Average - Needs optimization";
        return "Poor - Significant optimization required";
    }
}
