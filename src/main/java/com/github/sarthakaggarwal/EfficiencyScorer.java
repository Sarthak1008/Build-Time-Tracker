package com.github.sarthakaggarwal;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculates build efficiency scores based on various metrics.
 */
public class EfficiencyScorer {

    public EfficiencyScore calculateScore(BuildMetrics currentBuild, List<BuildMetrics> buildHistory) {
        double timeScore = calculateTimeScore(currentBuild, buildHistory);
        double memoryScore = calculateMemoryScore(currentBuild);
        double cpuScore = calculateCpuScore(currentBuild);
        double consistencyScore = calculateConsistencyScore(buildHistory);

        double totalScore = timeScore + memoryScore + cpuScore + consistencyScore;
        
        List<String> suggestions = generateSuggestions(timeScore, memoryScore, cpuScore, consistencyScore);

        return new EfficiencyScore(totalScore, timeScore, memoryScore, cpuScore, consistencyScore, suggestions);
    }

    private double calculateTimeScore(BuildMetrics currentBuild, List<BuildMetrics> buildHistory) {
        // Time efficiency: 30 points max
        // Based on build time relative to project complexity and history
        
        if (buildHistory.isEmpty()) {
            // For new projects, score based on absolute time
            long timeMs = currentBuild.totalTime;
            if (timeMs < 30000) return 30.0; // < 30s = excellent
            if (timeMs < 60000) return 25.0; // < 1m = good
            if (timeMs < 180000) return 20.0; // < 3m = fair
            if (timeMs < 300000) return 15.0; // < 5m = poor
            return 10.0; // > 5m = very poor
        }

        // Compare with historical average
        double avgTime = buildHistory.stream()
            .mapToLong(build -> build.totalTime)
            .average()
            .orElse(currentBuild.totalTime);

        double ratio = currentBuild.totalTime / avgTime;
        
        if (ratio <= 0.8) return 30.0; // 20% faster than average
        if (ratio <= 0.9) return 28.0; // 10% faster than average
        if (ratio <= 1.1) return 25.0; // Within 10% of average
        if (ratio <= 1.3) return 20.0; // 30% slower than average
        if (ratio <= 1.5) return 15.0; // 50% slower than average
        return 10.0; // More than 50% slower
    }

    private double calculateMemoryScore(BuildMetrics currentBuild) {
        // Memory efficiency: 25 points max
        long peakMemoryMB = currentBuild.systemMetrics.peakMemoryUsage / (1024 * 1024);
        
        if (peakMemoryMB < 256) return 25.0; // < 256MB = excellent
        if (peakMemoryMB < 512) return 22.0; // < 512MB = very good
        if (peakMemoryMB < 1024) return 20.0; // < 1GB = good
        if (peakMemoryMB < 2048) return 17.0; // < 2GB = fair
        if (peakMemoryMB < 4096) return 12.0; // < 4GB = poor
        return 8.0; // > 4GB = very poor
    }

    private double calculateCpuScore(BuildMetrics currentBuild) {
        // CPU efficiency: 25 points max
        double avgCpuPercent = currentBuild.systemMetrics.avgCpuUsage * 100;
        
        if (avgCpuPercent > 80) return 25.0; // High utilization = good
        if (avgCpuPercent > 60) return 22.0; // Good utilization
        if (avgCpuPercent > 40) return 18.0; // Moderate utilization
        if (avgCpuPercent > 20) return 15.0; // Low utilization
        return 10.0; // Very low utilization (might indicate I/O bottlenecks)
    }

    private double calculateConsistencyScore(List<BuildMetrics> buildHistory) {
        // Consistency: 20 points max
        if (buildHistory.size() < 3) {
            return 15.0; // Default score for insufficient data
        }

        // Calculate coefficient of variation for build times
        double[] times = buildHistory.stream()
            .mapToDouble(build -> build.totalTime)
            .toArray();

        double mean = 0;
        for (double time : times) {
            mean += time;
        }
        mean /= times.length;

        double variance = 0;
        for (double time : times) {
            variance += Math.pow(time - mean, 2);
        }
        variance /= times.length;

        double stdDev = Math.sqrt(variance);
        double coefficientOfVariation = stdDev / mean;

        // Lower variation = higher score
        if (coefficientOfVariation < 0.1) return 20.0; // Very consistent
        if (coefficientOfVariation < 0.2) return 18.0; // Consistent
        if (coefficientOfVariation < 0.3) return 15.0; // Moderately consistent
        if (coefficientOfVariation < 0.5) return 12.0; // Inconsistent
        return 8.0; // Very inconsistent
    }

    private List<String> generateSuggestions(double timeScore, double memoryScore, 
                                           double cpuScore, double consistencyScore) {
        List<String> suggestions = new ArrayList<>();

        if (timeScore < 20) {
            suggestions.add("Optimize build time by enabling parallel execution or incremental builds");
        }
        if (memoryScore < 15) {
            suggestions.add("Reduce memory usage by tuning JVM heap settings or optimizing dependencies");
        }
        if (cpuScore < 15) {
            suggestions.add("Improve CPU utilization by enabling parallel builds or reducing I/O bottlenecks");
        }
        if (consistencyScore < 12) {
            suggestions.add("Improve build consistency by stabilizing test environments and dependencies");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("Build efficiency is good - maintain current practices");
        }

        return suggestions;
    }

}
