package com.taller.proxy.controller;

import com.taller.proxy.proxy.ProxyRegistry;
import com.taller.proxy.proxy.ProxyCallException;
import com.taller.proxy.service.NotFoundException;
import com.taller.proxy.service.ServiceUnavailableException;
import com.taller.proxy.service.UnknownOperationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Todas las llamadas a los microservicios pasan por el LoggingProxy. */
@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ProxyRegistry proxies;

    public ServiceController(ProxyRegistry proxies) {
        this.proxies = proxies;
    }

    @PostMapping("/inventory/{operation}")
    public Object inventory(@PathVariable String operation, @RequestBody(required = false) Map<String, Object> body) {
        return proxies.get("inventory").execute(operation, body == null ? Map.of() : body);
    }

    @PostMapping("/orders/{operation}")
    public Object orders(@PathVariable String operation, @RequestBody(required = false) Map<String, Object> body) {
        return proxies.get("orders").execute(operation, body == null ? Map.of() : body);
    }

    @PostMapping("/payments/{operation}")
    public Object payments(@PathVariable String operation, @RequestBody(required = false) Map<String, Object> body) {
        return proxies.get("payments").execute(operation, body == null ? Map.of() : body);
    }

    @ExceptionHandler(ProxyCallException.class)
    public ResponseEntity<Map<String, Object>> handle(ProxyCallException e) {
        Throwable cause = e.getCause();
        HttpStatus status = switch (cause) {
            case NotFoundException ignored -> HttpStatus.NOT_FOUND;
            case UnknownOperationException ignored -> HttpStatus.NOT_FOUND;
            case IllegalArgumentException ignored -> HttpStatus.BAD_REQUEST;
            case IllegalStateException ignored -> HttpStatus.CONFLICT;
            case ServiceUnavailableException ignored -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requestId", e.getRequestId());
        body.put("status", status.value());
        body.put("error", e.getMessage());
        return ResponseEntity.status(status).body(body);
    }
}
