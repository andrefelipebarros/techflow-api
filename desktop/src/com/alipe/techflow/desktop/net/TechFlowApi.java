package com.alipe.techflow.desktop.net;

import com.alipe.techflow.desktop.model.Models.Priority;
import com.alipe.techflow.desktop.model.Models.Project;
import com.alipe.techflow.desktop.model.Models.ProjectStatus;
import com.alipe.techflow.desktop.model.Models.Task;
import com.alipe.techflow.desktop.model.Models.TaskStatus;
import com.alipe.techflow.desktop.model.Models.User;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Cliente da TechFlow API: um método por endpoint, sempre registrando no console. */
public final class TechFlowApi {
    private final Transport transport;
    private final RequestLog log;
    private String token;

    public TechFlowApi(Transport transport, RequestLog log) {
        this.transport = transport;
        this.log = log;
    }

    public Transport transport() { return transport; }

    public RequestLog log() { return log; }

    public boolean isLoggedIn() { return token != null; }

    public void logout() { token = null; }

    // ---------- chamada base ----------

    private Object call(String method, String path, Object body) {
        long start = System.nanoTime();
        try {
            Transport.Response res = transport.send(method, path, token, body);
            log.add(method, path, res.status(), (System.nanoTime() - start) / 1_000_000, body, res.body());
            if (res.status() >= 400) {
                String msg = res.body() instanceof Map<?, ?> m && m.get("message") != null
                        ? m.get("message").toString() : "Erro " + res.status();
                throw new ApiException(res.status(), msg);
            }
            return res.body();
        } catch (ApiException e) {
            if (e.status() == 0) {
                log.add(method, path, 0, (System.nanoTime() - start) / 1_000_000, body, Map.of("message", e.getMessage()));
            }
            throw e;
        }
    }

    private static Map<String, Object> obj(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<?, ?> asMap(Object o) { return (Map<?, ?>) o; }

    private static List<Map<?, ?>> content(Object page) {
        List<Map<?, ?>> out = new ArrayList<>();
        Object c = asMap(page).get("content");
        if (c instanceof List<?> list) for (Object o : list) out.add(asMap(o));
        return out;
    }

    // ---------- autenticação ----------

    public void login(String email, String password) {
        token = (String) asMap(call("POST", "/api/v1/auth/login", obj("email", email, "password", password))).get("accessToken");
    }

    public void register(String name, String email, String password) {
        token = (String) asMap(call("POST", "/api/v1/auth/register", obj("name", name, "email", email, "password", password))).get("accessToken");
    }

    public User me() { return User.from(asMap(call("GET", "/api/v1/users/me", null))); }

    public boolean health() {
        Object r = call("GET", "/actuator/health", null);
        return r instanceof Map<?, ?> m && "UP".equals(m.get("status"));
    }

    // ---------- projetos ----------

    public List<Project> projects() {
        List<Project> out = new ArrayList<>();
        for (Map<?, ?> m : content(call("GET", "/api/v1/projects?page=0&size=100", null))) out.add(Project.from(m));
        return out;
    }

    public Project createProject(String name, String description, ProjectStatus status) {
        return Project.from(asMap(call("POST", "/api/v1/projects", projectBody(name, description, status))));
    }

    public Project updateProject(long id, String name, String description, ProjectStatus status) {
        return Project.from(asMap(call("PUT", "/api/v1/projects/" + id, projectBody(name, description, status))));
    }

    public void deleteProject(long id) { call("DELETE", "/api/v1/projects/" + id, null); }

    private static Map<String, Object> projectBody(String name, String description, ProjectStatus status) {
        Map<String, Object> b = obj("name", name);
        if (description != null && !description.isBlank()) b.put("description", description);
        b.put("status", status.name());
        return b;
    }

    // ---------- tarefas ----------

    public List<Task> tasks(long projectId) {
        List<Task> out = new ArrayList<>();
        for (Map<?, ?> m : content(call("GET", "/api/v1/projects/" + projectId + "/tasks?page=0&size=100", null))) out.add(Task.from(m));
        return out;
    }

    public Task createTask(long projectId, String title, String description, Priority priority, LocalDate due) {
        return Task.from(asMap(call("POST", "/api/v1/projects/" + projectId + "/tasks", taskBody(title, description, priority, due))));
    }

    public Task updateTask(long id, String title, String description, Priority priority, LocalDate due) {
        return Task.from(asMap(call("PUT", "/api/v1/tasks/" + id, taskBody(title, description, priority, due))));
    }

    public Task changeStatus(long id, TaskStatus status) {
        return Task.from(asMap(call("PUT", "/api/v1/tasks/" + id + "/status", obj("status", status.name()))));
    }

    public void deleteTask(long id) { call("DELETE", "/api/v1/tasks/" + id, null); }

    private static Map<String, Object> taskBody(String title, String description, Priority priority, LocalDate due) {
        Map<String, Object> b = obj("title", title);
        if (description != null && !description.isBlank()) b.put("description", description);
        b.put("priority", priority.name());
        if (due != null) b.put("dueDate", due.toString());
        return b;
    }
}
