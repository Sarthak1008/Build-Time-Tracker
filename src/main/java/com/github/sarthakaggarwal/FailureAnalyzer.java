package com.github.sarthakaggarwal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files; // Add missing import
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.project.MavenProject;

public class FailureAnalyzer {
    private final boolean enableFailureAnalysis;
    private final List<BuildFailure> buildFailures;
    private final MavenProject project;
    private final int sourceCodeContext;
    private final java.util.Map<String, String> sourceFileCache;
    private final java.util.function.Consumer<String> logError;
    private final java.util.function.Consumer<BuildFailure> printFailureAnalysis;

    public FailureAnalyzer(
            boolean enableFailureAnalysis,
            List<BuildFailure> buildFailures,
            MavenProject project,
            int sourceCodeContext,
            java.util.Map<String, String> sourceFileCache,
            java.util.function.Consumer<String> logError,
            java.util.function.Consumer<BuildFailure> printFailureAnalysis) {
        this.enableFailureAnalysis = enableFailureAnalysis;
        this.buildFailures = buildFailures;
        this.project = project;
        this.sourceCodeContext = sourceCodeContext;
        this.sourceFileCache = sourceFileCache;
        this.logError = logError;
        this.printFailureAnalysis = printFailureAnalysis;
    }

    public void analyzeFailure(ExecutionEvent event) {
        if (!enableFailureAnalysis)
            return;

        try {
            String phase = event.getMojoExecution().getLifecyclePhase();
            Throwable exception = event.getException();

            if (exception != null) {
                BuildFailure failure = extractFailureDetails(phase, exception);
                buildFailures.add(failure);

                logError.accept("🚨 BUILD FAILURE DETECTED in phase: " + phase);
                printFailureAnalysis.accept(failure);
            }
        } catch (Exception e) {
            logError.accept("Error analyzing build failure: " + e.getMessage());
        }
    }

    private BuildFailure extractFailureDetails(String phase, Throwable exception) {
        String errorType = exception.getClass().getSimpleName();
        String errorMessage = exception.getMessage() != null ? exception.getMessage() : "Unknown error";
        String fileName = "";
        int lineNumber = 0;
        String sourceSnippet = "";

        // Extract file and line information from stack trace
        StackTraceElement[] stackTrace = exception.getStackTrace();
        if (stackTrace.length > 0) {
            for (StackTraceElement element : stackTrace) {
                if (isProjectSourceFile(element.getFileName())) {
                    fileName = element.getFileName();
                    lineNumber = element.getLineNumber();
                    sourceSnippet = extractSourceCodeSnippet(fileName, lineNumber);
                    break;
                }
            }
        }

        // Get full stack trace
        String fullStackTrace = getStackTraceAsString(exception);

        BuildFailure failure = new BuildFailure(phase, errorType, errorMessage,
                fileName, lineNumber, sourceSnippet, fullStackTrace);

        // Generate suggested fixes
        generateSuggestedFixes(failure);

        return failure;
    }

    private boolean isProjectSourceFile(String fileName) {
        if (fileName == null)
            return false;
        return fileName.endsWith(".java") || fileName.endsWith(".scala") ||
                fileName.endsWith(".kotlin") || fileName.endsWith(".groovy");
    }

    private String extractSourceCodeSnippet(String fileName, int lineNumber) {
        if (fileName == null || lineNumber <= 0)
            return "";

        try {
            // Try to find the source file in common Maven directories
            List<String> possiblePaths = Arrays.asList(
                    project.getBasedir() + "/src/main/java/" + fileName,
                    project.getBasedir() + "/src/test/java/" + fileName,
                    project.getBasedir() + "/src/main/scala/" + fileName,
                    project.getBasedir() + "/src/test/scala/" + fileName);

            for (String path : possiblePaths) {
                File sourceFile = new File(path);
                if (sourceFile.exists()) {
                    return extractLinesFromFile(sourceFile, lineNumber);
                }
            }

            // Try to find file recursively in src directory
            return findAndExtractFromSourceTree(fileName, lineNumber);

        } catch (Exception e) {
            logError.accept("Could not extract source code snippet: " + e.getMessage());
            return "";
        }
    }

    private String extractLinesFromFile(File sourceFile, int targetLine) throws IOException {
        List<String> lines = Files.readAllLines(sourceFile.toPath());

        if (targetLine > lines.size())
            return "";

        StringBuilder snippet = new StringBuilder();
        int startLine = Math.max(1, targetLine - sourceCodeContext);
        int endLine = Math.min(lines.size(), targetLine + sourceCodeContext);

        for (int i = startLine; i <= endLine; i++) {
            String prefix = (i == targetLine) ? ">>> " : "    ";
            snippet.append(String.format("%s%4d: %s%n", prefix, i, lines.get(i - 1)));
        }

        return snippet.toString();
    }

    private String findAndExtractFromSourceTree(String fileName, int lineNumber) {
        try {
            Files.walk(Paths.get(project.getBasedir().getAbsolutePath(), "src"))
                    .filter(path -> path.toString().endsWith(fileName))
                    .findFirst()
                    .ifPresent(path -> {
                        try {
                            String snippet = extractLinesFromFile(path.toFile(), lineNumber);
                            sourceFileCache.put(fileName, snippet);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });

            return sourceFileCache.getOrDefault(fileName, "");
        } catch (Exception e) {
            return "";
        }
    }

    private void generateSuggestedFixes(BuildFailure failure) {
        String errorMessage = failure.errorMessage.toLowerCase();
        String errorType = failure.errorType.toLowerCase();

        // Common compilation errors
        if (errorType.contains("compilationerror") || errorMessage.contains("cannot find symbol")) {
            failure.suggestedFixes.add("Check if all required dependencies are included in pom.xml");
            failure.suggestedFixes.add("Verify import statements and class names");
            failure.suggestedFixes.add("Ensure target class is in the classpath");
        }

        // Dependency issues
        if (errorMessage.contains("could not resolve dependencies") ||
                errorMessage.contains("artifact not found")) {
            failure.suggestedFixes.add("Check Maven repository availability");
            failure.suggestedFixes.add("Verify dependency coordinates (groupId, artifactId, version)");
            failure.suggestedFixes.add("Try clearing local repository: mvn dependency:purge-local-repository");
        }

        // Test failures
        if (failure.phase.contains("test") && errorType.contains("test")) {
            failure.suggestedFixes.add("Run tests individually to isolate the issue");
            failure.suggestedFixes.add("Check test data and mock configurations");
            failure.suggestedFixes.add("Verify test environment setup");
        }

        // Memory issues
        if (errorMessage.contains("outofmemoryerror") || errorMessage.contains("heap space")) {
            failure.suggestedFixes.add("Increase heap size: -Xmx2g");
            failure.suggestedFixes.add("Enable garbage collection logging");
            failure.suggestedFixes.add("Consider using Maven daemon for builds");
        }

        // Plugin issues
        if (errorMessage.contains("plugin") || errorMessage.contains("mojo")) {
            failure.suggestedFixes.add("Update plugin to latest version");
            failure.suggestedFixes.add("Check plugin configuration in pom.xml");
            failure.suggestedFixes.add("Try running with -X flag for detailed debug output");
        }
    }

    private String getStackTraceAsString(Throwable throwable) {
        StringBuilder result = new StringBuilder();
        result.append(throwable.toString()).append("\n");

        StackTraceElement[] trace = throwable.getStackTrace();
        for (StackTraceElement element : trace) {
            result.append("\tat ").append(element).append("\n");
        }

        Throwable cause = throwable.getCause();
        if (cause != null) {
            result.append("Caused by: ").append(getStackTraceAsString(cause));
        }

        return result.toString();
    }
}
