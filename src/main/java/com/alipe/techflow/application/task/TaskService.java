package com.alipe.techflow.application.task;

import com.alipe.techflow.domain.model.Task;
import com.alipe.techflow.domain.repository.ProjectRepository;
import com.alipe.techflow.domain.repository.TaskRepository;
import com.alipe.techflow.infrastructure.messaging.TaskEvent;
import com.alipe.techflow.infrastructure.messaging.TaskEventPublisher;
import com.alipe.techflow.infrastructure.security.CurrentUser;
import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {
    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final CurrentUser currentUser;
    private final TaskEventPublisher events;

    public TaskService(TaskRepository tasks, ProjectRepository projects, CurrentUser currentUser,
                       TaskEventPublisher events) {
        this.tasks = tasks;
        this.projects = projects;
        this.currentUser = currentUser;
        this.events = events;
    }

    @Transactional
    public TaskResponse create(Long projectId, TaskRequest request) {
        var project = projects.findByIdAndOwnerId(projectId, currentUser.id())
                .orElseThrow(() -> new NoSuchElementException("Projeto não encontrado"));
        var task = tasks.save(new Task(project, request.title().trim(), request.description(), request.priority(), request.dueDate()));
        var response = TaskResponse.from(task);
        events.publish(new TaskEvent("TaskCreated", task.getId(), projectId, task.getTitle(), task.getStatus().name(), OffsetDateTime.now()));
        return response;
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> list(Long projectId, Pageable pageable) {
        projects.findByIdAndOwnerId(projectId, currentUser.id())
                .orElseThrow(() -> new NoSuchElementException("Projeto não encontrado"));
        return tasks.findByProjectId(projectId, pageable).map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long taskId) {
        return TaskResponse.from(tasks.findByIdAndProjectOwnerId(taskId, currentUser.id())
                .orElseThrow(() -> new NoSuchElementException("Tarefa não encontrada")));
    }

    @Transactional
    public TaskResponse update(Long taskId, TaskRequest request) {
        var task = ownedTask(taskId);
        task.update(request.title().trim(), request.description(), request.priority(), request.dueDate());
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse changeStatus(Long taskId, TaskStatusRequest request) {
        var task = ownedTask(taskId);
        task.changeStatus(request.status());
        var response = TaskResponse.from(task);
        events.publish(new TaskEvent("TaskStatusChanged", task.getId(), task.getProject().getId(), task.getTitle(), task.getStatus().name(), OffsetDateTime.now()));
        return response;
    }

    @Transactional
    public void delete(Long taskId) {
        tasks.delete(ownedTask(taskId));
    }

    private Task ownedTask(Long taskId) {
        return tasks.findByIdAndProjectOwnerId(taskId, currentUser.id())
                .orElseThrow(() -> new NoSuchElementException("Tarefa não encontrada"));
    }
}
