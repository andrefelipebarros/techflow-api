package com.alipe.techflow.desktop.model;

import java.time.LocalDate;
import java.util.Map;

/** Modelos que espelham os DTOs da TechFlow API. */
public final class Models {
    private Models() {}

    public enum TaskStatus {
        TODO("A fazer"), IN_PROGRESS("Em andamento"), DONE("Concluída"), CANCELED("Cancelada");
        public final String label;
        TaskStatus(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public enum Priority {
        LOW("Baixa"), MEDIUM("Média"), HIGH("Alta"), URGENT("Urgente");
        public final String label;
        Priority(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public enum ProjectStatus {
        PLANNED("Planejado"), IN_PROGRESS("Em andamento"), COMPLETED("Concluído"), ARCHIVED("Arquivado");
        public final String label;
        ProjectStatus(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public record User(long id, String name, String email, String role) {
        public static User from(Map<?, ?> m) {
            return new User(num(m, "id"), str(m, "name"), str(m, "email"), str(m, "role"));
        }
    }

    public record Project(long id, String name, String description, ProjectStatus status) {
        public static Project from(Map<?, ?> m) {
            return new Project(num(m, "id"), str(m, "name"), str(m, "description"),
                    ProjectStatus.valueOf(str(m, "status")));
        }
    }

    public record Task(long id, long projectId, String title, String description,
                       TaskStatus status, Priority priority, LocalDate dueDate) {
        public static Task from(Map<?, ?> m) {
            String due = str(m, "dueDate");
            return new Task(num(m, "id"), num(m, "projectId"), str(m, "title"), str(m, "description"),
                    TaskStatus.valueOf(str(m, "status")), Priority.valueOf(str(m, "priority")),
                    due == null || due.isBlank() ? null : LocalDate.parse(due));
        }
    }

    static String str(Map<?, ?> m, String key) {
        Object v = m.get(key);
        return v == null ? null : v.toString();
    }

    static long num(Map<?, ?> m, String key) {
        Object v = m.get(key);
        return v instanceof Number n ? n.longValue() : 0L;
    }
}
