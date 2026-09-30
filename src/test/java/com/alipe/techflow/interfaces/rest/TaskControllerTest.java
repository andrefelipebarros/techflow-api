package com.alipe.techflow.interfaces.rest;

import com.alipe.techflow.application.task.TaskResponse;
import com.alipe.techflow.application.task.TaskService;
import com.alipe.techflow.domain.model.Priority;
import com.alipe.techflow.domain.model.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    TaskService taskService;

    @Test
    void shouldReturnTaskAsJson() throws Exception {
        var response = new TaskResponse(1L, 10L, "Implementar login", "JWT", TaskStatus.TODO,
                Priority.HIGH, LocalDate.of(2026, 10, 15), null);
        given(taskService.findById(1L)).willReturn(response);

        mvc.perform(get("/api/v1/tasks/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Implementar login"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }
}
