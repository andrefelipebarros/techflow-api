package com.alipe.techflow.application.project;

import com.alipe.techflow.domain.model.Project;
import com.alipe.techflow.domain.model.ProjectStatus;
import java.time.OffsetDateTime;

public record ProjectResponse(Long id, String name, String description, ProjectStatus status,
                              Long ownerId, OffsetDateTime createdAt) implements java.io.Serializable {
    public static ProjectResponse from(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(), p.getStatus(), p.getOwner().getId(), p.getCreatedAt());
    }
}
