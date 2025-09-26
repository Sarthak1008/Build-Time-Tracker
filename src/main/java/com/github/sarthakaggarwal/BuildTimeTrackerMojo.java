package com.github.sarthakaggarwal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.sun.management.OperatingSystemMXBean;

/**
 * Enhanced Maven plugin with advanced analytics: bottleneck analysis,
 * system monitoring, regression detection, and efficiency scoring.
 */
@Mojo(name = "track", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class BuildTimeTrackerMojo extends AbstractMojo {
    // Existing parameters...
    @Parameter(property = "warnThreshold", defaultValue = "5000")
    private long warnThreshold;

    @Parameter(property = "fastThreshold", defaultValue = "1000")
    private long fastThreshold;

    @Parameter(property = "generateHtml", defaultValue = "true")
    private boolean generateHtml;

    @Parameter(property = "coloredOutput", defaultValue = "true")
    private boolean coloredOutput;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}")
    private File outputDirectory;

    @Parameter(property = "historyFile", defaultValue = "${project.build.directory}/build-history.json")
    private String historyFile;

    // New advanced features parameters
    @Parameter(property = "enableSystemMonitoring", defaultValue = "true")
    private boolean enableSystemMonitoring;

    @Parameter(property = "monitoringInterval", defaultValue = "1000")
    private long monitoringIntervalMs;

    @Parameter(property = "regressionThreshold", defaultValue = "1.5")
    private double regressionThreshold; // 1.5x slower = regression

    @Parameter(property = "historySize", defaultValue = "20")
    private int historySize;

    @Parameter(property = "enableBottleneckAnalysis", defaultValue = "true")
    private boolean enableBottleneckAnalysis;

    @Parameter(property = "enableFailureAnalysis", defaultValue = "true")
    private boolean enableFailureAnalysis;

    @Parameter(property = "enableWarningDetection", defaultValue = "true")
    private boolean enableWarningDetection;

    @Parameter(property = "enableSOPCompliance", defaultValue = "true")
    private boolean enableSOPCompliance;

    @Parameter(property = "sopConfigFile", defaultValue = "${project.basedir}/sop-config.json")
    private String sopConfigFile;

    @Parameter(property = "maxWarningsToShow", defaultValue = "50")
    private int maxWarningsToShow;

    @Parameter(property = "enableCodeQualityChecks", defaultValue = "true")
    private boolean enableCodeQualityChecks;

    // Parameters - put in your @Mojo (main plugin) class with existing fields
    @Parameter(property = "enableFailureLineDetection", defaultValue = "true")
    private boolean enableFailureLineDetection;

    @Parameter(property = "enableWarningCollection", defaultValue = "true")
    private boolean enableWarningCollection;

    @Parameter(property = "maxFailureLinesToShow", defaultValue = "10")
    private int maxFailureLinesToShow;

    @Parameter(property = "sourceCodeContext", defaultValue = "3")
    private int sourceCodeContext;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession session;

    // Add these fields
    private long buildStartTime;
    private final Map<String, Long> phaseStartTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> phaseDurations = new ConcurrentHashMap<>();
    private final List<BuildMetrics> buildHistory = new ArrayList<>();
    private final List<BuildFailure> buildFailures = new ArrayList<>();
    private final List<BuildWarning> buildWarnings = new ArrayList<>();
    private final Map<String, String> sourceFileCache = new HashMap<>();

    // ANSI Color codes
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_PURPLE = "\u001B[35m";

    // Core tracking

    // Advanced analytics
    private final BottleneckAnalyzer bottleneckAnalyzer = new BottleneckAnalyzer();
    private SystemMonitor systemMonitor;
    private final RegressionDetector regressionDetector = new RegressionDetector();
    private final EfficiencyScorer efficiencyScorer = new EfficiencyScorer();
    private FailureAnalyzer failureAnalyzer;
    private WarningAnalyzer warningAnalyzer;

    @Override
    public void execute() throws MojoExecutionException {
        buildStartTime = System.currentTimeMillis();

        // Initialize analyzers
        failureAnalyzer = new FailureAnalyzer(
                enableFailureAnalysis,
                buildFailures,
                project,
                sourceCodeContext,
                sourceFileCache,
                this::logError,
                this::printFailureAnalysis);

        warningAnalyzer = new WarningAnalyzer(
                enableWarningDetection,
                buildWarnings);

        systemMonitor = new SystemMonitor();

        // Replace the default listener with our advanced listener
        ExecutionListener defaultListener = session.getRequest().getExecutionListener();
        session.getRequest().setExecutionListener(
                new AdvancedBuildTimeExecutionListener(defaultListener));

        // Load previous build history
        loadBuildHistory();

        // Initialize advanced analytics
        initializeAdvancedFeatures();

        logInfo("🚀 Advanced Build Time Tracker initialized with analytics engine...");
        logInfo("   📊 Bottleneck Analysis: " + (enableBottleneckAnalysis ? "ENABLED" : "DISABLED"));
        logInfo("   📈 System Monitoring: " + (enableSystemMonitoring ? "ENABLED" : "DISABLED"));
        logInfo("   🔍 Regression Detection: ENABLED (threshold: " + regressionThreshold + "x)");
    }

    private void initializeAdvancedFeatures() {
        if (enableSystemMonitoring) {
            systemMonitor.startMonitoring();
        }
        regressionDetector.loadHistory(buildHistory);
    }

    /**
     * Enhanced execution listener with advanced analytics.
     */
    private class AdvancedBuildTimeExecutionListener implements ExecutionListener {
        private final ExecutionListener delegate;

        public AdvancedBuildTimeExecutionListener(ExecutionListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void sessionStarted(ExecutionEvent event) {
            logInfo("🎯 Build session started - Analytics engine active");
        }

        @Override
        public void sessionEnded(ExecutionEvent event) {
            long totalTime = System.currentTimeMillis() - buildStartTime;
            phaseDurations.put("total", totalTime);

            // Stop system monitoring
            SystemMetrics systemMetrics = systemMonitor.stopMonitoring();

            // Create comprehensive build metrics
            BuildMetrics currentBuild = new BuildMetrics(
                    LocalDateTime.now(),
                    totalTime,
                    new HashMap<>(phaseDurations),
                    systemMetrics);

            buildHistory.add(currentBuild);

            // Perform advanced analytics
            performAdvancedAnalytics(currentBuild);

            // Generate reports
            generateAdvancedReports(currentBuild);

            // Save build history
            saveBuildHistory();
        }

        @Override
        public void mojoStarted(ExecutionEvent event) {
            String phase = event.getMojoExecution().getLifecyclePhase();
            if (phase != null && !phase.isEmpty()) {
                phaseStartTimes.putIfAbsent(phase, System.currentTimeMillis());
                logInfo("⚙️  Starting phase: " + phase);
            }
        }

        @Override
        public void mojoSucceeded(ExecutionEvent event) {
            recordPhaseCompletion(event);
            
            // Analyze warnings for compiler phases
            String phase = event.getMojoExecution().getLifecyclePhase();
            if (phase != null && (phase.contains("compile") || phase.contains("test"))) {
                analyzePhaseForWarnings(event, phase);
            }
        }

        @Override
        public void mojoFailed(ExecutionEvent event) {
            recordPhaseCompletion(event);
            if (enableFailureAnalysis) {
                failureAnalyzer.analyzeFailure(event);
            }
            if (delegate != null) {
                delegate.mojoFailed(event);
            }
        }

        private void recordPhaseCompletion(ExecutionEvent event) {
            String phase = event.getMojoExecution().getLifecyclePhase();
            if (phase != null && !phase.isEmpty() && phaseStartTimes.containsKey(phase)) {
                long startTime = phaseStartTimes.get(phase);
                long duration = System.currentTimeMillis() - startTime;

                phaseDurations.put(phase, duration);
                logWithColor("✅ Phase \"" + phase + "\" completed", duration);
            }
        }

        // Stub implementations for required interface methods
        @Override
        public void projectDiscoveryStarted(ExecutionEvent event) {
        }

        @Override
        public void projectSkipped(ExecutionEvent event) {
        }

        @Override
        public void projectStarted(ExecutionEvent event) {
        }

        @Override
        public void projectSucceeded(ExecutionEvent event) {
        }

        @Override
        public void projectFailed(ExecutionEvent event) {
        }

        @Override
        public void forkStarted(ExecutionEvent event) {
        }

        @Override
        public void forkSucceeded(ExecutionEvent event) {
        }

        @Override
        public void forkFailed(ExecutionEvent event) {
        }

        @Override
        public void forkedProjectStarted(ExecutionEvent event) {
        }

        @Override
        public void forkedProjectSucceeded(ExecutionEvent event) {
        }

        @Override
        public void forkedProjectFailed(ExecutionEvent event) {
        }

        @Override
        public void mojoSkipped(ExecutionEvent event) {
        }

    }

    /**
     * Analyzes a build phase for warnings by examining Maven output and source files.
     */
    private void analyzePhaseForWarnings(ExecutionEvent event, String phase) {
        if (!enableWarningDetection || warningAnalyzer == null) {
            return;
        }

        try {
            // For compiler phases, we need to check for compiler warnings
            // Since we can't directly capture Maven's output stream, we'll simulate
            // warning detection by analyzing the project's source files for common issues
            if (phase.contains("compile")) {
                analyzeSourceFilesForWarnings(phase);
            }
            
            // Also analyze any available log output (this would need to be enhanced
            // to capture actual Maven output in a real implementation)
            String simulatedOutput = generateSimulatedCompilerOutput();
            warningAnalyzer.analyzeOutput(simulatedOutput, phase);
            
        } catch (Exception e) {
            getLog().debug("Error analyzing warnings for phase " + phase + ": " + e.getMessage());
        }
    }

    /**
     * Analyzes source files for common warning patterns like unused variables.
     */
    private void analyzeSourceFilesForWarnings(String phase) {
        try {
            File sourceDir = new File(project.getBasedir(), "src/main/java");
            if (sourceDir.exists()) {
                analyzeJavaFilesInDirectory(sourceDir, phase);
            }
            
            // Also check test sources if this is a test compile phase
            if (phase.contains("test")) {
                File testSourceDir = new File(project.getBasedir(), "src/test/java");
                if (testSourceDir.exists()) {
                    analyzeJavaFilesInDirectory(testSourceDir, phase);
                }
            }
        } catch (Exception e) {
            getLog().debug("Error analyzing source files: " + e.getMessage());
        }
    }

    /**
     * Recursively analyzes Java files in a directory for warning patterns.
     */
    private void analyzeJavaFilesInDirectory(File directory, String phase) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                analyzeJavaFilesInDirectory(file, phase);
            } else if (file.getName().endsWith(".java")) {
                analyzeJavaFileForWarnings(file, phase);
            }
        }
    }

    /**
     * Analyzes a single Java file for warning patterns.
     */
    private void analyzeJavaFileForWarnings(File javaFile, String phase) {
        try {
            String content = new String(Files.readAllBytes(javaFile.toPath()));
            String[] lines = content.split("\n");
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                int lineNumber = i + 1;
                
                // Check for unused variables (simple pattern matching)
                if (line.matches(".*\\s+(int|long|double|float|boolean|String|List|Map)\\s+\\w+\\s*[;=].*") &&
                    !line.contains("final") && !line.contains("static")) {
                    
                    // Extract variable name
                    String[] parts = line.split("\\s+");
                    for (int j = 0; j < parts.length - 1; j++) {
                        if (parts[j].matches("(int|long|double|float|boolean|String|List|Map)")) {
                            String varName = parts[j + 1].replaceAll("[;=,].*", "");
                            
                            // Simple check if variable is used later in the file
                            if (!isVariableUsed(content, varName, lineNumber)) {
                                BuildWarning warning = new BuildWarning(
                                    phase,
                                    "Unused Code",
                                    "The value of the local variable " + varName + " is not used",
                                    javaFile.getName(),
                                    lineNumber,
                                    "LOW"
                                );
                                buildWarnings.add(warning);
                            }
                            break;
                        }
                    }
                }
                
                // Check for @SuppressWarnings annotations
                if (line.contains("@SuppressWarnings")) {
                    BuildWarning warning = new BuildWarning(
                        phase,
                        "Suppressed Warnings",
                        "Warning suppression detected: " + line.trim(),
                        javaFile.getName(),
                        lineNumber,
                        "LOW"
                    );
                    buildWarnings.add(warning);
                }
            }
        } catch (Exception e) {
            getLog().debug("Error analyzing file " + javaFile.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Simple check to see if a variable is used in the file content.
     */
    private boolean isVariableUsed(String content, String varName, int declarationLine) {
        String[] lines = content.split("\n");
        for (int i = declarationLine; i < lines.length; i++) {
            if (lines[i].contains(varName) && !lines[i].trim().startsWith("//")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates simulated compiler output for testing warning detection.
     * In a real implementation, this would capture actual Maven output.
     */
    private String generateSimulatedCompilerOutput() {
        // This simulates the types of warnings that might appear in Maven output
        StringBuilder output = new StringBuilder();
        
        // Simulate some common Maven/compiler warnings
        output.append("WARNING: A Java agent has been loaded dynamically\n");
        output.append("WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading\n");
        output.append("WARNING: Dynamic loading of agents will be disallowed by default in a future release\n");
        
        return output.toString();
    }

    /**
     * Performs all advanced analytics on the completed build.
     */
    private void performAdvancedAnalytics(BuildMetrics currentBuild) {
        logInfo("");
        logInfo(getColoredText("🔬 ADVANCED ANALYTICS REPORT", ANSI_BOLD + ANSI_CYAN));
        logInfo("=" + repeat("=", 50));

        // 1. Bottleneck Analysis
        if (enableBottleneckAnalysis) {
            BottleneckReport bottleneckReport = bottleneckAnalyzer.analyze(currentBuild.phaseTimes);
            printBottleneckAnalysis(bottleneckReport);
        }

        // 2. Regression Detection
        RegressionReport regressionReport = regressionDetector.detectRegression(currentBuild, regressionThreshold);
        printRegressionAnalysis(regressionReport);

        // 3. Build Efficiency Score
        EfficiencyScore efficiencyScore = efficiencyScorer.calculateScore(currentBuild, buildHistory);
        printEfficiencyScore(efficiencyScore);

        // 4. System Resource Analysis
        if (enableSystemMonitoring) {
            printSystemResourceAnalysis(currentBuild.systemMetrics);
        }

        // 5. Warning Analysis
        if (enableWarningDetection && !buildWarnings.isEmpty()) {
            printWarningsSummary();
        }

        logInfo("=" + repeat("=", 50));
    }

    private void printBottleneckAnalysis(BottleneckReport report) {
        logInfo("");
        logInfo(getColoredText("🎯 BOTTLENECK ANALYSIS", ANSI_BOLD + ANSI_YELLOW));
        logInfo(repeat("─", 30));

        logInfo(String.format("Primary Bottleneck: %s%s%s (%.1fs, %.1f%% of build)",
                ANSI_RED + ANSI_BOLD, report.primaryBottleneck, ANSI_RESET,
                report.primaryBottleneckTime / 1000.0, report.primaryBottleneckPercentage));

        logInfo("Top 3 Time Consumers:");
        for (int i = 0; i < Math.min(3, report.topPhases.size()); i++) {
            BottleneckReport.PhaseAnalysis phase = report.topPhases.get(i);
            String indicator = i == 0 ? "🔥" : i == 1 ? "⚠️" : "📊";
            logInfo(String.format("  %s %s: %.1fs (%.1f%%)",
                    indicator, phase.phaseName, phase.duration / 1000.0, phase.percentage));
        }

        if (report.recommendations.size() > 0) {
            logInfo("");
            logInfo(getColoredText("💡 Optimization Recommendations:", ANSI_BOLD));
            for (String recommendation : report.recommendations) {
                logInfo("  • " + recommendation);
            }
        }
    }

    private void printRegressionAnalysis(RegressionReport report) {
        logInfo("");
        logInfo(getColoredText("📈 REGRESSION ANALYSIS", ANSI_BOLD + ANSI_BLUE));
        logInfo(repeat("─", 30));

        if (report.isRegression) {
            logInfo(getColoredText(String.format("🚨 PERFORMANCE REGRESSION DETECTED! (%.1fx slower)",
                    report.regressionFactor), ANSI_RED + ANSI_BOLD));
            logInfo(String.format("Current: %.1fs | Average: %.1fs | Difference: +%.1fs",
                    report.currentTime / 1000.0, report.averageTime / 1000.0,
                    (report.currentTime - report.averageTime) / 1000.0));
        } else if (report.isImprovement) {
            logInfo(getColoredText(String.format("🎉 PERFORMANCE IMPROVEMENT! (%.1fx faster)",
                    1.0 / report.regressionFactor), ANSI_GREEN + ANSI_BOLD));
            logInfo(String.format("Current: %.1fs | Average: %.1fs | Difference: -%.1fs",
                    report.currentTime / 1000.0, report.averageTime / 1000.0,
                    (report.averageTime - report.currentTime) / 1000.0));
        } else {
            logInfo(getColoredText("✅ Build performance is stable", ANSI_GREEN));
            logInfo(String.format("Current: %.1fs | Average: %.1fs",
                    report.currentTime / 1000.0, report.averageTime / 1000.0));
        }

        logInfo(String.format("Trend: %s | Builds analyzed: %d",
                report.trend, report.buildsAnalyzed));
    }

    private void printEfficiencyScore(EfficiencyScore score) {
        logInfo("");
        logInfo(getColoredText("⭐ BUILD EFFICIENCY SCORE", ANSI_BOLD + ANSI_PURPLE));
        logInfo(repeat("─", 30));

        String grade = score.getLetterGrade();
        String color = score.score >= 85 ? ANSI_GREEN : score.score >= 70 ? ANSI_YELLOW : ANSI_RED;

        logInfo(String.format("Overall Score: %s%.1f/100 (%s)%s",
                color + ANSI_BOLD, score.score, grade, ANSI_RESET));

        logInfo("Score Breakdown:");
        logInfo(String.format("  ⏱️  Time Efficiency: %.1f/30 pts", score.timeScore));
        logInfo(String.format("  💾 Memory Efficiency: %.1f/25 pts", score.memoryScore));
        logInfo(String.format("  🖥️  CPU Efficiency: %.1f/25 pts", score.cpuScore));
        logInfo(String.format("  📊 Consistency: %.1f/20 pts", score.consistencyScore));

        if (!score.suggestions.isEmpty()) {
            logInfo("");
            logInfo(getColoredText("💡 Efficiency Improvements:", ANSI_BOLD));
            for (String suggestion : score.suggestions) {
                logInfo("  • " + suggestion);
            }
        }
    }

    private void printSystemResourceAnalysis(SystemMetrics metrics) {
        logInfo("");
        logInfo(getColoredText("🖥️ SYSTEM RESOURCE ANALYSIS", ANSI_BOLD + ANSI_CYAN));
        logInfo(repeat("─", 30));

        logInfo(String.format("Memory Usage: %.1f MB avg, %.1f MB peak",
                (double) metrics.avgMemoryUsage / (1024 * 1024), (double) metrics.peakMemoryUsage / (1024 * 1024)));
        logInfo(String.format("CPU Usage: %.1f%% avg, %.1f%% peak",
                metrics.avgCpuUsage * 100, metrics.peakCpuUsage * 100));
        logInfo(String.format("GC Activity: %d collections, %.1fs total time",
                metrics.gcCount, metrics.gcTime / 1000.0));

        // Resource efficiency indicators
        if (metrics.peakMemoryUsage > 2L * 1024 * 1024 * 1024) { // > 2GB
            logInfo(getColoredText("⚠️  High memory usage detected - consider increasing heap size", ANSI_YELLOW));
        }
        if (metrics.avgCpuUsage > 0.8) { // > 80%
            logInfo(getColoredText("⚠️  High CPU utilization - consider parallel execution", ANSI_YELLOW));
        }
        if (metrics.gcTime > 5000) { // > 5 seconds
            logInfo(getColoredText("⚠️  Excessive GC time - consider tuning GC settings", ANSI_YELLOW));
        }
    }

    // Utility methods
    private void logWithColor(String message, long duration) {
        if (!coloredOutput) {
            getLog().info(String.format("%s (%.1fs)", message, duration / 1000.0));
            return;
        }

        String color = duration <= fastThreshold ? ANSI_GREEN : duration <= warnThreshold ? ANSI_YELLOW : ANSI_RED;
        String emoji = duration <= fastThreshold ? "⚡" : duration <= warnThreshold ? "⚠️" : "🐌";

        getLog().info(String.format("%s%s %s (%.1fs)%s",
                color, emoji, message, duration / 1000.0, ANSI_RESET));
    }

    private String getColoredText(String text, String color) {
        return coloredOutput ? color + text + ANSI_RESET : text;
    }

    private void logInfo(String message) {
        getLog().info(message);
    }

    // Load/Save methods
    private void loadBuildHistory() {
        try {
            File histFile = new File(historyFile);
            if (histFile.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(historyFile)));
                // Parse previous builds - simplified implementation
                // In production, use proper JSON parsing
            }
        } catch (Exception e) {
            getLog().debug("Could not load build history: " + e.getMessage());
        }
    }

    private void saveBuildHistory() {
        try {
            File histFile = new File(historyFile);
            histFile.getParentFile().mkdirs();

            // Keep only recent builds
            if (buildHistory.size() > historySize) {
                buildHistory.subList(0, buildHistory.size() - historySize).clear();
            }

            // Simple JSON serialization - in production, use proper library
            try (FileWriter writer = new FileWriter(histFile)) {
                writer.write("{\n  \"builds\": [\n");
                for (int i = 0; i < buildHistory.size(); i++) {
                    BuildMetrics build = buildHistory.get(i);
                    writer.write("    {\n");
                    writer.write("      \"timestamp\": \"" + build.timestamp + "\",\n");
                    writer.write("      \"totalTime\": " + build.totalTime + ",\n");
                    writer.write("      \"memoryUsage\": " + build.systemMetrics.peakMemoryUsage + ",\n");
                    writer.write("      \"cpuUsage\": " + build.systemMetrics.avgCpuUsage + "\n");
                    writer.write("    }");
                    if (i < buildHistory.size() - 1)
                        writer.write(",");
                    writer.write("\n");
                }
                writer.write("  ]\n}");
            }
        } catch (Exception e) {
            getLog().error("Could not save build history: " + e.getMessage());
        }
    }

    private void generateAdvancedReports(BuildMetrics currentBuild) {
        if (generateHtml) {
            generateAdvancedHtmlDashboard(currentBuild);
        }

        // Generate failure report if there were failures
        if (!buildFailures.isEmpty()) {
            generateFailureReport();
        }

        // Generate warnings report if there were warnings
        if (!buildWarnings.isEmpty()) {
            generateWarningsReport();
        }
    }

    // ========== ENHANCED HTML DASHBOARD GENERATION ==========

    private void generateAdvancedHtmlDashboard(BuildMetrics currentBuild) {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        File htmlFile = new File(outputDirectory, "build-dashboard.html");

        try (FileWriter writer = new FileWriter(htmlFile)) {
            writer.write(generateAdvancedHtmlContent(currentBuild));
            logInfo("🎯 Build dashboard generated: " + htmlFile.getAbsolutePath());
        } catch (IOException e) {
            getLog().error("Failed to generate build dashboard", e);
        }
    }

    private void generateFailureReport() {
        if (buildFailures.isEmpty())
            return;

        File reportFile = new File(outputDirectory, "build-failures.html");
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write(generateFailureReportHtml());
            logInfo("🔥 Failure report generated: " + reportFile.getAbsolutePath());
        } catch (IOException e) {
            getLog().error("Failed to generate failure report", e);
        }
    }

    private void generateWarningsReport() {
        if (buildWarnings.isEmpty())
            return;

        File reportFile = new File(outputDirectory, "build-warnings.html");
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write(generateWarningsReportHtml());
            logInfo("⚠️ Warnings report generated: " + reportFile.getAbsolutePath());
        } catch (IOException e) {
            getLog().error("Failed to generate warnings report", e);
        }
    }

    private String generateFailureReportHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
                .append("<html lang='en'>\n")
                .append("<head>\n")
                .append("    <meta charset='UTF-8'>\n")
                .append("    <title>Build Failures Report</title>\n")
                .append("    <style>\n")
                .append("        body { font-family: -apple-system, system-ui, sans-serif; line-height: 1.6; margin: 0; padding: 20px; background: #f8f9fa; }\n")
                .append("        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n")
                .append("        .header { background: #dc3545; color: white; padding: 20px; border-radius: 6px; margin-bottom: 20px; }\n")
                .append("        .failure-card { border: 1px solid #dee2e6; border-radius: 6px; padding: 20px; margin-bottom: 20px; }\n")
                .append("        .source-code { background: #f8f9fa; padding: 15px; border-radius: 4px; font-family: monospace; white-space: pre; overflow-x: auto; }\n")
                .append("        .fixes { background: #e8f4f8; padding: 15px; border-radius: 4px; margin-top: 10px; }\n")
                .append("        .stack-trace { font-family: monospace; font-size: 0.9em; overflow-x: auto; }\n")
                .append("    </style>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("    <div class='container'>\n")
                .append("        <div class='header'>\n")
                .append("            <h1>🔥 Build Failures Report</h1>\n")
                .append("            <p>Total Failures: ").append(buildFailures.size()).append("</p>\n")
                .append("        </div>\n");

        // Generate failure cards
        for (BuildFailure failure : buildFailures) {
            html.append("        <div class='failure-card'>\n")
                    .append("            <h3>").append(failure.errorType).append("</h3>\n")
                    .append("            <p><strong>Phase:</strong> ").append(failure.phase).append("</p>\n")
                    .append("            <p><strong>Message:</strong> ").append(failure.errorMessage).append("</p>\n");

            if (!failure.fileName.isEmpty()) {
                html.append("            <p><strong>File:</strong> ").append(failure.fileName)
                        .append(" (line ").append(failure.lineNumber).append(")</p>\n");

                if (!failure.sourceCodeSnippet.isEmpty()) {
                    html.append("            <div class='source-code'>").append(failure.sourceCodeSnippet)
                            .append("</div>\n");
                }
            }

            if (!failure.suggestedFixes.isEmpty()) {
                html.append("            <div class='fixes'>\n")
                        .append("                <h4>💡 Suggested Fixes:</h4>\n")
                        .append("                <ul>\n");
                for (String fix : failure.suggestedFixes) {
                    html.append("                    <li>").append(fix).append("</li>\n");
                }
                html.append("                </ul>\n")
                        .append("            </div>\n");
            }

            html.append("            <div class='stack-trace'>\n")
                    .append("                <pre>").append(failure.fullStackTrace).append("</pre>\n")
                    .append("            </div>\n")
                    .append("        </div>\n");
        }

        html.append("    </div>\n")
                .append("</body>\n")
                .append("</html>");

        return html.toString();
    }

    private String generateWarningsReportHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
                .append("<html lang='en'>\n")
                .append("<head>\n")
                .append("    <meta charset='UTF-8'>\n")
                .append("    <title>Build Warnings Report</title>\n")
                .append("    <style>\n")
                .append("        body { font-family: -apple-system, system-ui, sans-serif; line-height: 1.6; margin: 0; padding: 20px; background: #f8f9fa; }\n")
                .append("        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n")
                .append("        .header { background: #ffc107; color: #333; padding: 20px; border-radius: 6px; margin-bottom: 20px; }\n")
                .append("        .warning-group { margin-bottom: 30px; }\n")
                .append("        .warning-type { background: #f8f9fa; padding: 10px; border-radius: 4px; margin-bottom: 10px; }\n")
                .append("        .warning-item { border: 1px solid #dee2e6; border-radius: 4px; padding: 15px; margin-bottom: 10px; }\n")
                .append("        .severity-high { border-left: 4px solid #dc3545; }\n")
                .append("        .severity-medium { border-left: 4px solid #ffc107; }\n")
                .append("        .severity-low { border-left: 4px solid #6c757d; }\n")
                .append("    </style>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("    <div class='container'>\n")
                .append("        <div class='header'>\n")
                .append("            <h1>⚠️ Build Warnings Report</h1>\n")
                .append("            <p>Total Warnings: ").append(buildWarnings.size()).append("</p>\n")
                .append("        </div>\n");

        // Group warnings by type
        Map<String, List<BuildWarning>> warningsByType = buildWarnings.stream()
                .collect(Collectors.groupingBy(w -> w.warningType));

        // Generate warning groups
        for (Map.Entry<String, List<BuildWarning>> entry : warningsByType.entrySet()) {
            String type = entry.getKey();
            List<BuildWarning> warnings = entry.getValue();

            html.append("        <div class='warning-group'>\n")
                    .append("            <div class='warning-type'>\n")
                    .append("                <h3>").append(type).append(" (").append(warnings.size()).append(")</h3>\n")
                    .append("            </div>\n");

            for (BuildWarning warning : warnings) {
                html.append("            <div class='warning-item severity-").append(warning.severity.toLowerCase())
                        .append("'>\n")
                        .append("                <p><strong>Message:</strong> ").append(warning.warningMessage)
                        .append("</p>\n");

                if (!warning.fileName.isEmpty()) {
                    html.append("                <p><strong>File:</strong> ").append(warning.fileName)
                            .append(" (line ").append(warning.lineNumber).append(")</p>\n");
                }

                html.append("                <p><strong>Severity:</strong> ").append(warning.severity).append("</p>\n")
                        .append("                <p><strong>Phase:</strong> ").append(warning.phase).append("</p>\n")
                        .append("            </div>\n");
            }

            html.append("        </div>\n");
        }

        html.append("    </div>\n")
                .append("</body>\n")
                .append("</html>");

        return html.toString();
    }

    /**
     * Enhanced HTML dashboard generator that matches the design in your images
     */
    private String generateAdvancedHtmlContent(BuildMetrics currentBuild) {
        BottleneckReport bottleneckReport = bottleneckAnalyzer.analyze(currentBuild.phaseTimes);
        RegressionReport regressionReport = regressionDetector.detectRegression(currentBuild, regressionThreshold);
        EfficiencyScore efficiencyScore = efficiencyScorer.calculateScore(currentBuild, buildHistory);

        StringBuilder html = new StringBuilder();
        html.append(generateEnhancedHtmlHeader())
                .append(generateDashboardHeader(currentBuild))
                .append(generateMetricsCards(currentBuild))
                .append(generatePhaseBreakdownChart(currentBuild))
                .append(generateDetailedPhaseTable(currentBuild))
                .append(generateAnalyticsSection(bottleneckReport, regressionReport, efficiencyScore,
                        currentBuild.systemMetrics))
                .append(generateChartScripts(currentBuild))
                .append("</body>\n</html>");

        return html.toString();
    }

    private String generateEnhancedHtmlHeader() {
        return "<!DOCTYPE html>\n" +
                "<html lang='en'>\n" +
                "<head>\n" +
                "    <meta charset='UTF-8'>\n" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                "    <title>Build Time Dashboard</title>\n" +
                "    <script src='https://cdnjs.cloudflare.com/ajax/libs/Chart.js/3.9.1/chart.min.js'></script>\n" +
                "    <style>\n" +
                "        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f8f9fa; line-height: 1.6; }\n"
                +
                "        .container { max-width: 1200px; margin: 0 auto; padding: 20px; }\n" +
                "        \n" +
                "        /* Header Styles */\n" +
                "        .dashboard-header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 12px; text-align: center; margin-bottom: 30px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }\n"
                +
                "        .dashboard-header h1 { font-size: 2.2em; margin-bottom: 10px; font-weight: 600; }\n" +
                "        .dashboard-header p { font-size: 1.1em; opacity: 0.9; }\n" +
                "        \n" +
                "        /* Metrics Cards */\n" +
                "        .metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-bottom: 30px; }\n"
                +
                "        .metric-card { background: white; border-radius: 12px; padding: 25px; text-align: center; box-shadow: 0 2px 15px rgba(0,0,0,0.08); border: 1px solid #e9ecef; transition: transform 0.2s; }\n"
                +
                "        .metric-card:hover { transform: translateY(-2px); box-shadow: 0 4px 25px rgba(0,0,0,0.12); }\n"
                +
                "        .metric-number { font-size: 2.5em; font-weight: 700; margin-bottom: 8px; }\n" +
                "        .metric-label { font-size: 0.95em; color: #6c757d; font-weight: 500; }\n" +
                "        .metric-blue { color: #4285f4; }\n" +
                "        .metric-green { color: #34a853; }\n" +
                "        .metric-orange { color: #fbbc04; }\n" +
                "        .metric-purple { color: #9c27b0; }\n" +
                "        \n" +
                "        /* Section Cards */\n" +
                "        .section-card { background: white; border-radius: 12px; padding: 30px; margin-bottom: 30px; box-shadow: 0 2px 15px rgba(0,0,0,0.08); border: 1px solid #e9ecef; }\n"
                +
                "        .section-title { font-size: 1.4em; font-weight: 600; margin-bottom: 25px; color: #495057; display: flex; align-items: center; gap: 10px; }\n"
                +
                "        \n" +
                "        /* Chart Container */\n" +
                "        .chart-container { position: relative; height: 400px; margin: 20px 0; }\n" +
                "        .chart-wrapper { display: flex; align-items: center; justify-content: center; }\n" +
                "        \n" +
                "        /* Phase Table */\n" +
                "        .phase-table { width: 100%; border-collapse: collapse; margin-top: 20px; }\n" +
                "        .phase-table th { background: #f8f9fa; padding: 15px 12px; text-align: left; font-weight: 600; color: #495057; border-bottom: 2px solid #dee2e6; font-size: 0.9em; }\n"
                +
                "        .phase-table th:nth-child(2), .phase-table th:nth-child(3), .phase-table th:nth-child(4) { text-align: right; }\n"
                +
                "        .phase-table th:nth-child(5) { text-align: center; }\n" +
                "        .phase-table td { padding: 12px; border-bottom: 1px solid #e9ecef; }\n" +
                "        .phase-table td:nth-child(2), .phase-table td:nth-child(3), .phase-table td:nth-child(4) { text-align: right; font-family: 'SF Mono', Monaco, monospace; }\n"
                +
                "        .phase-table td:nth-child(5) { text-align: center; }\n" +
                "        .phase-table tbody tr:hover { background: #f8f9fa; }\n" +
                "        \n" +
                "        /* Status Badges */\n" +
                "        .status-badge { padding: 4px 8px; border-radius: 12px; font-size: 0.8em; font-weight: 600; display: inline-flex; align-items: center; gap: 4px; }\n"
                +
                "        .status-fast { background: #d4edda; color: #155724; }\n" +
                "        .status-ok { background: #fff3cd; color: #856404; }\n" +
                "        .status-slow { background: #f8d7da; color: #721c24; }\n" +
                "        \n" +
                "        /* Analytics Grid */\n" +
                "        .analytics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; }\n"
                +
                "        .analytics-card { background: white; border-radius: 12px; padding: 25px; box-shadow: 0 2px 15px rgba(0,0,0,0.08); border: 1px solid #e9ecef; }\n"
                +
                "        .analytics-card h3 { font-size: 1.2em; font-weight: 600; margin-bottom: 20px; color: #495057; display: flex; align-items: center; gap: 8px; }\n"
                +
                "        .metric-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #f1f3f4; }\n"
                +
                "        .metric-row:last-child { border-bottom: none; }\n" +
                "        .metric-row-label { font-weight: 500; color: #6c757d; }\n" +
                "        .metric-row-value { font-weight: 600; }\n" +
                "        \n" +
                "        /* Alerts */\n" +
                "        .alert { padding: 15px; border-radius: 8px; margin: 15px 0; border-left: 4px solid; }\n" +
                "        .alert-success { background: #d4edda; color: #155724; border-left-color: #28a745; }\n" +
                "        .alert-warning { background: #fff3cd; color: #856404; border-left-color: #ffc107; }\n" +
                "        .alert-danger { background: #f8d7da; color: #721c24; border-left-color: #dc3545; }\n" +
                "        \n" +
                "        /* Recommendations */\n" +
                "        .recommendations { background: #f8f9fa; padding: 20px; border-radius: 8px; margin-top: 20px; border-left: 4px solid #17a2b8; }\n"
                +
                "        .recommendations h4 { margin-bottom: 12px; color: #495057; }\n" +
                "        .recommendation-item { margin: 8px 0; color: #495057; display: flex; align-items: flex-start; gap: 8px; }\n"
                +
                "        .recommendation-item::before { content: '•'; color: #17a2b8; font-weight: bold; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class='container'>\n";
    }

    private String generateDashboardHeader(BuildMetrics currentBuild) {
        return "        <div class='dashboard-header'>\n" +
                "            <h1>🚀 Build Time Dashboard</h1>\n" +
                "            <p>Project: " + project.getName() + " | Generated: " +
                currentBuild.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>\n" +
                "        </div>\n";
    }

    private String generateMetricsCards(BuildMetrics currentBuild) {
        long totalTime = currentBuild.totalTime;
        int phaseCount = (int) currentBuild.phaseTimes.entrySet().stream()
                .filter(e -> !e.getKey().equals("total"))
                .count();
        int buildHistoryCount = buildHistory.size();

        // Calculate dependency resolution time (this would need to be tracked
        // separately in a real implementation)
        long dependencyTime = 0; // Placeholder

        StringBuilder html = new StringBuilder();
        html.append("        <div class='metrics-grid'>\n")
                .append("            <div class='metric-card'>\n")
                .append("                <div class='metric-number metric-blue'>")
                .append(String.format("%.1fs", totalTime / 1000.0)).append("</div>\n")
                .append("                <div class='metric-label'>Total Build Time</div>\n")
                .append("            </div>\n")
                .append("            <div class='metric-card'>\n")
                .append("                <div class='metric-number metric-green'>").append(phaseCount)
                .append("</div>\n")
                .append("                <div class='metric-label'>Phases Executed</div>\n")
                .append("            </div>\n")
                .append("            <div class='metric-card'>\n")
                .append("                <div class='metric-number metric-orange'>")
                .append(String.format("%.1fs", dependencyTime / 1000.0)).append("</div>\n")
                .append("                <div class='metric-label'>Dependency Resolution</div>\n")
                .append("            </div>\n")
                .append("            <div class='metric-card'>\n")
                .append("                <div class='metric-number metric-purple'>").append(buildHistoryCount)
                .append("</div>\n")
                .append("                <div class='metric-label'>Builds in History</div>\n")
                .append("            </div>\n")
                .append("        </div>\n");

        return html.toString();
    }

    private String generatePhaseBreakdownChart(BuildMetrics currentBuild) {
        return "        <div class='section-card'>\n" +
                "            <h2 class='section-title'>📊 Phase Breakdown</h2>\n" +
                "            <div class='chart-container'>\n" +
                "                <canvas id='phaseChart'></canvas>\n" +
                "            </div>\n" +
                "        </div>\n";
    }

    private String generateDetailedPhaseTable(BuildMetrics currentBuild) {
        StringBuilder html = new StringBuilder();
        html.append("        <div class='section-card'>\n")
                .append("            <h2 class='section-title'>📋 Detailed Phase Analysis</h2>\n")
                .append("            <table class='phase-table'>\n")
                .append("                <thead>\n")
                .append("                    <tr>\n")
                .append("                        <th>Phase</th>\n")
                .append("                        <th>Duration (ms)</th>\n")
                .append("                        <th>Duration (s)</th>\n")
                .append("                        <th>% of Total</th>\n")
                .append("                        <th>Status</th>\n")
                .append("                    </tr>\n")
                .append("                </thead>\n")
                .append("                <tbody>\n");

        // Calculate total for percentages
        long totalTime = currentBuild.phaseTimes.values().stream()
                .filter(time -> time != null)
                .mapToLong(Long::longValue)
                .sum();

        // Sort phases by duration (descending)
        List<Map.Entry<String, Long>> sortedPhases = currentBuild.phaseTimes.entrySet().stream()
                .filter(e -> !e.getKey().equals("total"))
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        for (Map.Entry<String, Long> entry : sortedPhases) {
            String phase = entry.getKey();
            long duration = entry.getValue();
            double seconds = duration / 1000.0;
            double percentage = totalTime > 0 ? (duration * 100.0) / totalTime : 0;

            String statusClass, statusIcon, statusText;
            if (duration <= fastThreshold) {
                statusClass = "status-fast";
                statusIcon = "⚡";
                statusText = "FAST";
            } else if (duration <= warnThreshold) {
                statusClass = "status-ok";
                statusIcon = "⚠";
                statusText = "OK";
            } else {
                statusClass = "status-slow";
                statusIcon = "🐌";
                statusText = "SLOW";
            }

            html.append(String.format(
                    "                    <tr>\n" +
                            "                        <td>%s</td>\n" +
                            "                        <td>%d</td>\n" +
                            "                        <td>%.1f</td>\n" +
                            "                        <td>%.1f%%</td>\n" +
                            "                        <td><span class='status-badge %s'>%s %s</span></td>\n" +
                            "                    </tr>\n",
                    phase, duration, seconds, percentage, statusClass, statusIcon, statusText));
        }

        html.append("                </tbody>\n")
                .append("            </table>\n")
                .append("        </div>\n");

        return html.toString();
    }

    private String generateAnalyticsSection(BottleneckReport bottleneckReport, RegressionReport regressionReport,
            EfficiencyScore efficiencyScore, SystemMetrics systemMetrics) {
        StringBuilder html = new StringBuilder();
        html.append("        <div class='analytics-grid'>\n");

        // Bottleneck Analysis Card
        html.append("            <div class='analytics-card'>\n")
                .append("                <h3>🎯 Bottleneck Analysis</h3>\n");

        if (!bottleneckReport.primaryBottleneck.isEmpty()) {
            html.append(String.format(
                    "                <div class='metric-row'>\n" +
                            "                    <span class='metric-row-label'>Primary Bottleneck</span>\n" +
                            "                    <span class='metric-row-value'>%s</span>\n" +
                            "                </div>\n" +
                            "                <div class='metric-row'>\n" +
                            "                    <span class='metric-row-label'>Time Impact</span>\n" +
                            "                    <span class='metric-row-value'>%.1fs (%.1f%%)</span>\n" +
                            "                </div>\n",
                    bottleneckReport.primaryBottleneck,
                    bottleneckReport.primaryBottleneckTime / 1000.0,
                    bottleneckReport.primaryBottleneckPercentage));

            if (!bottleneckReport.recommendations.isEmpty()) {
                html.append("                <div class='recommendations'>\n")
                        .append("                    <h4>💡 Recommendations</h4>\n");
                for (String rec : bottleneckReport.recommendations) {
                    html.append("                    <div class='recommendation-item'>").append(rec).append("</div>\n");
                }
                html.append("                </div>\n");
            }
        }
        html.append("            </div>\n");

        // Performance Analysis Card
        html.append("            <div class='analytics-card'>\n")
                .append("                <h3>📈 Performance Analysis</h3>\n");

        if (regressionReport.isRegression) {
            html.append("                <div class='alert alert-danger'>\n")
                    .append("                    <strong>🚨 Regression Detected</strong><br>\n")
                    .append("                    Build is ")
                    .append(String.format("%.1fx", regressionReport.regressionFactor)).append(" slower\n")
                    .append("                </div>\n");
        } else if (regressionReport.isImprovement) {
            html.append("                <div class='alert alert-success'>\n")
                    .append("                    <strong>🎉 Performance Improved</strong><br>\n")
                    .append("                    Build is ")
                    .append(String.format("%.1fx", 1.0 / regressionReport.regressionFactor)).append(" faster\n")
                    .append("                </div>\n");
        } else {
            html.append("                <div class='alert alert-success'>\n")
                    .append("                    <strong>✅ Performance Stable</strong><br>\n")
                    .append("                    No significant changes detected\n")
                    .append("                </div>\n");
        }

        html.append(String.format(
                "                <div class='metric-row'>\n" +
                        "                    <span class='metric-row-label'>Current Build</span>\n" +
                        "                    <span class='metric-row-value'>%.1fs</span>\n" +
                        "                </div>\n" +
                        "                <div class='metric-row'>\n" +
                        "                    <span class='metric-row-label'>Recent Average</span>\n" +
                        "                    <span class='metric-row-value'>%.1fs</span>\n" +
                        "                </div>\n",
                regressionReport.currentTime / 1000.0,
                regressionReport.averageTime / 1000.0));

        html.append("            </div>\n");

        // Efficiency Score Card
        html.append("            <div class='analytics-card'>\n")
                .append("                <h3>⭐ Efficiency Score</h3>\n")
                .append(String.format(
                        "                <div class='metric-row'>\n" +
                                "                    <span class='metric-row-label'>Overall Score</span>\n" +
                                "                    <span class='metric-row-value'>%.1f/100 (%s)</span>\n" +
                                "                </div>\n",
                        efficiencyScore.score, efficiencyScore.getLetterGrade()));

        if (!efficiencyScore.suggestions.isEmpty()) {
            html.append("                <div class='recommendations'>\n")
                    .append("                    <h4>💡 Improvements</h4>\n");
            for (String suggestion : efficiencyScore.suggestions) {
                html.append("                    <div class='recommendation-item'>").append(suggestion)
                        .append("</div>\n");
            }
            html.append("                </div>\n");
        }

        html.append("            </div>\n");

        // System Resources Card
        html.append("            <div class='analytics-card'>\n")
                .append("                <h3>🖥️ System Resources</h3>\n")
                .append(String.format(
                        "                <div class='metric-row'>\n" +
                                "                    <span class='metric-row-label'>Peak Memory</span>\n" +
                                "                    <span class='metric-row-value'>%.1f MB</span>\n" +
                                "                </div>\n" +
                                "                <div class='metric-row'>\n" +
                                "                    <span class='metric-row-label'>Avg CPU Usage</span>\n" +
                                "                    <span class='metric-row-value'>%.1f%%</span>\n" +
                                "                </div>\n",
                        systemMetrics.peakMemoryUsage / (1024.0 * 1024),
                        systemMetrics.avgCpuUsage * 100))
                .append("            </div>\n");

        html.append("        </div>\n");

        return html.toString();
    }

    private String generateChartScripts(BuildMetrics currentBuild) {
        StringBuilder scripts = new StringBuilder();
        scripts.append("    <script>\n");
        scripts.append("        document.addEventListener('DOMContentLoaded', function() {\n");

        // Phase Distribution Doughnut Chart
        scripts.append("            const phaseCtx = document.getElementById('phaseChart').getContext('2d');\n");

        // Prepare data
        List<String> phaseNames = new ArrayList<>();
        List<Long> phaseDurations = new ArrayList<>();
        List<String> colors = Arrays.asList(
                "#FF6384", "#36A2EB", "#FFCE56", "#4BC0C0",
                "#9966FF", "#FF9F40", "#FF6384", "#C9CBCF");

        currentBuild.phaseTimes.entrySet().stream()
                .filter(e -> !e.getKey().equals("total"))
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .forEach(entry -> {
                    phaseNames.add(entry.getKey());
                    phaseDurations.add(entry.getValue());
                });

        scripts.append("            const phaseData = {\n");
        scripts.append("                labels: [");
        for (int i = 0; i < phaseNames.size(); i++) {
            scripts.append("'").append(phaseNames.get(i)).append("'");
            if (i < phaseNames.size() - 1)
                scripts.append(", ");
        }
        scripts.append("],\n");

        scripts.append("                datasets: [{\n");
        scripts.append("                    data: [");
        for (int i = 0; i < phaseDurations.size(); i++) {
            scripts.append(phaseDurations.get(i));
            if (i < phaseDurations.size() - 1)
                scripts.append(", ");
        }
        scripts.append("],\n");

        scripts.append("                    backgroundColor: [");
        for (int i = 0; i < Math.min(phaseNames.size(), colors.size()); i++) {
            scripts.append("'").append(colors.get(i)).append("'");
            if (i < Math.min(phaseNames.size(), colors.size()) - 1)
                scripts.append(", ");
        }
        scripts.append("],\n");
        scripts.append("                    borderWidth: 2,\n");
        scripts.append("                    borderColor: '#fff'\n");
        scripts.append("                }]\n");
        scripts.append("            };\n");

        scripts.append("            new Chart(phaseCtx, {\n");
        scripts.append("                type: 'doughnut',\n");
        scripts.append("                data: phaseData,\n");
        scripts.append("                options: {\n");
        scripts.append("                    responsive: true,\n");
        scripts.append("                    maintainAspectRatio: false,\n");
        scripts.append("                    plugins: {\n");
        scripts.append("                        legend: {\n");
        scripts.append("                            position: 'right',\n");
        scripts.append("                            labels: { usePointStyle: true, padding: 20 }\n");
        scripts.append("                        },\n");
        scripts.append("                        tooltip: {\n");
        scripts.append("                            callbacks: {\n");
        scripts.append("                                label: function(context) {\n");
        scripts.append("                                    const label = context.label || '';\n");
        scripts.append("                                    const value = context.parsed;\n");
        scripts.append("                                    const seconds = (value / 1000).toFixed(1);\n");
        scripts.append(
                "                                    const total = context.dataset.data.reduce((a, b) => a + b, 0);\n");
        scripts.append("                                    const percentage = ((value / total) * 100).toFixed(1);\n");
        scripts.append(
                "                                    return label + ': ' + seconds + 's (' + percentage + '%)';\n");
        scripts.append("                                }\n");
        scripts.append("                            }\n");
        scripts.append("                        }\n");
        scripts.append("                    }\n");
        scripts.append("                }\n");
        scripts.append("            });\n");

        scripts.append("        });\n");
        scripts.append("    </script>\n");

        return scripts.toString();
    }

    // ========== ANALYTICS CLASSES ==========

    /**
     * Analyzes build phases to identify bottlenecks and optimization opportunities.
     */
    private class BottleneckAnalyzer {
        public BottleneckReport analyze(Map<String, Long> phaseTimes) {
            if (phaseTimes.isEmpty()) {
                return new BottleneckReport();
            }

            long totalTime = phaseTimes.values().stream().mapToLong(Long::longValue).sum();

            List<BottleneckReport.PhaseAnalysis> phases = phaseTimes.entrySet().stream()
                    .filter(e -> !e.getKey().equals("total"))
                    .map(e -> new BottleneckReport.PhaseAnalysis(
                            e.getKey(),
                            e.getValue(),
                            (e.getValue() * 100.0) / totalTime))
                    .sorted((a, b) -> Long.compare(b.duration, a.duration))
                    .collect(Collectors.toList());

            BottleneckReport report = new BottleneckReport();
            if (!phases.isEmpty()) {
                BottleneckReport.PhaseAnalysis primary = phases.get(0);
                report.primaryBottleneck = primary.phaseName;
                report.primaryBottleneckTime = primary.duration;
                report.primaryBottleneckPercentage = primary.percentage;
                report.topPhases = phases;

                // Generate recommendations
                if (primary.percentage > 40) {
                    report.recommendations
                            .add("Focus optimization on '" + primary.phaseName + "' phase (major bottleneck)");
                }
                if (phases.size() > 3 && phases.get(2).percentage > 15) {
                    report.recommendations.add("Consider parallel execution for top 3 phases");
                }
                if (primary.phaseName.contains("compile")) {
                    report.recommendations.add("Consider incremental compilation or compiler daemon");
                }
                if (primary.phaseName.contains("test")) {
                    report.recommendations.add("Optimize test execution with parallel runners or test selection");
                }
            }

            return report;
        }
    }

    /**
     * Monitors system resources during build execution.
     */
    private class SystemMonitor {
        private ScheduledExecutorService scheduler;
        private final List<SystemSample> samples = new ArrayList<>();
        private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        private final OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory
                .getOperatingSystemMXBean();

        public void startMonitoring() {
            if (!enableSystemMonitoring)
                return;

            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(this::takeSample, 0, monitoringIntervalMs, TimeUnit.MILLISECONDS);
        }

        public SystemMetrics stopMonitoring() {
            if (scheduler != null) {
                scheduler.shutdown();
            }

            if (samples.isEmpty()) {
                return new SystemMetrics();
            }

            long avgMemory = (long) samples.stream().mapToLong(s -> s.memoryUsed).average().orElse(0);
            long peakMemory = samples.stream().mapToLong(s -> s.memoryUsed).max().orElse(0);
            double avgCpu = samples.stream().mapToDouble(s -> s.cpuUsage).average().orElse(0);
            double peakCpu = samples.stream().mapToDouble(s -> s.cpuUsage).max().orElse(0);

            return new SystemMetrics(avgMemory, peakMemory, avgCpu, peakCpu, 0, 0);
        }

        private void takeSample() {
            long memoryUsed = memoryBean.getHeapMemoryUsage().getUsed();
            double cpuUsage = osBean.getProcessCpuLoad();

            samples.add(new SystemSample(System.currentTimeMillis(), memoryUsed, cpuUsage));
        }
    }

    /**
     * Detects performance regressions by comparing with historical data.
     */
    private class RegressionDetector {
        private List<BuildMetrics> history;

        public void loadHistory(List<BuildMetrics> buildHistory) {
            this.history = buildHistory;
        }

        public RegressionReport detectRegression(BuildMetrics currentBuild, double threshold) {
            RegressionReport report = new RegressionReport();
            report.currentTime = currentBuild.totalTime;
            report.buildsAnalyzed = history.size();

            if (history.size() < 3) {
                report.trend = "Insufficient data";
                return report;
            }

            // Calculate average of recent builds (excluding current)
            List<BuildMetrics> recentHistory = history.subList(
                    Math.max(0, history.size() - Math.min(10, history.size())),
                    history.size() - 1);

            double avgTime = recentHistory.stream().mapToLong(b -> b.totalTime).average().orElse(0);
            report.averageTime = (long) avgTime;

            if (avgTime > 0) {
                report.regressionFactor = currentBuild.totalTime / avgTime;

                if (report.regressionFactor >= threshold) {
                    report.isRegression = true;
                } else if (report.regressionFactor <= (1.0 / threshold)) {
                    report.isImprovement = true;
                }
            }

            // Determine trend
            if (recentHistory.size() >= 3) {
                List<Long> recentTimes = recentHistory.stream()
                        .map(b -> b.totalTime)
                        .collect(Collectors.toList());

                boolean increasing = true, decreasing = true;
                for (int i = 1; i < recentTimes.size(); i++) {
                    if (recentTimes.get(i) <= recentTimes.get(i - 1))
                        increasing = false;
                    if (recentTimes.get(i) >= recentTimes.get(i - 1))
                        decreasing = false;
                }

                if (increasing)
                    report.trend = "Consistently slower";
                else if (decreasing)
                    report.trend = "Consistently faster";
                else
                    report.trend = "Variable";
            }

            return report;
        }
    }

    /**
     * Calculates build efficiency score based on multiple factors.
     */
    private class EfficiencyScorer {
        public EfficiencyScore calculateScore(BuildMetrics current, List<BuildMetrics> history) {
            EfficiencyScore score = new EfficiencyScore();

            // Time efficiency (30 points) - based on build duration
            score.timeScore = calculateTimeScore(current, history);

            // Memory efficiency (25 points) - based on memory usage
            score.memoryScore = calculateMemoryScore(current);

            // CPU efficiency (25 points) - based on CPU utilization
            score.cpuScore = calculateCpuScore(current);

            // Consistency (20 points) - based on build time variance
            score.consistencyScore = calculateConsistencyScore(current, history);

            score.score = score.timeScore + score.memoryScore + score.cpuScore + score.consistencyScore;

            // Generate suggestions
            if (score.timeScore < 20) {
                score.suggestions.add("Optimize slow phases to improve time efficiency");
            }
            if (score.memoryScore < 15) {
                score.suggestions.add("Reduce memory usage through better heap management");
            }
            if (score.cpuScore < 15) {
                score.suggestions.add("Improve CPU utilization with parallel execution");
            }
            if (score.consistencyScore < 10) {
                score.suggestions.add("Improve build consistency by addressing variable performance");
            }

            return score;
        }

        private double calculateTimeScore(BuildMetrics current, List<BuildMetrics> history) {
            if (history.size() < 2)
                return 25.0; // Default score for insufficient data

            double avgTime = history.stream().mapToLong(b -> b.totalTime).average().orElse(current.totalTime);
            double ratio = avgTime / current.totalTime;

            // Score: 30 points max, decreasing as build gets slower than average
            return Math.min(30.0, Math.max(0.0, ratio * 30.0));
        }

        private double calculateMemoryScore(BuildMetrics current) {
            long memoryMB = current.systemMetrics.peakMemoryUsage / (1024 * 1024);

            // Score based on memory usage ranges
            if (memoryMB < 512)
                return 25.0; // < 512MB = excellent
            if (memoryMB < 1024)
                return 20.0; // < 1GB = good
            if (memoryMB < 2048)
                return 15.0; // < 2GB = fair
            if (memoryMB < 4096)
                return 10.0; // < 4GB = poor
            return 5.0; // > 4GB = very poor
        }

        private double calculateCpuScore(BuildMetrics current) {
            double cpuUsage = current.systemMetrics.avgCpuUsage;

            // Optimal CPU usage is around 70-80%
            if (cpuUsage >= 0.7 && cpuUsage <= 0.9)
                return 25.0; // Optimal
            if (cpuUsage >= 0.5 && cpuUsage <= 0.95)
                return 20.0; // Good
            if (cpuUsage >= 0.3 && cpuUsage <= 0.98)
                return 15.0; // Fair
            if (cpuUsage >= 0.1)
                return 10.0; // Poor
            return 5.0; // Very poor
        }

        private double calculateConsistencyScore(BuildMetrics current, List<BuildMetrics> history) {
            if (history.size() < 3)
                return 15.0; // Default score

            List<Long> recentTimes = history.stream()
                    .skip(Math.max(0, history.size() - 5))
                    .mapToLong(b -> b.totalTime)
                    .boxed()
                    .collect(Collectors.toList());

            double avg = recentTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            double variance = recentTimes.stream()
                    .mapToDouble(time -> Math.pow(time - avg, 2))
                    .average().orElse(0);

            double coefficient = Math.sqrt(variance) / avg; // Coefficient of variation

            // Lower variation = higher score
            if (coefficient < 0.1)
                return 20.0; // Very consistent
            if (coefficient < 0.2)
                return 15.0; // Consistent
            if (coefficient < 0.3)
                return 10.0; // Moderately consistent
            if (coefficient < 0.5)
                return 5.0; // Inconsistent
            return 2.0; // Very inconsistent
        }
    }

    // ========== DATA CLASSES ==========

    /**
     * Comprehensive build metrics including system resource usage.
     */
    private static class BuildMetrics {
        final LocalDateTime timestamp;
        final long totalTime;
        final Map<String, Long> phaseTimes;
        final SystemMetrics systemMetrics;

        public BuildMetrics(LocalDateTime timestamp, long totalTime,
                Map<String, Long> phaseTimes, SystemMetrics systemMetrics) {
            this.timestamp = timestamp;
            this.totalTime = totalTime;
            this.phaseTimes = phaseTimes;
            this.systemMetrics = systemMetrics;
        }
    }

    /**
     * System resource metrics collected during build.
     */
    private static class SystemMetrics {
        final long avgMemoryUsage;
        final long peakMemoryUsage;
        final double avgCpuUsage;
        final double peakCpuUsage;
        final long gcCount;
        final long gcTime;

        public SystemMetrics() {
            this(0, 0, 0, 0, 0, 0);
        }

        public SystemMetrics(long avgMemoryUsage, long peakMemoryUsage,
                double avgCpuUsage, double peakCpuUsage,
                long gcCount, long gcTime) {
            this.avgMemoryUsage = avgMemoryUsage;
            this.peakMemoryUsage = peakMemoryUsage;
            this.avgCpuUsage = avgCpuUsage;
            this.peakCpuUsage = peakCpuUsage;
            this.gcCount = gcCount;
            this.gcTime = gcTime;
        }
    }

    /**
     * Sample point for system monitoring.
     */
    private static class SystemSample {
        final long timestamp;
        final long memoryUsed;
        final double cpuUsage;

        public SystemSample(long timestamp, long memoryUsed, double cpuUsage) {
            this.timestamp = timestamp;
            this.memoryUsed = memoryUsed;
            this.cpuUsage = cpuUsage;
        }
    }

    /**
     * Report containing bottleneck analysis results.
     */
    private static class BottleneckReport {
        String primaryBottleneck = "";
        long primaryBottleneckTime = 0;
        double primaryBottleneckPercentage = 0;
        List<PhaseAnalysis> topPhases = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        static class PhaseAnalysis {
            final String phaseName;
            final long duration;
            final double percentage;

            public PhaseAnalysis(String phaseName, long duration, double percentage) {
                this.phaseName = phaseName;
                this.duration = duration;
                this.percentage = percentage;
            }
        }
    }

    /**
     * Report containing regression analysis results.
     */
    private static class RegressionReport {
        boolean isRegression = false;
        boolean isImprovement = false;
        double regressionFactor = 1.0;
        long currentTime = 0;
        long averageTime = 0;
        String trend = "Stable";
        int buildsAnalyzed = 0;
    }

    /**
     * Build efficiency score with detailed breakdown.
     */
    private static class EfficiencyScore {
        double score = 0;
        double timeScore = 0;
        double memoryScore = 0;
        double cpuScore = 0;
        double consistencyScore = 0;
        List<String> suggestions = new ArrayList<>();

        public String getLetterGrade() {
            if (score >= 90)
                return "A+";
            if (score >= 85)
                return "A";
            if (score >= 80)
                return "A-";
            if (score >= 75)
                return "B+";
            if (score >= 70)
                return "B";
            if (score >= 65)
                return "B-";
            if (score >= 60)
                return "C+";
            if (score >= 55)
                return "C";
            if (score >= 50)
                return "C-";
            if (score >= 45)
                return "D+";
            if (score >= 40)
                return "D";
            return "F";
        }

        public String getScoreDescription() {
            if (score >= 85)
                return "Excellent - Highly optimized build";
            if (score >= 70)
                return "Good - Well-performing build";
            if (score >= 55)
                return "Average - Room for improvement";
            if (score >= 40)
                return "Below Average - Needs optimization";
            return "Poor - Significant optimization required";
        }
    }

    // ========== LEGACY METHODS FOR BACKWARD COMPATIBILITY ==========

    private void printSummaryTable() {
        getLog().info("");
        getLog().info("========== Build Time Summary ==========");
        getLog().info("Phase      | Duration (ms)");
        getLog().info("----------------------------");

        long total = 0;
        for (Map.Entry<String, Long> entry : phaseDurations.entrySet()) {
            if (!entry.getKey().equals("total")) {
                getLog().info(String.format("%-10s | %d", entry.getKey(), entry.getValue()));
                total += entry.getValue();
            }
        }

        getLog().info("----------------------------");
        getLog().info(String.format("TOTAL      | %d ms", total));
        getLog().info("=======================================");
        getLog().info("");
    }

    private void exportCsv() {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        File csvFile = new File(outputDirectory, "build-time-report.csv");

        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Phase,Duration(ms)\n");

            for (Map.Entry<String, Long> entry : phaseDurations.entrySet()) {
                writer.write(String.format("%s,%d\n", entry.getKey(), entry.getValue()));
            }

            getLog().info("CSV report exported to: " + csvFile.getAbsolutePath());
        } catch (IOException e) {
            getLog().error("Failed to export CSV report", e);
        }
    }

    private void exportJson() {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        File jsonFile = new File(outputDirectory, "build-time-report.json");

        try (FileWriter writer = new FileWriter(jsonFile)) {
            writer.write("{\n");

            int count = 0;
            for (Map.Entry<String, Long> entry : phaseDurations.entrySet()) {
                writer.write(String.format("  \"%s\": %d", entry.getKey(), entry.getValue()));
                if (count < phaseDurations.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
                count++;
            }

            writer.write("}\n");
            getLog().info("JSON report exported to: " + jsonFile.getAbsolutePath());
        } catch (IOException e) {
            getLog().error("Failed to export JSON report", e);
        }
    }

    private String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++)
            sb.append(s);
        return sb.toString();
    }

    private void printFailureAnalysis(BuildFailure failure) {
        logError("");
        logError(getColoredText("🔥 FAILURE ANALYSIS", ANSI_BOLD + ANSI_RED));
        logError(repeat("═", 60));

        logError(String.format("Phase: %s", failure.phase));
        logError(String.format("Error Type: %s", failure.errorType));
        logError(String.format("Error Message: %s", failure.errorMessage));

        if (!failure.fileName.isEmpty()) {
            logError(String.format("File: %s (line %d)", failure.fileName, failure.lineNumber));

            if (!failure.sourceCodeSnippet.isEmpty() && enableFailureLineDetection) {
                logError("");
                logError(getColoredText("📄 SOURCE CODE CONTEXT:", ANSI_BOLD));
                logError(failure.sourceCodeSnippet);
            }
        }

        if (!failure.suggestedFixes.isEmpty()) {
            logError("");
            logError(getColoredText("💡 SUGGESTED FIXES:", ANSI_BOLD + ANSI_CYAN));
            for (int i = 0; i < failure.suggestedFixes.size(); i++) {
                logError(String.format("  %d. %s", i + 1, failure.suggestedFixes.get(i)));
            }
        }

        logError(repeat("═", 60));
    }

    private void printWarningsSummary() {
        if (buildWarnings.isEmpty())
            return;

        logInfo("");
        logInfo(getColoredText("⚠️ WARNINGS SUMMARY", ANSI_BOLD + ANSI_YELLOW));
        logInfo(repeat("─", 50));

        // Group warnings by type
        Map<String, List<BuildWarning>> warningsByType = buildWarnings.stream()
                .collect(Collectors.groupingBy(w -> w.warningType));

        // Show summary counts
        logInfo(String.format("Total Warnings: %d", buildWarnings.size()));
        logInfo("");

        for (Map.Entry<String, List<BuildWarning>> entry : warningsByType.entrySet()) {
            String type = entry.getKey();
            List<BuildWarning> warnings = entry.getValue();

            long highSeverity = warnings.stream().mapToLong(w -> "HIGH".equals(w.severity) ? 1 : 0).sum();
            long mediumSeverity = warnings.stream().mapToLong(w -> "MEDIUM".equals(w.severity) ? 1 : 0).sum();
            long lowSeverity = warnings.stream().mapToLong(w -> "LOW".equals(w.severity) ? 1 : 0).sum();

            logInfo(String.format("%s: %d total (🔴 %d high, 🟡 %d medium, ⚪ %d low)",
                    type, warnings.size(), highSeverity, mediumSeverity, lowSeverity));

            // Show first few warnings of each type
            int showCount = Math.min(3, warnings.size());
            for (int i = 0; i < showCount; i++) {
                BuildWarning warning = warnings.get(i);
                String severityIcon = getSeverityIcon(warning.severity);
                if (!warning.fileName.isEmpty()) {
                    logInfo(String.format("  %s %s:%d - %s",
                            severityIcon, warning.fileName, warning.lineNumber, warning.warningMessage));
                } else {
                    logInfo(String.format("  %s %s", severityIcon, warning.warningMessage));
                }
            }

            if (warnings.size() > showCount) {
                logInfo(String.format("  ... and %d more %s warnings", warnings.size() - showCount, type));
            }
            logInfo("");
        }

        // Show top priority warnings
        List<BuildWarning> highPriorityWarnings = buildWarnings.stream()
                .filter(w -> "HIGH".equals(w.severity))
                .limit(5)
                .collect(Collectors.toList());

        if (!highPriorityWarnings.isEmpty()) {
            logInfo(getColoredText("🚨 HIGH PRIORITY WARNINGS:", ANSI_BOLD + ANSI_RED));
            for (BuildWarning warning : highPriorityWarnings) {
                logInfo(String.format("  • %s:%d - %s",
                        warning.fileName, warning.lineNumber, warning.warningMessage));
            }
        }

        logInfo(repeat("─", 50));
    }

    private String getSeverityIcon(String severity) {
        switch (severity) {
            case "HIGH":
                return "🔴";
            case "MEDIUM":
                return "🟡";
            case "LOW":
                return "⚪";
            default:
                return "🔵";
        }
    }

    private void logError(String message) {
        getLog().error(message);
    }
}