package com.alipe.techflow.desktop.net;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Fala com a TechFlow API de verdade, por HTTP. */
public final class HttpTransport implements Transport {
    private final String baseUrl;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public HttpTransport(String baseUrl) {
        String url = baseUrl == null ? "" : baseUrl.trim();
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        this.baseUrl = url;
    }

    @Override
    public Response send(String method, String path, String bearerToken, Object body) {
        HttpRequest.Builder b;
        try {
            b = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(15)).header("Accept", "application/json");
        } catch (IllegalArgumentException e) {
            throw new ApiException(0, "Endereço da API inválido: " + baseUrl);
        }
        if (bearerToken != null) b.header("Authorization", "Bearer " + bearerToken);
        if (body != null) {
            b.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(Json.stringify(body)));
        } else {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        }
        try {
            HttpResponse<String> res = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(res.statusCode(), parseBody(res.body()));
        } catch (IOException e) {
            throw new ApiException(0, "Não foi possível conectar em " + baseUrl
                    + ". Confira o endereço e se a API está no ar.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "Chamada interrompida.");
        }
    }

    private static Object parseBody(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Json.parse(raw);
        } catch (IllegalArgumentException e) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("message", raw.length() > 300 ? raw.substring(0, 300) : raw);
            return m;
        }
    }

    @Override public String describe() { return baseUrl; }
}
