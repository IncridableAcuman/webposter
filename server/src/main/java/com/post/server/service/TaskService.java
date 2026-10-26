package com.post.server.service;

import com.post.server.dto.TaskDto;
import com.post.server.entity.TaskEntity;
import com.post.server.entity.UserEntity;
import com.post.server.entity.enums.Role;
import com.post.server.entity.enums.TaskStatus;
import com.post.server.exception.CustomNotFoundException;
import com.post.server.repository.TaskRepository;
import com.post.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    private UserEntity resolveManagedUser(UserEntity user) {
        if (user == null) {
            return null;
        }
        if (user.getId() != null) {
            return userRepository.findById(user.getId()).orElse(user);
        }
        if (user.getEmail() != null) {
            return userRepository.findByEmail(user.getEmail()).orElse(user);
        }
        return user;
    }

    @Transactional
    public TaskDto.TaskResponse createTask(TaskDto.CreateTaskRequest request, UserEntity currentUser) {
        UserEntity managedUser = resolveManagedUser(currentUser);

        TaskEntity task = TaskEntity.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .publishedAt(request.getPublishedAt() != null ? request.getPublishedAt() : LocalDateTime.now())
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.INCOMPLETE)
                .user(managedUser)
                .build();

        TaskEntity saved = taskRepository.save(task);
        return TaskDto.TaskResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TaskDto.TaskResponse> getAllTasks(UserEntity currentUser, TaskStatus status) {
        UserEntity managedUser = resolveManagedUser(currentUser);
        List<TaskEntity> tasks;

        if (managedUser != null && managedUser.getRole() != Role.ADMIN) {
            if (status != null) {
                tasks = taskRepository.findByUserAndStatusOrderByPublishedAtDesc(managedUser, status);
            } else {
                tasks = taskRepository.findByUserOrderByPublishedAtDesc(managedUser);
            }
        } else {
            if (status != null) {
                tasks = taskRepository.findByStatus(status);
            } else {
                tasks = taskRepository.findAll();
            }
        }

        return tasks.stream()
                .map(TaskDto.TaskResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskDto.TaskResponse getTaskById(Long id, UserEntity currentUser) {
        TaskEntity task = findTaskAndVerifyAccess(id, currentUser);
        return TaskDto.TaskResponse.from(task);
    }

    @Transactional
    public TaskDto.TaskResponse updateTask(Long id, TaskDto.UpdateTaskRequest request, UserEntity currentUser) {
        TaskEntity task = findTaskAndVerifyAccess(id, currentUser);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            task.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            task.setContent(request.getContent());
        }
        if (request.getPublishedAt() != null) {
            task.setPublishedAt(request.getPublishedAt());
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }

        TaskEntity updated = taskRepository.save(task);
        return TaskDto.TaskResponse.from(updated);
    }

    @Transactional
    public void deleteTask(Long id, UserEntity currentUser) {
        TaskEntity task = findTaskAndVerifyAccess(id, currentUser);
        taskRepository.delete(task);
    }

    private TaskEntity findTaskAndVerifyAccess(Long id, UserEntity currentUser) {
        TaskEntity task = taskRepository.findById(id)
                .orElseThrow(() -> new CustomNotFoundException("Task not found with id: " + id));

        if (currentUser != null && currentUser.getRole() != Role.ADMIN && task.getUser() != null) {
            if (!task.getUser().getId().equals(currentUser.getId())) {
                throw new CustomNotFoundException("Task not found with id: " + id);
            }
        }

        return task;
    }
}
