package com.post.server.controller;

import com.post.server.dto.TaskDto;
import com.post.server.entity.UserEntity;
import com.post.server.entity.enums.TaskStatus;
import com.post.server.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskDto.TaskResponse> createTask(
            @Valid @RequestBody TaskDto.CreateTaskRequest request,
            @AuthenticationPrincipal UserEntity currentUser
    ) {
        TaskDto.TaskResponse created = taskService.createTask(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<TaskDto.TaskResponse>> getAllTasks(
            @RequestParam(name = "status", required = false) String status,
            @AuthenticationPrincipal UserEntity currentUser
    ) {
        TaskStatus taskStatus = (status != null && !status.isBlank()) ? TaskStatus.fromString(status) : null;
        List<TaskDto.TaskResponse> tasks = taskService.getAllTasks(currentUser, taskStatus);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDto.TaskResponse> getTaskById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserEntity currentUser
    ) {
        TaskDto.TaskResponse task = taskService.getTaskById(id, currentUser);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDto.TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskDto.UpdateTaskRequest request,
            @AuthenticationPrincipal UserEntity currentUser
    ) {
        TaskDto.TaskResponse updated = taskService.updateTask(id, request, currentUser);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserEntity currentUser
    ) {
        taskService.deleteTask(id, currentUser);
        return ResponseEntity.ok("Task deleted successfully");
    }
}
