package com.github.sarthakaggarwal;

/**
 * Report containing regression analysis results.
 */
public class RegressionReport {
    public boolean isRegression = false;
    public boolean isImprovement = false;
    public double regressionFactor = 1.0;
    public long currentTime = 0;
    public long averageTime = 0;
    public String trend = "Stable";
    public int buildsAnalyzed = 0;

    public RegressionReport() {
    }

    public RegressionReport(boolean isRegression, boolean isImprovement, double regressionFactor,
                          long currentTime, long averageTime, String trend, int buildsAnalyzed) {
        this.isRegression = isRegression;
        this.isImprovement = isImprovement;
        this.regressionFactor = regressionFactor;
        this.currentTime = currentTime;
        this.averageTime = averageTime;
        this.trend = trend;
        this.buildsAnalyzed = buildsAnalyzed;
    }
}
