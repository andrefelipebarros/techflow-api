package com.alipe.techflow.domain.repository;

import com.alipe.techflow.domain.model.Task;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByProjectId(Long projectId, Pageable pageable);
    Optional<Task> findByIdAndProjectOwnerId(Long id, Long ownerId);
}
