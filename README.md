# Monitor de Microservicios — Backend (Patrón Proxy)

Spring Boot 3 / Java 21. Tres microservicios (Inventario, Pedidos, Pagos) detrás de un **LoggingProxy** que audita cada llamada.

## Patrón Proxy
- `MicroserviceProxy<T>` — interfaz genérica con `T execute(String operation, Object... params)`.
- `InventoryService`, `OrderService`, `PaymentService` — servicios reales (implementan la interfaz).
- `LoggingProxy<T>` — implementa la misma interfaz y envuelve a cualquier servicio: registra inicio, duración, resultado y, si falla, el error con stack trace resumido. Cada log tiene `serviceId, operation, durationMs, status (SUCCESS/ERROR), requestId (UUID), timestamp`.
- `PaymentService` falla intencionalmente el 10 % de las llamadas (`payment.failure-rate`).

## Endpoints
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/services/inventory/{operation}` | `list`, `get{sku}`, `check{sku,quantity}`, `reserve{sku,quantity}`, `restock{sku,quantity}` |
| POST | `/api/services/orders/{operation}` | `create{customer,sku,quantity}`, `get{orderId}`, `list`, `cancel{orderId}` |
| POST | `/api/services/payments/{operation}` | `charge{orderId,amount}`, `status{paymentId}`, `refund{paymentId}` |
| GET | `/api/metrics/summary` | Total de llamadas, tasa de éxito/error y tiempo promedio por servicio |
| GET | `/api/metrics/logs?service=&status=&from=&to=&page=&size=` | Logs filtrados y paginados (más recientes primero) |
| POST | `/api/metrics/simulate-load` | Genera 50 llamadas aleatorias |

El cuerpo de las operaciones es un JSON con los parámetros, p. ej. `{"sku":"SKU-001"}`.

## Ejecutar
```bash
mvn spring-boot:run      # http://localhost:8080
mvn test
```

## Despliegue (Railway)
Se despliega con el `Dockerfile` incluido. Variables: `PORT` (la inyecta Railway) y opcional `CORS_ALLOWED_ORIGINS` (URL del frontend en Vercel, por defecto `*`).
