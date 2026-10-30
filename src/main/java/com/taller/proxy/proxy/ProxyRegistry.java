package com.taller.proxy.proxy;

import java.util.List;
import java.util.Map;

/** Registro de los proxies por identificador de servicio. */
public class ProxyRegistry {

    private final Map<String, MicroserviceProxy<Object>> proxies;

    public ProxyRegistry(Map<String, MicroserviceProxy<Object>> proxies) {
        this.proxies = proxies;
    }

    public MicroserviceProxy<Object> get(String serviceId) {
        return proxies.get(serviceId);
    }

    public List<String> serviceIds() {
        return List.copyOf(proxies.keySet());
    }
}
