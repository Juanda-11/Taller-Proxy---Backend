package com.taller.proxy.proxy;

/**
 * Interfaz comun del patron Proxy: la implementan tanto los servicios reales
 * como el {@link LoggingProxy} que los envuelve.
 */
public interface MicroserviceProxy<T> {

    T execute(String operation, Object... params);
}
