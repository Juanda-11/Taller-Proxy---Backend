package com.taller.proxy.model;

import java.time.Instant;

public record CallLog(
        String requestId,
        String serviceId,
        String operation,
        long durationMs,
        CallStatus status,
        Instant timestamp,
        Object params,
        Object response,
        String error) {
}
