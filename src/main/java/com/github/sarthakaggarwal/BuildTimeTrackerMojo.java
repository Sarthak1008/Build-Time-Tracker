package com.github.sarthakaggarwal;

import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maven plugin to track and report build times for different phases.
 */
@Mojo(name = "track", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class BuildTimeTrackerMojo extends AbstractMojo {

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Threshold in milliseconds for warning about long-running phases.
     */
    @Parameter(property = "warnThreshold", defaultValue = "5000")
    private long warnThreshold;

    /**
     * Whether to export timing data to JSON.
     */
    @Parameter(property = "exportJson", defaultValue = "false")
    private boolean exportJson;

    /**
     * Whether to export timing data to CSV.
     */
    @Parameter(property = "exportCsv", defaultValue = "false")
    private boolean exportCsv;

    /**
     * Output directory for reports.
     */
    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}")
    private File outputDirectory;

    private final Map<String, Long> phaseStartTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> phaseDurations = new ConcurrentHashMap<>();
    private long buildStartTime;

    @Override
    public void execute() throws MojoExecutionException {
        buildStartTime = System.currentTimeMillis();
        
        // Register our execution listener
        session.getRequest().setExecutionListener(new BuildTimeExecutionListener());
        
        getLog().info("Build Time Tracker initialized. Tracking build phases...");
    }

    /**
     * Listener to track execution events and record timings.
     */
    private class BuildTimeExecutionListener implements ExecutionListener {
        @Override
        public void projectDiscoveryStarted(ExecutionEvent event) {
        }

        @Override
        public void sessionStarted(ExecutionEvent event) {
        }

        @Override
        public void sessionEnded(ExecutionEvent event) {
            long totalTime = System.currentTimeMillis() - buildStartTime;
            phaseDurations.put("total", totalTime);
            
            // Print summary table
            printSummaryTable();
            
            // Export data if configured
            if (exportJson) {
                exportJson();
            }
            
            if (exportCsv) {
                exportCsv();
            }
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
        public void mojoStarted(ExecutionEvent event) {
            String phase = event.getMojoExecution().getLifecyclePhase();
            if (phase != null && !phase.isEmpty()) {
                phaseStartTimes.putIfAbsent(phase, System.currentTimeMillis());
            }
        }

        @Override
        public void mojoSucceeded(ExecutionEvent event) {
            recordPhaseCompletion(event);
        }

        @Override
        public void mojoFailed(ExecutionEvent event) {
            recordPhaseCompletion(event);
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

        private void recordPhaseCompletion(ExecutionEvent event) {
            String phase = event.getMojoExecution().getLifecyclePhase();
            if (phase != null && !phase.isEmpty() && phaseStartTimes.containsKey(phase)) {
                long startTime = phaseStartTimes.get(phase);
                long duration = System.currentTimeMillis() - startTime;
                
                // Store the duration
                phaseDurations.put(phase, duration);
                
                // Check if we need to warn about long-running phases
                if (duration > warnThreshold) {
                    getLog().warn(String.format("⚠️  Phase \"%s\" took %.1fs (threshold = %.1fs)",
                            phase, duration / 1000.0, warnThreshold / 1000.0));
                }
            }
        }
    }

    /**
     * Prints a formatted summary table of all phase durations.
     */
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

    /**
     * Exports timing data to a JSON file.
     */
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

    /**
     * Exports timing data to a CSV file.
     */
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
}