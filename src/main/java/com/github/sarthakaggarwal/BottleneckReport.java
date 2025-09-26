package com.github.sarthakaggarwal;

import java.util.ArrayList;
import java.util.List;

/**
 * Report containing bottleneck analysis results.
 */
public class BottleneckReport {
    public String primaryBottleneck = "";
    public long primaryBottleneckTime = 0;
    public double primaryBottleneckPercentage = 0;
    public List<PhaseAnalysis> topPhases = new ArrayList<>();
    public List<String> recommendations = new ArrayList<>();

    public BottleneckReport() {
    }

    public BottleneckReport(String primaryBottleneck, long primaryBottleneckTime,
                          double primaryBottleneckPercentage, List<PhaseAnalysis> topPhases,
                          List<String> recommendations) {
        this.primaryBottleneck = primaryBottleneck;
        this.primaryBottleneckTime = primaryBottleneckTime;
        this.primaryBottleneckPercentage = primaryBottleneckPercentage;
        this.topPhases = topPhases;
        this.recommendations = recommendations;
    }

    public static class PhaseAnalysis {
        public final String phaseName;
        public final long duration;
        public final double percentage;

        public PhaseAnalysis(String phaseName, long duration, double percentage) {
            this.phaseName = phaseName;
            this.duration = duration;
            this.percentage = percentage;
        }
    }
}
