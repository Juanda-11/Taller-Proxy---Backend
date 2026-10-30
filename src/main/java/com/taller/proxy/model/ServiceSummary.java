package com.taller.proxy.model;

public record ServiceSummary(
        String serviceId,
        long totalCalls,
        long successCount,
        long errorCount,
        double successRate,
        double errorRate,
        double avgDurationMs) {
}
