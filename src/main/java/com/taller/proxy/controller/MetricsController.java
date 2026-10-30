package com.taller.proxy.controller;

import com.taller.proxy.model.CallLog;
import com.taller.proxy.model.CallStatus;
import com.taller.proxy.model.PageResult;
import com.taller.proxy.model.ServiceSummary;
import com.taller.proxy.proxy.MicroserviceProxy;
import com.taller.proxy.proxy.ProxyRegistry;
import com.taller.proxy.service.InventoryService;
import com.taller.proxy.service.OrderService;
import com.taller.proxy.service.PaymentService;
import com.taller.proxy.store.LogStore;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private static final int SIMULATED_CALLS = 50;
    private static final List<String> SKUS = List.of("SKU-001", "SKU-002", "SKU-003", "SKU-004", "SKU-005");
    private static final List<String> CUSTOMERS = List.of("Ana", "Luis", "Marta", "Carlos", "Sofia");

    private final LogStore store;
    private final ProxyRegistry proxies;
    private final OrderService orderService;
    private final PaymentService paymentService;

    public MetricsController(LogStore store, ProxyRegistry proxies,
                             OrderService orderService, PaymentService paymentService) {
        this.store = store;
        this.proxies = proxies;
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @GetMapping("/summary")
    public List<ServiceSummary> summary() {
        return store.summary(proxies.serviceIds());
    }

    @GetMapping("/logs")
    public PageResult<CallLog> logs(@RequestParam(required = false) String service,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String from,
                                    @RequestParam(required = false) String to,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        CallStatus callStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                callStatus = CallStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status debe ser SUCCESS o ERROR");
            }
        }
        return store.search(service, callStatus, parse(from), parse(to), page, size);
    }

    @PostMapping("/simulate-load")
    public Map<String, Object> simulateLoad() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int failed = 0;
        for (int i = 0; i < SIMULATED_CALLS; i++) {
            try {
                switch (rnd.nextInt(3)) {
                    case 0 -> inventoryCall(rnd);
                    case 1 -> orderCall(rnd);
                    default -> paymentCall(rnd);
                }
            } catch (RuntimeException e) {
                failed++; // el proxy ya registro el error
            }
        }
        return Map.of("generated", SIMULATED_CALLS, "failed", failed);
    }

    private void inventoryCall(ThreadLocalRandom rnd) {
        MicroserviceProxy<Object> proxy = proxies.get("inventory");
        String sku = pick(SKUS, rnd);
        switch (rnd.nextInt(4)) {
            case 0 -> proxy.execute("list", Map.of());
            case 1 -> proxy.execute("get", Map.of("sku", sku));
            case 2 -> proxy.execute("check", Map.of("sku", sku, "quantity", rnd.nextInt(1, 10)));
            default -> proxy.execute("restock", Map.of("sku", sku, "quantity", rnd.nextInt(1, 20)));
        }
    }

    private void orderCall(ThreadLocalRandom rnd) {
        MicroserviceProxy<Object> proxy = proxies.get("orders");
        List<String> ids = orderService.orderIds();
        switch (ids.isEmpty() ? 0 : rnd.nextInt(3)) {
            case 0 -> proxy.execute("create", Map.of("customer", pick(CUSTOMERS, rnd),
                    "sku", pick(SKUS, rnd), "quantity", rnd.nextInt(1, 6)));
            case 1 -> proxy.execute("list", Map.of());
            default -> proxy.execute("get", Map.of("orderId", pick(ids, rnd)));
        }
    }

    private void paymentCall(ThreadLocalRandom rnd) {
        MicroserviceProxy<Object> proxy = proxies.get("payments");
        List<String> ids = paymentService.paymentIds();
        if (ids.isEmpty() || rnd.nextInt(3) > 0) {
            proxy.execute("charge", Map.of("orderId", "ORD-" + rnd.nextInt(1000, 1100),
                    "amount", Math.round(rnd.nextDouble(10, 500) * 100) / 100.0));
        } else {
            proxy.execute("status", Map.of("paymentId", pick(ids, rnd)));
        }
    }

    private static <T> T pick(List<T> list, ThreadLocalRandom rnd) {
        return list.get(rnd.nextInt(list.size()));
    }

    /** Acepta instantes ISO-8601 (con zona) o fechas-hora locales sin zona (tomadas como UTC). */
    private static Instant parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException e2) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fecha invalida: " + value);
            }
        }
    }
}
