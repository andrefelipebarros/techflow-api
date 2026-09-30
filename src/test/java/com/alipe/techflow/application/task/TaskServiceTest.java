package com.alipe.techflow.application.task;

import com.alipe.techflow.domain.model.Project;
import com.alipe.techflow.domain.model.ProjectStatus;
import com.alipe.techflow.domain.model.Priority;
import com.alipe.techflow.domain.model.Task;
import com.alipe.techflow.domain.model.User;
import com.alipe.techflow.domain.repository.ProjectRepository;
import com.alipe.techflow.domain.repository.TaskRepository;
import com.alipe.techflow.infrastructure.messaging.TaskEventPublisher;
import com.alipe.techflow.infrastructure.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TaskServiceTest {
    private final TaskRepository tasks = mock(TaskRepository.class);
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final TaskEventPublisher events = mock(TaskEventPublisher.class);
    private final TaskService service = new TaskService(tasks, projects, currentUser, events);

    @Test
    void shouldCreateTaskForOwnedProject() {
        var owner = new User("Alice", "alice@example.com", "hash");
        var project = new Project("TechFlow", "Demo", ProjectStatus.IN_PROGRESS, owner);
        var request = new TaskRequest("Implementar login", "JWT", Priority.HIGH, null);

        given(currentUser.id()).willReturn(1L);
        given(projects.findByIdAndOwnerId(10L, 1L)).willReturn(Optional.of(project));
        given(tasks.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(10L, request);

        assertThat(response.title()).isEqualTo("Implementar login");
        assertThat(response.priority()).isEqualTo(Priority.HIGH);
        verify(events).publish(any());
    }

    @Test
    void shouldRejectTaskCreationWhenProjectIsNotOwned() {
        given(currentUser.id()).willReturn(1L);
        given(projects.findByIdAndOwnerId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(10L, new TaskRequest("Task", null, Priority.LOW, null)))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessage("Projeto não encontrado");
    }

    @Test
    void shouldListTasksUsingPagination() {
        Page<Task> page = new PageImpl<>(List.of());
        var pageable = PageRequest.of(0, 20);
        given(currentUser.id()).willReturn(1L);
        given(projects.findByIdAndOwnerId(10L, 1L)).willReturn(Optional.of(mock(Project.class)));
        given(tasks.findByProjectId(10L, pageable)).willReturn(page);

        var result = service.list(10L, pageable);

        assertThat(result.getTotalElements()).isZero();
        verify(tasks).findByProjectId(10L, pageable);
    }
}
