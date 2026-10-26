package com.post.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.post.server.dto.TaskDto;
import com.post.server.entity.UserEntity;
import com.post.server.entity.enums.Role;
import com.post.server.entity.enums.TaskStatus;
import com.post.server.exception.CustomNotFoundException;
import com.post.server.exception.GlobalExceptionHandler;
import com.post.server.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private TaskDto.TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders
                .standaloneSetup(taskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        taskResponse = TaskDto.TaskResponse.builder()
                .id(1L)
                .title("Test Task")
                .content("Test Content")
                .publishedAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .status(TaskStatus.INCOMPLETE)
                .userId(10L)
                .userEmail("test@example.com")
                .build();
    }

    @Test
    @DisplayName("POST /tasks creates task and returns 201 Created")
    void createTask_success() throws Exception {
        TaskDto.CreateTaskRequest request = TaskDto.CreateTaskRequest.builder()
                .title("New Task")
                .content("Content here")
                .status(TaskStatus.INCOMPLETE)
                .build();

        when(taskService.createTask(any(TaskDto.CreateTaskRequest.class), any()))
                .thenReturn(taskResponse);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Task"));
    }

    @Test
    @DisplayName("POST /tasks with blank title returns 400 Bad Request")
    void createTask_validationError() throws Exception {
        TaskDto.CreateTaskRequest request = TaskDto.CreateTaskRequest.builder()
                .title("")
                .content("Content without title")
                .build();

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /tasks returns 200 with list of tasks")
    void getAllTasks_success() throws Exception {
        when(taskService.getAllTasks(any(), isNull())).thenReturn(List.of(taskResponse));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Task"));
    }

    @Test
    @DisplayName("GET /tasks?status=complete accepts status parameter")
    void getAllTasks_withStatusFilter() throws Exception {
        when(taskService.getAllTasks(any(), eq(TaskStatus.COMPLETE))).thenReturn(List.of());

        mockMvc.perform(get("/tasks?status=complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /tasks/{id} returns 200 with task when found")
    void getTaskById_success() throws Exception {
        when(taskService.getTaskById(eq(1L), any())).thenReturn(taskResponse);

        mockMvc.perform(get("/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Task"));
    }

    @Test
    @DisplayName("GET /tasks/{id} returns 404 when task not found")
    void getTaskById_notFound() throws Exception {
        when(taskService.getTaskById(eq(999L), any()))
                .thenThrow(new CustomNotFoundException("Task not found with id: 999"));

        mockMvc.perform(get("/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found with id: 999"));
    }

    @Test
    @DisplayName("PUT /tasks/{id} updates task and returns 200")
    void updateTask_success() throws Exception {
        TaskDto.UpdateTaskRequest updateRequest = TaskDto.UpdateTaskRequest.builder()
                .title("Updated Task Title")
                .status(TaskStatus.COMPLETE)
                .build();

        TaskDto.TaskResponse updatedResponse = TaskDto.TaskResponse.builder()
                .id(1L)
                .title("Updated Task Title")
                .status(TaskStatus.COMPLETE)
                .build();

        when(taskService.updateTask(eq(1L), any(TaskDto.UpdateTaskRequest.class), any()))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Task Title"))
                .andExpect(jsonPath("$.status").value("complete"));
    }

    @Test
    @DisplayName("DELETE /tasks/{id} returns 200 with success message")
    void deleteTask_success() throws Exception {
        doNothing().when(taskService).deleteTask(eq(1L), any());

        mockMvc.perform(delete("/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Task deleted successfully"));

        verify(taskService).deleteTask(eq(1L), any());
    }
}
