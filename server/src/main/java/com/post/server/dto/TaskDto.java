package com.post.server.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.post.server.entity.TaskEntity;
import com.post.server.entity.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class TaskDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateTaskRequest {
        @NotBlank(message = "Title is required")
        private String title;

        private String content;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime publishedAt;

        private TaskStatus status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateTaskRequest {
        private String title;

        private String content;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime publishedAt;

        private TaskStatus status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaskResponse {
        private Long id;
        private String title;
        private String content;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime publishedAt;

        private TaskStatus status;
        private Long userId;
        private String userEmail;

        public static TaskResponse from(TaskEntity task) {
            return TaskResponse.builder()
                    .id(task.getId())
                    .title(task.getTitle())
                    .content(task.getContent())
                    .publishedAt(task.getPublishedAt())
                    .status(task.getStatus())
                    .userId(task.getUser() != null ? task.getUser().getId() : null)
                    .userEmail(task.getUser() != null ? task.getUser().getEmail() : null)
                    .build();
        }
    }
}
