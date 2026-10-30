package com.taller.proxy.service;

import com.taller.proxy.proxy.MicroserviceProxy;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servicio real de pagos. Operaciones: charge, status, refund.
 * El 10% de las llamadas (configurable) falla intencionalmente.
 */
@Service
public class PaymentService implements MicroserviceProxy<Object> {

    public record Payment(String paymentId, String orderId, double amount, String status, Instant createdAt) {
    }

    private final Map<String, Payment> payments = new ConcurrentHashMap<>();
    private final double failureRate;

    public PaymentService(@Value("${payment.failure-rate:0.10}") double failureRate) {
        this.failureRate = failureRate;
    }

    @Override
    public Object execute(String operation, Object... params) {
        Latency.simulate(60, 250);
        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new ServiceUnavailableException("Falla simulada: pasarela de pagos no disponible");
        }
        Map<String, Object> p = Params.map(params);
        return switch (operation) {
            case "charge" -> charge(Params.str(p, "orderId"), Params.decimal(p, "amount"));
            case "status" -> find(Params.str(p, "paymentId"));
            case "refund" -> refund(Params.str(p, "paymentId"));
            default -> throw new UnknownOperationException("PaymentService", operation);
        };
    }

    private Payment charge(String orderId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero");
        }
        Payment payment = new Payment("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                orderId, amount, "APPROVED", Instant.now());
        payments.put(payment.paymentId(), payment);
        return payment;
    }

    private Payment refund(String paymentId) {
        Payment payment = find(paymentId);
        if ("REFUNDED".equals(payment.status())) {
            throw new IllegalStateException("El pago " + paymentId + " ya fue reembolsado");
        }
        Payment refunded = new Payment(payment.paymentId(), payment.orderId(), payment.amount(),
                "REFUNDED", payment.createdAt());
        payments.put(paymentId, refunded);
        return refunded;
    }

    private Payment find(String paymentId) {
        Payment payment = payments.get(paymentId);
        if (payment == null) {
            throw new NotFoundException("Pago no encontrado: " + paymentId);
        }
        return payment;
    }

    public java.util.List<String> paymentIds() {
        return new java.util.ArrayList<>(payments.keySet());
    }
}
