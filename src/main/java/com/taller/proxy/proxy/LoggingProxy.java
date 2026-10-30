package com.taller.proxy.proxy;

import com.taller.proxy.model.CallLog;
import com.taller.proxy.model.CallStatus;
import com.taller.proxy.store.LogStore;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy de logging: envuelve cualquier {@link MicroserviceProxy}, registra el inicio,
 * la duracion y el resultado (o el error con stack trace resumido) de cada llamada.
 */
public class LoggingProxy<T> implements MicroserviceProxy<T> {

    private static final Logger log = LoggerFactory.getLogger(LoggingProxy.class);

    private final String serviceId;
    private final MicroserviceProxy<T> target;
    private final LogStore store;

    public LoggingProxy(String serviceId, MicroserviceProxy<T> target, LogStore store) {
        this.serviceId = serviceId;
        this.target = target;
        this.store = store;
    }

    @Override
    public T execute(String operation, Object... params) {
        String requestId = UUID.randomUUID().toString();
        Instant timestamp = Instant.now();
        long start = System.nanoTime();
        log.info("[{}] INICIO {}.{} params={}", requestId, serviceId, operation, Arrays.toString(params));
        try {
            T result = target.execute(operation, params);
            long ms = elapsedMs(start);
            store.add(new CallLog(requestId, serviceId, operation, ms, CallStatus.SUCCESS, timestamp,
                    params.length > 0 ? params[0] : null, result, null));
            log.info("[{}] FIN {}.{} SUCCESS {}ms", requestId, serviceId, operation, ms);
            return result;
        } catch (RuntimeException e) {
            long ms = elapsedMs(start);
            store.add(new CallLog(requestId, serviceId, operation, ms, CallStatus.ERROR, timestamp,
                    params.length > 0 ? params[0] : null, null, summarize(e)));
            log.warn("[{}] FIN {}.{} ERROR {}ms: {}", requestId, serviceId, operation, ms, e.getMessage());
            throw new ProxyCallException(requestId, e);
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    /** Clase + mensaje + primeros frames del stack trace. */
    private static String summarize(Throwable e) {
        StringBuilder sb = new StringBuilder(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
        StackTraceElement[] trace = e.getStackTrace();
        for (int i = 0; i < Math.min(4, trace.length); i++) {
            sb.append("\n  at ").append(trace[i]);
        }
        return sb.toString();
    }
}
