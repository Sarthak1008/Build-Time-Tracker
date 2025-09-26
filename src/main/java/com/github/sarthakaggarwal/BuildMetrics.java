package com.github.sarthakaggarwal;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents build metrics collected during a Maven build.
 */
public class BuildMetrics {
    public final LocalDateTime timestamp;
    public final long totalTime;
    public final Map<String, Long> phaseTimes;
    public final SystemMetrics systemMetrics;

    public BuildMetrics(LocalDateTime timestamp, long totalTime, 
                       Map<String, Long> phaseTimes, SystemMetrics systemMetrics) {
        this.timestamp = timestamp;
        this.totalTime = totalTime;
        this.phaseTimes = phaseTimes;
        this.systemMetrics = systemMetrics;
    }
}
