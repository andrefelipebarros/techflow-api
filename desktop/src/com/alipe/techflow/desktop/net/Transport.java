package com.alipe.techflow.desktop.net;

/** Meio de transporte das chamadas: HTTP real ou o servidor de demonstração local. */
public interface Transport {

    record Response(int status, Object body) {}

    Response send(String method, String path, String bearerToken, Object body) throws ApiException;

    /** Descrição curta para a barra superior, como "Demo local" ou "http://localhost:8080". */
    String describe();
}
