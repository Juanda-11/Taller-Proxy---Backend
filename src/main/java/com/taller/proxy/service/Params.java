package com.taller.proxy.service;

import java.util.Map;

/** Utilidades para leer los parametros (un Map) que llegan a los servicios. */
final class Params {

    private Params() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object... params) {
        if (params == null || params.length == 0 || params[0] == null) {
            return Map.of();
        }
        if (params[0] instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        throw new IllegalArgumentException("Los parametros deben ser un objeto JSON");
    }

    static String str(Map<String, Object> p, String key) {
        Object v = p.get(key);
        if (v == null || v.toString().isBlank()) {
            throw new IllegalArgumentException("Falta el parametro '" + key + "'");
        }
        return v.toString();
    }

    static int integer(Map<String, Object> p, String key) {
        try {
            return Integer.parseInt(str(p, key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El parametro '" + key + "' debe ser un entero");
        }
    }

    static double decimal(Map<String, Object> p, String key) {
        try {
            return Double.parseDouble(str(p, key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El parametro '" + key + "' debe ser numerico");
        }
    }
}
