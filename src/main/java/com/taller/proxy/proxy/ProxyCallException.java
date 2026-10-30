package com.taller.proxy.proxy;

/** Error lanzado por el proxy tras registrar la falla; conserva el requestId del log. */
public class ProxyCallException extends RuntimeException {

    private final String requestId;

    public ProxyCallException(String requestId, Throwable cause) {
        super(cause.getMessage(), cause);
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }
}
