package com.github.sarthakaggarwal;

import java.util.List;

/**
 * Detects performance regressions by comparing current build with historical data.
 */
public class RegressionDetector {
    private List<BuildMetrics> buildHistory;

    public void loadHistory(List<BuildMetrics> buildHistory) {
        this.buildHistory = buildHistory;
    }

    public RegressionReport detectRegression(BuildMetrics currentBuild, double regressionThreshold) {
        if (buildHistory == null || buildHistory.size() < 2) {
            return new RegressionReport(false, false, 1.0, currentBuild.totalTime, 
                currentBuild.totalTime, "INSUFFICIENT_DATA", 1);
        }

        // Calculate average of previous builds (excluding current)
        double averageTime = buildHistory.stream()
            .filter(build -> !build.equals(currentBuild))
            .mapToLong(build -> build.totalTime)
            .average()
            .orElse(currentBuild.totalTime);

        long currentTime = currentBuild.totalTime;
        double regressionFactor = currentTime / averageTime;

        boolean isRegression = regressionFactor > regressionThreshold;
        boolean isImprovement = regressionFactor < (1.0 / regressionThreshold);

        String trend = calculateTrend();

        return new RegressionReport(isRegression, isImprovement, regressionFactor,
            currentTime, (long) averageTime, trend, buildHistory.size());
    }

    private String calculateTrend() {
        if (buildHistory.size() < 3) {
            return "INSUFFICIENT_DATA";
        }

        // Look at last 3 builds to determine trend
        int size = buildHistory.size();
        long recent1 = buildHistory.get(size - 1).totalTime;
        long recent2 = buildHistory.get(size - 2).totalTime;
        long recent3 = buildHistory.get(size - 3).totalTime;

        if (recent1 > recent2 && recent2 > recent3) {
            return "DEGRADING";
        } else if (recent1 < recent2 && recent2 < recent3) {
            return "IMPROVING";
        } else {
            return "STABLE";
        }
    }

}
