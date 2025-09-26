package com.github.sarthakaggarwal;

import java.util.List;

public class WarningAnalyzer {
    private final boolean enableWarningDetection;
    private final List<BuildWarning> buildWarnings;

    public WarningAnalyzer(boolean enableWarningDetection, List<BuildWarning> buildWarnings) {
        this.enableWarningDetection = enableWarningDetection;
        this.buildWarnings = buildWarnings;
    }

    public void analyzeOutput(String output, String phase) {
        if (!enableWarningDetection || output == null) {
            return;
        }

        // Split output into lines
        String[] lines = output.split("\n");
        for (String line : lines) {
            // Capture different types of warnings
            if (isWarningLine(line)) {
                BuildWarning warning = parseWarningFromLine(line, phase);
                if (warning != null) {
                    buildWarnings.add(warning);
                }
            }
        }
    }

    private boolean isWarningLine(String line) {
        String lowerLine = line.toLowerCase();
        
        // Standard Maven warning patterns
        if (line.contains("WARNING:") || line.contains("[WARNING]") || lowerLine.contains("warning")) {
            return true;
        }
        
        // Java compiler warning patterns
        if (lowerLine.contains("note:") && (lowerLine.contains("unused") || lowerLine.contains("deprecated"))) {
            return true;
        }
        
        // Specific compiler warning patterns
        if (lowerLine.matches(".*\\.java:\\d+:.*warning.*") || 
            lowerLine.matches(".*\\.java:\\d+:.*note.*")) {
            return true;
        }
        
        // Unused variable/import patterns
        if (lowerLine.contains("unused") || lowerLine.contains("never read") || 
            lowerLine.contains("never used") || lowerLine.contains("is never used locally")) {
            return true;
        }
        
        return false;
    }

    private BuildWarning parseWarningFromLine(String line, String phase) {
        String message = extractWarningMessage(line);
        String fileName = extractFileName(line);
        int lineNumber = extractLineNumber(line);
        String severity = determineSeverity(message);
        String warningType = categorizeWarning(message);

        return new BuildWarning(
                phase,
                warningType,
                message,
                fileName,
                lineNumber,
                severity);
    }

    private String extractWarningMessage(String line) {
        // Remove prefix like [WARNING] or WARNING:
        if (line.contains("[WARNING]")) {
            return line.substring(line.indexOf("[WARNING]") + 9).trim();
        } else if (line.contains("WARNING:")) {
            return line.substring(line.indexOf("WARNING:") + 8).trim();
        }
        return line.trim();
    }

    private String extractFileName(String line) {
        // Try to find file name patterns
        if (line.contains(".java:") || line.contains(".class:")) {
            String[] parts = line.split(":");
            for (String part : parts) {
                if (part.endsWith(".java") || part.endsWith(".class")) {
                    return part.trim();
                }
            }
        }
        return "";
    }

    private int extractLineNumber(String line) {
        // Try to find line numbers after file names
        if (line.matches(".*\\.(java|class):\\d+.*")) {
            String[] parts = line.split(":");
            for (int i = 0; i < parts.length - 1; i++) {
                if (parts[i].endsWith(".java") || parts[i].endsWith(".class")) {
                    try {
                        return Integer.parseInt(parts[i + 1].trim());
                    } catch (NumberFormatException e) {
                        // Ignore parsing errors
                    }
                }
            }
        }
        return 0;
    }

    private String determineSeverity(String warningMessage) {
        warningMessage = warningMessage.toLowerCase();

        // High severity warnings
        if (warningMessage.contains("critical") ||
                warningMessage.contains("severe") ||
                warningMessage.contains("security") ||
                warningMessage.contains("vulnerability") ||
                warningMessage.contains("will no longer work") ||
                warningMessage.contains("future release")) {
            return "HIGH";
        }

        // Medium severity warnings
        if (warningMessage.contains("important") ||
                warningMessage.contains("deprecated") ||
                warningMessage.contains("unchecked") ||
                warningMessage.contains("raw type") ||
                warningMessage.contains("agent has been loaded") ||
                warningMessage.contains("serviceability tool")) {
            return "MEDIUM";
        }

        // Low severity warnings (code quality issues)
        if (warningMessage.contains("unused") ||
                warningMessage.contains("never read") ||
                warningMessage.contains("never used") ||
                warningMessage.contains("is never used locally") ||
                warningMessage.contains("serialversionuid")) {
            return "LOW";
        }

        // Default to LOW
        return "LOW";
    }

    private String categorizeWarning(String warningMessage) {
        warningMessage = warningMessage.toLowerCase();

        // Java compiler specific warnings
        if (warningMessage.contains("unused") || warningMessage.contains("never read") || 
            warningMessage.contains("never used") || warningMessage.contains("is never used locally")) {
            return "Unused Code";
        }
        if (warningMessage.contains("deprecated") || warningMessage.contains("deprecation")) {
            return "Deprecation";
        }
        if (warningMessage.contains("unchecked") || warningMessage.contains("raw type")) {
            return "Type Safety";
        }
        if (warningMessage.contains("serial") || warningMessage.contains("serialversionuid")) {
            return "Serialization";
        }
        if (warningMessage.contains("override") || warningMessage.contains("overrides")) {
            return "Override";
        }
        
        // Framework specific warnings
        if (warningMessage.contains("mockito"))
            return "Mockito";
        if (warningMessage.contains("java agent") || warningMessage.contains("agent has been loaded"))
            return "Java Agent";
        if (warningMessage.contains("spring") || warningMessage.contains("boot"))
            return "Spring Framework";
        
        // Build system warnings
        if (warningMessage.contains("dependency") || warningMessage.contains("maven"))
            return "Dependency";
        if (warningMessage.contains("compiler") || warningMessage.contains("javac"))
            return "Compiler";
        if (warningMessage.contains("test") || warningMessage.contains("junit"))
            return "Testing";

        return "General";
    }
}
