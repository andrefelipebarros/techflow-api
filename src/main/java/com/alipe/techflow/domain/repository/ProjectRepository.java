package com.alipe.techflow.domain.repository;

import com.alipe.techflow.domain.model.Project;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Page<Project> findByOwnerId(Long ownerId, Pageable pageable);
    Optional<Project> findByIdAndOwnerId(Long id, Long ownerId);
}
