package com.alipe.techflow.application.task;

import com.alipe.techflow.domain.model.Priority;
import com.alipe.techflow.domain.model.Task;
import com.alipe.techflow.domain.model.TaskStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TaskResponse(Long id, Long projectId, String title, String description,
                           TaskStatus status, Priority priority, LocalDate dueDate,
                           OffsetDateTime createdAt) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getProject().getId(), task.getTitle(),
                task.getDescription(), task.getStatus(), task.getPriority(), task.getDueDate(), task.getCreatedAt());
    }
}
