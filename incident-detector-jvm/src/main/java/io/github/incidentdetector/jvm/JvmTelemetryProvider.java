package io.github.incidentdetector.jvm;

import io.github.incidentdetector.api.TelemetryProvider;
import io.github.incidentdetector.api.TelemetrySnapshot;
import io.github.incidentdetector.core.DefaultTelemetrySnapshot;

import java.lang.management.*;
import java.time.Instant;
import java.util.List;

/**
 * JVM Telemetry Provider using Java ManagementFactory MXBeans.
 */
public class JvmTelemetryProvider implements TelemetryProvider {

    private long lastGcTimeMs = 0;
    private long lastGcCount = 0;
    private Instant lastGcCheckTime = Instant.now();

    @Override
    public String name() {
        return "jvm-telemetry-provider";
    }

    @Override
    public TelemetrySnapshot snapshot() {
        DefaultTelemetrySnapshot.Builder builder = DefaultTelemetrySnapshot.builder().timestamp(Instant.now());

        // Memory MXBean
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        if (heapUsage.getMax() > 0) {
            double util = (double) heapUsage.getUsed() / heapUsage.getMax();
            builder.putMetric("jvm.memory.heap_utilization", util);
            builder.putMetric("jvm.memory.heap_used_bytes", heapUsage.getUsed());
            builder.putMetric("jvm.memory.heap_max_bytes", heapUsage.getMax());
        }

        // Thread MXBean
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        builder.putMetric("executor.active_threads", threadMXBean.getThreadCount());
        builder.putMetric("executor.max_threads", threadMXBean.getPeakThreadCount());

        // GC MXBeans
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long totalGcTime = 0;
        long totalGcCount = 0;
        long fullGcCount = 0;

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            long count = gcBean.getCollectionCount();
            long time = gcBean.getCollectionTime();
            if (count > 0) {
                totalGcCount += count;
            }
            if (time > 0) {
                totalGcTime += time;
            }
            if (gcBean.getName().toLowerCase().contains("old") || gcBean.getName().toLowerCase().contains("full") || gcBean.getName().toLowerCase().contains("marksweep")) {
                fullGcCount += Math.max(0, count);
            }
        }

        Instant now = Instant.now();
        long timeDiffMs = Math.max(1, now.toEpochMilli() - lastGcCheckTime.toEpochMilli());
        long gcCountDelta = Math.max(0, totalGcCount - lastGcCount);
        long gcTimeDelta = Math.max(0, totalGcTime - lastGcTimeMs);

        builder.putMetric("jvm.gc.pause_duration", gcTimeDelta);
        builder.putMetric("jvm.gc.frequency", (double) gcCountDelta / (timeDiffMs / 1000.0));
        builder.putMetric("jvm.gc.full_gc_count", fullGcCount);

        lastGcTimeMs = totalGcTime;
        lastGcCount = totalGcCount;
        lastGcCheckTime = now;

        // CPU OperatingSystemMXBean
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        builder.putMetric("jvm.cpu.system_load_average", osBean.getSystemLoadAverage());

        return builder.build();
    }
}
