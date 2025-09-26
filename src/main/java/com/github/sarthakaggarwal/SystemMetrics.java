package com.github.sarthakaggarwal;

/**
 * Represents system resource metrics collected during build.
 */
public class SystemMetrics {
    public final long avgMemoryUsage;
    public final long peakMemoryUsage;
    public final double avgCpuUsage;
    public final double peakCpuUsage;
    public final int gcCount;
    public final long gcTime;

    public SystemMetrics(long avgMemoryUsage, long peakMemoryUsage, 
                        double avgCpuUsage, double peakCpuUsage, 
                        int gcCount, long gcTime) {
        this.avgMemoryUsage = avgMemoryUsage;
        this.peakMemoryUsage = peakMemoryUsage;
        this.avgCpuUsage = avgCpuUsage;
        this.peakCpuUsage = peakCpuUsage;
        this.gcCount = gcCount;
        this.gcTime = gcTime;
    }
}
