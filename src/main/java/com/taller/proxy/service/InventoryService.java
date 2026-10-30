package com.taller.proxy.service;

import com.taller.proxy.proxy.MicroserviceProxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/** Servicio real de inventario. Operaciones: list, get, check, reserve, restock. */
@Service
public class InventoryService implements MicroserviceProxy<Object> {

    public record Product(String sku, String name, int stock) {
    }

    private final Map<String, Product> products = new ConcurrentHashMap<>();

    public InventoryService() {
        List.of(new Product("SKU-001", "Laptop", 25),
                new Product("SKU-002", "Mouse", 100),
                new Product("SKU-003", "Teclado", 60),
                new Product("SKU-004", "Monitor", 15),
                new Product("SKU-005", "Auriculares", 40))
                .forEach(p -> products.put(p.sku(), p));
    }

    @Override
    public Object execute(String operation, Object... params) {
        Latency.simulate(10, 60);
        Map<String, Object> p = Params.map(params);
        return switch (operation) {
            case "list" -> new ArrayList<>(products.values().stream()
                    .sorted((a, b) -> a.sku().compareTo(b.sku())).toList());
            case "get" -> find(Params.str(p, "sku"));
            case "check" -> {
                Product prod = find(Params.str(p, "sku"));
                int qty = Params.integer(p, "quantity");
                yield Map.of("sku", prod.sku(), "available", prod.stock() >= qty, "stock", prod.stock());
            }
            case "reserve" -> reserve(Params.str(p, "sku"), Params.integer(p, "quantity"));
            case "restock" -> restock(Params.str(p, "sku"), Params.integer(p, "quantity"));
            default -> throw new UnknownOperationException("InventoryService", operation);
        };
    }

    /** Usado tambien internamente por OrderService. */
    public synchronized Product reserve(String sku, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        Product prod = find(sku);
        if (prod.stock() < quantity) {
            throw new IllegalStateException("Stock insuficiente para " + sku + ": disponible " + prod.stock());
        }
        Product updated = new Product(sku, prod.name(), prod.stock() - quantity);
        products.put(sku, updated);
        return updated;
    }

    public synchronized Product restock(String sku, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        Product prod = find(sku);
        Product updated = new Product(sku, prod.name(), prod.stock() + quantity);
        products.put(sku, updated);
        return updated;
    }

    private Product find(String sku) {
        Product prod = products.get(sku);
        if (prod == null) {
            throw new NotFoundException("Producto no encontrado: " + sku);
        }
        return prod;
    }
}
