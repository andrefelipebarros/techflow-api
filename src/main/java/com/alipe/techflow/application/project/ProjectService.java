package com.alipe.techflow.application.project;

import com.alipe.techflow.domain.model.Project;
import com.alipe.techflow.domain.repository.ProjectRepository;
import com.alipe.techflow.infrastructure.security.CurrentUser;
import java.util.function.Supplier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
    private final ProjectRepository projects;
    private final CurrentUser currentUser;

    public ProjectService(ProjectRepository projects, CurrentUser currentUser) {
        this.projects = projects;
        this.currentUser = currentUser;
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        var owner = currentUser.get();
        return ProjectResponse.from(projects.save(new Project(request.name().trim(), request.description(), request.status(), owner)));
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> list(Pageable pageable) {
        return projects.findByOwnerId(currentUser.id(), pageable).map(ProjectResponse::from);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "projects", key = "#id + ':' + #ownerId")
    public ProjectResponse findById(Long id, Long ownerId) {
        var p = projects.findByIdAndOwnerId(id, ownerId).orElseThrow(notFound());
        return ProjectResponse.from(p);
    }

    @Transactional
    @CacheEvict(cacheNames = "projects", allEntries = true)
    public ProjectResponse update(Long id, ProjectRequest request) {
        var p = projects.findByIdAndOwnerId(id, currentUser.id()).orElseThrow(notFound());
        p.update(request.name().trim(), request.description(), request.status());
        return ProjectResponse.from(p);
    }

    @Transactional
    @CacheEvict(cacheNames = "projects", allEntries = true)
    public void delete(Long id) {
        var p = projects.findByIdAndOwnerId(id, currentUser.id()).orElseThrow(notFound());
        projects.delete(p);
    }

    private Supplier<RuntimeException> notFound() {
        return () -> new java.util.NoSuchElementException("Projeto não encontrado");
    }
}
