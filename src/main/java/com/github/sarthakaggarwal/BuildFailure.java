package com.github.sarthakaggarwal;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a build failure with context information.
 */
public class BuildFailure {
    public final String phase;
    public final String errorType;
    public final String errorMessage;
    public final String fileName;
    public final int lineNumber;
    public final String sourceCodeSnippet;
    public final String fullStackTrace;
    public final List<String> suggestedFixes = new ArrayList<>();

    public BuildFailure(String phase, String errorType, String errorMessage, String fileName, int lineNumber,
            String sourceCodeSnippet, String fullStackTrace) {
        this.phase = phase;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.sourceCodeSnippet = sourceCodeSnippet;
        this.fullStackTrace = fullStackTrace;
    }
}
