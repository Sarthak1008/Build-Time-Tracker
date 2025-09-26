package com.github.sarthakaggarwal;

public class BuildWarning {
    public final String phase;
    public final String warningType;
    public final String warningMessage;
    public final String fileName;
    public final int lineNumber;
    public final String severity;

    public BuildWarning(String phase, String warningType, String warningMessage, String fileName, int lineNumber,
            String severity) {
        this.phase = phase;
        this.warningType = warningType;
        this.warningMessage = warningMessage;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.severity = severity;
    }
}
