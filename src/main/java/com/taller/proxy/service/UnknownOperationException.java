package com.taller.proxy.service;

public class UnknownOperationException extends RuntimeException {
    public UnknownOperationException(String service, String operation) {
        super("Operacion '" + operation + "' no soportada por " + service);
    }
}
