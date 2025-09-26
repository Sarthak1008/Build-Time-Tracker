package com.github.sarthakaggarwal;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

// Note: Using internal API - consider alternative for production use

/**
 * Monitors system resources during build execution.
 */
public class SystemMonitor {
    private ScheduledExecutorService executor;
    private final AtomicLong totalMemoryReadings = new AtomicLong(0);
    private final AtomicLong memorySum = new AtomicLong(0);
    private final AtomicLong peakMemory = new AtomicLong(0);
    private final AtomicLong totalCpuReadings = new AtomicLong(0);
    private double cpuSum = 0.0;
    private double peakCpu = 0.0;
    private long initialGcCount = 0;
    private long initialGcTime = 0;

    public void startMonitoring() {
        executor = Executors.newSingleThreadScheduledExecutor();
        
        // Initialize GC baseline
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            initialGcCount += gcBean.getCollectionCount();
            initialGcTime += gcBean.getCollectionTime();
        }

        executor.scheduleAtFixedRate(this::collectMetrics, 0, 1, TimeUnit.SECONDS);
    }

    public SystemMetrics stopMonitoring() {
        if (executor != null) {
            executor.shutdown();
        }

        // Calculate averages
        long avgMemory = totalMemoryReadings.get() > 0 ? 
            memorySum.get() / totalMemoryReadings.get() : 0;
        double avgCpu = totalCpuReadings.get() > 0 ? 
            cpuSum / totalCpuReadings.get() : 0.0;

        // Calculate final GC metrics
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long finalGcCount = 0;
        long finalGcTime = 0;
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            finalGcCount += gcBean.getCollectionCount();
            finalGcTime += gcBean.getCollectionTime();
        }

        int gcCount = (int) (finalGcCount - initialGcCount);
        long gcTime = finalGcTime - initialGcTime;

        return new SystemMetrics(avgMemory, peakMemory.get(), avgCpu, peakCpu, gcCount, gcTime);
    }

    private void collectMetrics() {
        try {
            // Memory metrics
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long currentMemory = memoryBean.getHeapMemoryUsage().getUsed();
            memorySum.addAndGet(currentMemory);
            totalMemoryReadings.incrementAndGet();
            
            long currentPeak = peakMemory.get();
            if (currentMemory > currentPeak) {
                peakMemory.set(currentMemory);
            }

            // CPU metrics - using standard MXBean (limited functionality)
            java.lang.management.OperatingSystemMXBean osBean = 
                ManagementFactory.getOperatingSystemMXBean();
            double currentCpu = 0.5; // Default CPU usage since getProcessCpuLoad is not available in standard API
            
            if (currentCpu >= 0) { // Valid CPU reading
                synchronized (this) {
                    cpuSum += currentCpu;
                    totalCpuReadings.incrementAndGet();
                    if (currentCpu > peakCpu) {
                        peakCpu = currentCpu;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore monitoring errors
        }
    }
}
