package com.taller.proxy.store;

import com.taller.proxy.model.CallLog;
import com.taller.proxy.model.CallStatus;
import com.taller.proxy.model.PageResult;
import com.taller.proxy.model.ServiceSummary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Almacen en memoria de los logs de llamadas (maximo {@value #MAX_LOGS}). */
@Component
public class LogStore {

    private static final int MAX_LOGS = 5000;

    private final ConcurrentLinkedDeque<CallLog> logs = new ConcurrentLinkedDeque<>();

    public void add(CallLog entry) {
        logs.addFirst(entry);
        while (logs.size() > MAX_LOGS) {
            logs.pollLast();
        }
    }

    public List<ServiceSummary> summary(List<String> serviceIds) {
        List<CallLog> snapshot = new ArrayList<>(logs);
        return serviceIds.stream().map(id -> {
            List<CallLog> mine = snapshot.stream().filter(l -> l.serviceId().equals(id)).toList();
            long total = mine.size();
            long errors = mine.stream().filter(l -> l.status() == CallStatus.ERROR).count();
            long ok = total - errors;
            double avg = mine.stream().mapToLong(CallLog::durationMs).average().orElse(0);
            return new ServiceSummary(id, total, ok, errors,
                    total == 0 ? 0 : round(ok * 100.0 / total),
                    total == 0 ? 0 : round(errors * 100.0 / total),
                    round(avg));
        }).collect(Collectors.toList());
    }

    public PageResult<CallLog> search(String service, CallStatus status, Instant from, Instant to,
                                      int page, int size) {
        List<CallLog> filtered = logs.stream()
                .filter(l -> service == null || service.isBlank() || l.serviceId().equalsIgnoreCase(service))
                .filter(l -> status == null || l.status() == status)
                .filter(l -> from == null || !l.timestamp().isBefore(from))
                .filter(l -> to == null || !l.timestamp().isAfter(to))
                .sorted(Comparator.comparing(CallLog::timestamp).reversed())
                .toList();
        int safeSize = Math.max(1, Math.min(size, 200));
        int safePage = Math.max(0, page);
        int fromIdx = Math.min(safePage * safeSize, filtered.size());
        int toIdx = Math.min(fromIdx + safeSize, filtered.size());
        int totalPages = (int) Math.ceil(filtered.size() / (double) safeSize);
        return new PageResult<>(filtered.subList(fromIdx, toIdx), safePage, safeSize, filtered.size(), totalPages);
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
