package com.post.server.repository;

import com.post.server.entity.TaskEntity;
import com.post.server.entity.UserEntity;
import com.post.server.entity.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    List<TaskEntity> findByUser(UserEntity user);
    List<TaskEntity> findByUserOrderByPublishedAtDesc(UserEntity user);
    List<TaskEntity> findByUserAndStatusOrderByPublishedAtDesc(UserEntity user, TaskStatus status);
    Optional<TaskEntity> findByIdAndUser(Long id, UserEntity user);
    List<TaskEntity> findByStatus(TaskStatus status);
}
