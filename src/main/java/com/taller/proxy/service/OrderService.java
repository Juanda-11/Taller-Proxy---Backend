package com.taller.proxy.service;

import com.taller.proxy.proxy.MicroserviceProxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

/** Servicio real de pedidos. Operaciones: create, get, list, cancel. */
@Service
public class OrderService implements MicroserviceProxy<Object> {

    public record Order(String orderId, String customer, String sku, int quantity, String status, Instant createdAt) {
    }

    private final InventoryService inventory;
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(1000);

    public OrderService(InventoryService inventory) {
        this.inventory = inventory;
    }

    @Override
    public Object execute(String operation, Object... params) {
        Latency.simulate(30, 120);
        Map<String, Object> p = Params.map(params);
        return switch (operation) {
            case "create" -> create(Params.str(p, "customer"), Params.str(p, "sku"), Params.integer(p, "quantity"));
            case "get" -> find(Params.str(p, "orderId"));
            case "list" -> new ArrayList<>(orders.values().stream()
                    .sorted((a, b) -> b.createdAt().compareTo(a.createdAt())).limit(50).toList());
            case "cancel" -> cancel(Params.str(p, "orderId"));
            default -> throw new UnknownOperationException("OrderService", operation);
        };
    }

    private Order create(String customer, String sku, int quantity) {
        inventory.reserve(sku, quantity);
        Order order = new Order("ORD-" + sequence.incrementAndGet(), customer, sku, quantity, "CREATED", Instant.now());
        orders.put(order.orderId(), order);
        return order;
    }

    private Order cancel(String orderId) {
        Order order = find(orderId);
        if ("CANCELLED".equals(order.status())) {
            throw new IllegalStateException("El pedido " + orderId + " ya esta cancelado");
        }
        inventory.restock(order.sku(), order.quantity());
        Order cancelled = new Order(order.orderId(), order.customer(), order.sku(), order.quantity(),
                "CANCELLED", order.createdAt());
        orders.put(orderId, cancelled);
        return cancelled;
    }

    private Order find(String orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            throw new NotFoundException("Pedido no encontrado: " + orderId);
        }
        return order;
    }

    public List<String> orderIds() {
        return new ArrayList<>(orders.keySet());
    }
}
