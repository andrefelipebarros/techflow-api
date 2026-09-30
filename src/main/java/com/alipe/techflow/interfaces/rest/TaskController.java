package com.alipe.techflow.interfaces.rest;

import com.alipe.techflow.application.task.TaskRequest;
import com.alipe.techflow.application.task.TaskResponse;
import com.alipe.techflow.application.task.TaskService;
import com.alipe.techflow.application.task.TaskStatusRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@PathVariable Long projectId, @Valid @RequestBody TaskRequest request) {
        return taskService.create(projectId, request);
    }

    @GetMapping("/projects/{projectId}/tasks")
    public Page<TaskResponse> list(@PathVariable Long projectId, Pageable pageable) {
        return taskService.list(projectId, pageable);
    }

    @GetMapping("/tasks/{taskId}")
    public TaskResponse findById(@PathVariable Long taskId) {
        return taskService.findById(taskId);
    }

    @PutMapping("/tasks/{taskId}")
    public TaskResponse update(@PathVariable Long taskId, @Valid @RequestBody TaskRequest request) {
        return taskService.update(taskId, request);
    }

    @PutMapping("/tasks/{taskId}/status")
    public TaskResponse changeStatus(@PathVariable Long taskId, @Valid @RequestBody TaskStatusRequest request) {
        return taskService.changeStatus(taskId, request);
    }

    @DeleteMapping("/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long taskId) {
        taskService.delete(taskId);
    }
}
