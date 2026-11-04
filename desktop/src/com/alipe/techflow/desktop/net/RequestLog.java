package com.alipe.techflow.desktop.net;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Histórico das chamadas, mostrado no console da tela principal. */
public final class RequestLog {

    public record Entry(LocalTime time, String method, String path, int status, long millis,
                        String request, String response) {}

    private final List<Entry> entries = new ArrayList<>();
    private final List<Consumer<Entry>> listeners = new CopyOnWriteArrayList<>();
    private Runnable onClear = () -> {};

    public synchronized List<Entry> all() { return new ArrayList<>(entries); }

    public void listen(Consumer<Entry> listener) { listeners.add(listener); }

    public void stopListening(Consumer<Entry> listener) { listeners.remove(listener); }

    public void onClear(Runnable r) { this.onClear = r; }

    public synchronized void clear() {
        entries.clear();
        onClear.run();
    }

    public void add(String method, String path, int status, long millis, Object request, Object response) {
        Entry e = new Entry(LocalTime.now().withNano(0), method, path, status, millis,
                request == null ? "" : Json.pretty(redact(request)),
                response == null ? "" : Json.pretty(redact(response)));
        synchronized (this) {
            entries.add(e);
            if (entries.size() > 300) entries.remove(0);
        }
        listeners.forEach(l -> l.accept(e));
    }

    /** Esconde senhas e encurta tokens antes de guardar no console. */
    @SuppressWarnings("unchecked")
    private static Object redact(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                String key = String.valueOf(e.getKey());
                Object v = e.getValue();
                if (key.equals("password")) v = "********";
                else if (key.equals("accessToken") && v instanceof String t) v = t.length() > 16 ? t.substring(0, 16) + "..." : t;
                else v = redact(v);
                copy.put(key, v);
            }
            return copy;
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            for (Object o : list) copy.add(redact(o));
            return copy;
        }
        return value;
    }
}
