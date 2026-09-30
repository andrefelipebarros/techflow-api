package com.alipe.techflow.application.task;

import com.alipe.techflow.domain.model.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusRequest(@NotNull TaskStatus status) {}
