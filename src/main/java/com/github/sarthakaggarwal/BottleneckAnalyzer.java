package com.github.sarthakaggarwal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Analyzes build phases to identify bottlenecks and performance issues.
 */
public class BottleneckAnalyzer {

    public BottleneckReport analyze(Map<String, Long> phaseTimes) {
        if (phaseTimes == null || phaseTimes.isEmpty()) {
            return new BottleneckReport("", 0, 0.0, new ArrayList<>(), new ArrayList<>());
        }

        // Calculate total time (excluding 'total' entry)
        long totalTime = phaseTimes.entrySet().stream()
            .filter(e -> !e.getKey().equals("total"))
            .mapToLong(Map.Entry::getValue)
            .sum();

        if (totalTime == 0) {
            return new BottleneckReport("", 0, 0.0, new ArrayList<>(), new ArrayList<>());
        }

        // Find primary bottleneck (slowest phase)
        Map.Entry<String, Long> slowestPhase = phaseTimes.entrySet().stream()
            .filter(e -> !e.getKey().equals("total"))
            .max(Map.Entry.comparingByValue())
            .orElse(null);

        String primaryBottleneck = "";
        long primaryBottleneckTime = 0;
        double primaryBottleneckPercentage = 0.0;

        if (slowestPhase != null) {
            primaryBottleneck = slowestPhase.getKey();
            primaryBottleneckTime = slowestPhase.getValue();
            primaryBottleneckPercentage = (primaryBottleneckTime * 100.0) / totalTime;
        }

        // Create sorted list of phases by duration
        List<BottleneckReport.PhaseAnalysis> topPhases = phaseTimes.entrySet().stream()
            .filter(e -> !e.getKey().equals("total"))
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .map(e -> new BottleneckReport.PhaseAnalysis(
                e.getKey(), 
                e.getValue(), 
                (e.getValue() * 100.0) / totalTime))
            .collect(Collectors.toList());

        // Generate recommendations
        List<String> recommendations = generateRecommendations(topPhases, totalTime);

        return new BottleneckReport(primaryBottleneck, primaryBottleneckTime, 
            primaryBottleneckPercentage, topPhases, recommendations);
    }

    private List<String> generateRecommendations(List<BottleneckReport.PhaseAnalysis> topPhases, long totalTime) {
        List<String> recommendations = new ArrayList<>();

        if (topPhases.isEmpty()) {
            return recommendations;
        }

        BottleneckReport.PhaseAnalysis slowest = topPhases.get(0);

        // Recommendations based on phase type and duration
        if (slowest.phaseName.contains("compile")) {
            if (slowest.duration > 30000) { // > 30 seconds
                recommendations.add("Consider enabling incremental compilation");
                recommendations.add("Use Maven Compiler Plugin 3.8+ with improved performance");
                recommendations.add("Consider splitting large modules into smaller ones");
            }
            if (slowest.percentage > 50) {
                recommendations.add("Compilation is the main bottleneck - consider parallel compilation");
            }
        }

        if (slowest.phaseName.contains("test")) {
            if (slowest.duration > 60000) { // > 1 minute
                recommendations.add("Consider running tests in parallel with surefire.parallel");
                recommendations.add("Profile slow tests and optimize them");
                recommendations.add("Use test categories to run only essential tests in CI");
            }
        }

        if (slowest.phaseName.contains("package") || slowest.phaseName.contains("install")) {
            if (slowest.duration > 10000) { // > 10 seconds
                recommendations.add("Consider using Maven Daemon to reduce startup overhead");
                recommendations.add("Optimize artifact packaging and dependency resolution");
            }
        }

        // General recommendations
        if (totalTime > 300000) { // > 5 minutes
            recommendations.add("Consider using Maven build cache plugins");
            recommendations.add("Profile with Maven profiler: mvn -Dprofile");
        }

        // If no specific recommendations, add general ones
        if (recommendations.isEmpty()) {
            recommendations.add("Monitor build trends to identify performance regressions");
            recommendations.add("Consider using parallel builds: mvn -T 1C");
        }

        return recommendations;
    }

}
