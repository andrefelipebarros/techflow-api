package com.alipe.techflow.application.task;

import com.alipe.techflow.domain.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskRequest(
        @NotBlank @Size(max = 180) String title,
        @Size(max = 2000) String description,
        @NotNull Priority priority,
        LocalDate dueDate) {}
