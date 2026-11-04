package com.alipe.techflow.desktop.net;

public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    /** Código HTTP, ou 0 quando nem chegou a haver resposta. */
    public int status() { return status; }
}
