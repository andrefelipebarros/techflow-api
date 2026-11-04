package com.alipe.techflow.desktop.net;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servidor de demonstração que roda dentro do aplicativo. Responde com os mesmos
 * formatos da TechFlow API (status HTTP, paginação e corpo de erro) e guarda os
 * dados em um arquivo na pasta do usuário.
 */
public final class DemoTransport implements Transport {
    public static final String DEMO_EMAIL = "demo@techflow.dev";
    public static final String DEMO_PASSWORD = "Senha@123";

    private static final List<String> PROJECT_STATUS = List.of("PLANNED", "IN_PROGRESS", "COMPLETED", "ARCHIVED");
    private static final List<String> TASK_STATUS = List.of("TODO", "IN_PROGRESS", "DONE", "CANCELED");
    private static final List<String> PRIORITY = List.of("LOW", "MEDIUM", "HIGH", "URGENT");

    private static final Pattern PROJECT = Pattern.compile("^/api/v1/projects/(\\d+)$");
    private static final Pattern PROJECT_TASKS = Pattern.compile("^/api/v1/projects/(\\d+)/tasks$");
    private static final Pattern TASK = Pattern.compile("^/api/v1/tasks/(\\d+)$");
    private static final Pattern TASK_STATUS_PATH = Pattern.compile("^/api/v1/tasks/(\\d+)/status$");

    private final Path file;
    private Map<String, Object> db;

    public DemoTransport() {
        this(Path.of(System.getProperty("user.home"), ".techflow-desktop", "demo.json"));
    }

    public DemoTransport(Path file) {
        this.file = file;
        load();
    }

    @Override public String describe() { return "Demo local"; }

    /** Apaga os dados e volta ao exemplo inicial. */
    public synchronized void reset() {
        db = seed();
        persist();
    }

    // ---------- armazenamento ----------

    @SuppressWarnings("unchecked")
    private void load() {
        try {
            if (Files.exists(file)) {
                db = (Map<String, Object>) Json.parse(Files.readString(file, StandardCharsets.UTF_8));
                return;
            }
        } catch (IOException | RuntimeException ignored) {
            // arquivo ausente ou corrompido: começa do exemplo
        }
        db = seed();
        persist();
    }

    private void persist() {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, Json.stringify(db), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // sem permissão de escrita: o app segue funcionando só em memória
        }
    }

    private static String now() { return OffsetDateTime.now().toString(); }

    private static String day(int offset) { return LocalDate.now().plusDays(offset).toString(); }

    private static String hash(String text) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(("techflow:" + text).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private Map<String, Object> seed() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("seq", map("user", 1L, "project", 2L, "task", 9L));
        List<Object> users = new ArrayList<>();
        users.add(map("id", 1L, "name", "Conta demo", "email", DEMO_EMAIL, "passwordHash", hash(DEMO_PASSWORD), "role", "USER", "createdAt", now()));
        data.put("users", users);
        List<Object> projects = new ArrayList<>();
        projects.add(map("id", 1L, "name", "Lançamento do app", "description", "Tudo o que falta para publicar a primeira versão do aplicativo.", "status", "IN_PROGRESS", "ownerId", 1L, "createdAt", now()));
        projects.add(map("id", 2L, "name", "Estudos de Java", "description", "Plano de estudos para a vaga de desenvolvedor backend júnior.", "status", "PLANNED", "ownerId", 1L, "createdAt", now()));
        data.put("projects", projects);
        List<Object> tasks = new ArrayList<>();
        tasks.add(task(1, 1, "Definir os endpoints de autenticação", "Cadastro e login com JWT.", "DONE", "HIGH", day(-6)));
        tasks.add(task(2, 1, "Configurar o banco com Flyway", "Migrations versionadas em vez de ddl-auto.", "DONE", "MEDIUM", day(-4)));
        tasks.add(task(3, 1, "Cache da consulta de projeto no Redis", "Invalidar ao atualizar ou remover.", "IN_PROGRESS", "HIGH", day(2)));
        tasks.add(task(4, 1, "Escrever testes de integração", "Usar Testcontainers para PostgreSQL e Redis.", "TODO", "URGENT", day(-1)));
        tasks.add(task(5, 1, "Publicar a imagem Docker", "Pipeline no GitHub Actions até o GHCR.", "TODO", "MEDIUM", day(7)));
        tasks.add(task(6, 1, "Filtros por status e prioridade", null, "TODO", "LOW", null));
        tasks.add(task(7, 1, "Integração com o Kafka nos testes", "Deixou de ser prioridade.", "CANCELED", "LOW", null));
        tasks.add(task(8, 2, "Revisar Streams e Optionals", null, "IN_PROGRESS", "MEDIUM", day(3)));
        tasks.add(task(9, 2, "Ler sobre transações no Spring", "Propagação e isolamento.", "TODO", "HIGH", day(10)));
        data.put("tasks", tasks);
        return data;
    }

    private static Map<String, Object> task(long id, long projectId, String title, String desc, String status, String prio, String due) {
        return map("id", id, "projectId", projectId, "title", title, "description", desc, "status", status,
                "priority", prio, "dueDate", due, "createdAt", now());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> table(String name) { return (List<Map<String, Object>>) (List<?>) db.get(name); }

    @SuppressWarnings("unchecked")
    private long nextId(String seq) {
        Map<String, Object> s = (Map<String, Object>) db.get("seq");
        long id = ((Number) s.get(seq)).longValue() + 1;
        s.put(seq, id);
        return id;
    }

    // ---------- servidor ----------

    private static Response ok(Object body) { return new Response(200, body); }

    private static Response fail(int status, String message, String path) {
        String reason = switch (status) {
            case 400 -> "Bad Request"; case 401 -> "Unauthorized"; case 403 -> "Forbidden"; case 404 -> "Not Found";
            default -> "Error";
        };
        return new Response(status, map("timestamp", now(), "status", (long) status, "error", reason, "message", message, "path", path));
    }

    private static String text(Object v) { return v instanceof String s ? s.trim() : ""; }

    private static Map<String, Object> page(List<Map<String, Object>> items, Map<String, String> params) {
        int size = Math.min(Math.max(parseInt(params.get("size"), 20), 1), 100);
        int number = Math.max(parseInt(params.get("page"), 0), 0);
        int from = Math.min(number * size, items.size());
        int to = Math.min(from + size, items.size());
        return map("content", new ArrayList<Object>(items.subList(from, to)), "totalElements", (long) items.size(),
                "totalPages", (long) Math.max((items.size() + size - 1) / size, 1), "number", (long) number, "size", (long) size);
    }

    private static int parseInt(String v, int def) {
        try { return v == null ? def : Integer.parseInt(v); } catch (NumberFormatException e) { return def; }
    }

    private static Map<String, String> query(String q) {
        Map<String, String> m = new LinkedHashMap<>();
        if (q == null || q.isEmpty()) return m;
        for (String pair : q.split("&")) {
            String[] kv = pair.split("=", 2);
            m.put(kv[0], kv.length > 1 ? kv[1] : "");
        }
        return m;
    }

    private String makeToken(Map<String, Object> user) {
        Map<String, Object> payload = map("sub", user.get("email"), "uid", user.get("id"), "roles", List.of(user.get("role")),
                "iss", "techflow-api", "exp", Instant.now().getEpochSecond() + 3600);
        String body = Base64.getUrlEncoder().withoutPadding().encodeToString(Json.stringify(payload).getBytes(StandardCharsets.UTF_8));
        return "demo." + body + ".assinatura-demo";
    }

    private Map<String, Object> readToken(String token) {
        try {
            String[] parts = token.split("\\.");
            Map<?, ?> payload = (Map<?, ?>) Json.parse(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
            if (((Number) payload.get("exp")).longValue() < Instant.now().getEpochSecond()) return null;
            long uid = ((Number) payload.get("uid")).longValue();
            return table("users").stream().filter(u -> ((Number) u.get("id")).longValue() == uid).findFirst().orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Response authResponse(Map<String, Object> user, int status) {
        return new Response(status, map("accessToken", makeToken(user), "tokenType", "Bearer", "expiresInSeconds", 3600L));
    }

    private static Map<String, Object> publicUser(Map<String, Object> u) {
        return map("id", u.get("id"), "name", u.get("name"), "email", u.get("email"), "role", u.get("role"), "createdAt", u.get("createdAt"));
    }

    private static Map<String, Object> projectOut(Map<String, Object> p) {
        Map<String, Object> m = new LinkedHashMap<>(p);
        if (m.get("description") == null) m.remove("description");
        return m;
    }

    private static Map<String, Object> taskOut(Map<String, Object> t) {
        Map<String, Object> m = new LinkedHashMap<>(t);
        m.values().removeIf(v -> v == null);
        return m;
    }

    private static List<String> validateCredentials(Map<?, ?> b, boolean withName) {
        List<String> errs = new ArrayList<>();
        if (withName) {
            if (text(b.get("name")).isEmpty()) errs.add("name: não deve estar em branco");
            else if (((String) b.get("name")).length() > 120) errs.add("name: tamanho deve ser no máximo 120");
        }
        String email = text(b.get("email"));
        if (email.isEmpty()) errs.add("email: não deve estar em branco");
        else if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) errs.add("email: deve ser um endereço de e-mail bem formado");
        Object pw = b.get("password");
        if (!(pw instanceof String p) || p.isEmpty()) errs.add("password: não deve estar em branco");
        else if (withName && (p.length() < 8 || p.length() > 72)) errs.add("password: tamanho deve ser entre 8 e 72");
        return errs;
    }

    private static List<String> validateProject(Map<?, ?> b) {
        List<String> errs = new ArrayList<>();
        if (text(b.get("name")).isEmpty()) errs.add("name: não deve estar em branco");
        else if (((String) b.get("name")).length() > 150) errs.add("name: tamanho deve ser no máximo 150");
        if (b.get("description") instanceof String d && d.length() > 1000) errs.add("description: tamanho deve ser no máximo 1000");
        if (!PROJECT_STATUS.contains(b.get("status"))) errs.add("status: não deve ser nulo");
        return errs;
    }

    private static List<String> validateTask(Map<?, ?> b) {
        List<String> errs = new ArrayList<>();
        if (text(b.get("title")).isEmpty()) errs.add("title: não deve estar em branco");
        else if (((String) b.get("title")).length() > 180) errs.add("title: tamanho deve ser no máximo 180");
        if (b.get("description") instanceof String d && d.length() > 2000) errs.add("description: tamanho deve ser no máximo 2000");
        if (!PRIORITY.contains(b.get("priority"))) errs.add("priority: não deve ser nulo");
        if (b.get("dueDate") instanceof String d && !d.isBlank() && !d.matches("\\d{4}-\\d{2}-\\d{2}")) errs.add("dueDate: formato inválido");
        return errs;
    }

    private static String blankToNull(Object v) {
        String t = text(v);
        return t.isEmpty() ? null : t;
    }

    @Override
    public synchronized Response send(String method, String rawPath, String bearerToken, Object rawBody) {
        try {
            Thread.sleep(25 + (long) (Math.random() * 60));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String[] split = rawPath.split("\\?", 2);
        String path = split[0];
        Map<String, String> params = query(split.length > 1 ? split[1] : null);
        Map<?, ?> body = rawBody instanceof Map<?, ?> m ? m : Map.of();

        if (method.equals("GET") && path.equals("/actuator/health")) return ok(map("status", "UP"));

        if (method.equals("POST") && path.equals("/api/v1/auth/register")) {
            List<String> errs = validateCredentials(body, true);
            if (!errs.isEmpty()) return fail(400, String.join("; ", errs), path);
            String email = text(body.get("email")).toLowerCase();
            if (table("users").stream().anyMatch(u -> ((String) u.get("email")).equalsIgnoreCase(email)))
                return fail(400, "E-mail já cadastrado", path);
            Map<String, Object> user = map("id", nextId("user"), "name", text(body.get("name")), "email", email,
                    "passwordHash", hash((String) body.get("password")), "role", "USER", "createdAt", now());
            table("users").add(user);
            persist();
            return authResponse(user, 201);
        }
        if (method.equals("POST") && path.equals("/api/v1/auth/login")) {
            List<String> errs = validateCredentials(body, false);
            if (!errs.isEmpty()) return fail(400, String.join("; ", errs), path);
            String email = text(body.get("email"));
            Map<String, Object> user = table("users").stream().filter(u -> ((String) u.get("email")).equalsIgnoreCase(email)).findFirst().orElse(null);
            if (user == null || !user.get("passwordHash").equals(hash((String) body.get("password"))))
                return fail(401, "Credenciais inválidas", path);
            return authResponse(user, 200);
        }

        Map<String, Object> me = bearerToken == null ? null : readToken(bearerToken);
        if (me == null) return fail(401, "Token ausente ou inválido", path);
        long meId = ((Number) me.get("id")).longValue();

        if (method.equals("GET") && path.equals("/api/v1/users/me")) return ok(publicUser(me));
        if (path.startsWith("/api/v1/admin/")) return fail(403, "Acesso negado", path);

        if (path.equals("/api/v1/projects")) {
            if (method.equals("GET")) {
                List<Map<String, Object>> list = new ArrayList<>();
                for (Map<String, Object> p : table("projects")) if (owns(p, meId)) list.add(projectOut(p));
                list.sort((a, b) -> Long.compare(((Number) b.get("id")).longValue(), ((Number) a.get("id")).longValue()));
                return ok(page(list, params));
            }
            if (method.equals("POST")) {
                List<String> errs = validateProject(body);
                if (!errs.isEmpty()) return fail(400, String.join("; ", errs), path);
                Map<String, Object> p = map("id", nextId("project"), "name", text(body.get("name")), "description", blankToNull(body.get("description")),
                        "status", body.get("status"), "ownerId", meId, "createdAt", now());
                table("projects").add(p);
                persist();
                return new Response(201, projectOut(p));
            }
        }

        Matcher m;
        if ((m = PROJECT.matcher(path)).matches()) {
            Map<String, Object> p = ownProject(Long.parseLong(m.group(1)), meId);
            if (p == null) return fail(404, "Projeto não encontrado", path);
            switch (method) {
                case "GET" -> { return ok(projectOut(p)); }
                case "PUT" -> {
                    List<String> errs = validateProject(body);
                    if (!errs.isEmpty()) return fail(400, String.join("; ", errs), path);
                    p.put("name", text(body.get("name")));
                    p.put("description", blankToNull(body.get("description")));
                    p.put("status", body.get("status"));
                    persist();
                    return ok(projectOut(p));
                }
                case "DELETE" -> {
                    table("projects").remove(p);
                    table("tasks").removeIf(t -> t.get("projectId").equals(p.get("id")));
                    persist();
                    return new Response(204, null);
                }
                default -> { }
            }
        }

        if ((m = PROJECT_TASKS.matcher(path)).matches()) {
            Map<String, Object> p = ownProject(Long.parseLong(m.group(1)), meId);
            if (p == null) return fail(404, "Projeto não encontrado", path);
            if (method.equals("GET")) {
                List<Map<String, Object>> list = new ArrayList<>();
                for (Map<String, Object> t : table("tasks")) if (t.get("projectId").equals(p.get("id"))) list.add(taskOut(t));
                return ok(page(list, params));
            }
            if (method.equals("POST")) {
                List<String> errs = validateTask(body);
                if (!errs.isEmpty()) return fail(400, String.join("; ", errs), path);
                Map<String, Object> t = task(nextId("task"), ((Number) p.get("id")).longValue(), text(body.get("title")), blankToNull(body.get("description")),
                        "TODO", (String) body.get("priority"), blankToNull(body.get("dueDate")));
                table("tasks").add(t);
                persist();
                return new Response(201, taskOut(t));
            }
        }

        if (method.equals("PUT") && (m = TASK_STATUS_PATH.matcher(path)).matches()) {
            Map<String, Object> t = ownTask(Long.parseLong(m.group(1)), meId);
            if (t == null) return fail(404, "Tarefa não encontrada", path);
            if (!TASK_STATUS.contains(body.get("status"))) return fail(400, "status: não deve ser nulo", path);
            t.put("status", body.get("status"));
            persist();
            return ok(taskOut(t));
        }

        if ((m = TASK.matcher(path)).matches()) {
            Map<String, Object> t = ownTask(Long.parseLong(m.group(1)), meId);
            if (t == null) return fail(404, "Tarefa não encontrada", path);
            switch (method) {
                case "GET" -> { return ok(taskOut(t)); }
                case "PUT" -> {
                    List<String> errs = validateTask(body);
                    if (!errs.isEmpty()) return fail(400, String.join("; ", errs), path);
                    t.put("title", text(body.get("title")));
                    t.put("description", blankToNull(body.get("description")));
                    t.put("priority", body.get("priority"));
                    t.put("dueDate", blankToNull(body.get("dueDate")));
                    persist();
                    return ok(taskOut(t));
                }
                case "DELETE" -> {
                    table("tasks").remove(t);
                    persist();
                    return new Response(204, null);
                }
                default -> { }
            }
        }
        return fail(404, "Rota não encontrada", path);
    }

    private static boolean owns(Map<String, Object> project, long userId) {
        return ((Number) project.get("ownerId")).longValue() == userId;
    }

    private Map<String, Object> ownProject(long id, long userId) {
        return table("projects").stream()
                .filter(p -> ((Number) p.get("id")).longValue() == id && owns(p, userId)).findFirst().orElse(null);
    }

    private Map<String, Object> ownTask(long id, long userId) {
        for (Map<String, Object> t : table("tasks")) {
            if (((Number) t.get("id")).longValue() == id) {
                return ownProject(((Number) t.get("projectId")).longValue(), userId) != null ? t : null;
            }
        }
        return null;
    }
}
