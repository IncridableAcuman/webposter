package com.post.server.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.post.server.dto.TaskDto;
import com.post.server.entity.TaskEntity;
import com.post.server.entity.UserEntity;
import com.post.server.entity.enums.Role;
import com.post.server.entity.enums.TaskStatus;
import com.post.server.repository.TaskRepository;
import com.post.server.repository.TokenRepository;
import com.post.server.repository.UserRepository;
import com.post.server.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserEntity user;
    private String accessToken;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        taskRepository.deleteAll();
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        user = UserEntity.builder()
                .username("taskuser")
                .email("taskuser@example.com")
                .password(passwordEncoder.encode("secret123"))
                .role(Role.USER)
                .build();
        user = userRepository.save(user);

        accessToken = jwtUtil.generateAccessToken(user);
    }

    @Test
    @DisplayName("POST /tasks creates task with columns title, content, published_at, status")
    void testCreateTaskEndToEnd() throws Exception {
        LocalDateTime publishedAt = LocalDateTime.of(2026, 10, 26, 15, 30, 0);
        TaskDto.CreateTaskRequest request = TaskDto.CreateTaskRequest.builder()
                .title("Complete Report")
                .content("Detailed quarterly report content")
                .publishedAt(publishedAt)
                .status(TaskStatus.INCOMPLETE)
                .build();

        mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Complete Report"))
                .andExpect(jsonPath("$.content").value("Detailed quarterly report content"))
                .andExpect(jsonPath("$.status").value("incomplete"))
                .andExpect(jsonPath("$.publishedAt").value("2026-10-26T15:30:00"))
                .andExpect(jsonPath("$.userEmail").value("taskuser@example.com"));

        assertEquals(1, taskRepository.count());
        TaskEntity saved = taskRepository.findAll().get(0);
        assertEquals("Complete Report", saved.getTitle());
        assertEquals(TaskStatus.INCOMPLETE, saved.getStatus());
        assertEquals(user.getId(), saved.getUser().getId());
    }

    @Test
    @DisplayName("POST /tasks supports 'complate' string deserialization")
    void testCreateTaskWithComplateSpelling() throws Exception {
        String jsonPayload = """
                {
                    "title": "Bug fix task",
                    "content": "Fix null pointer",
                    "status": "complate"
                }
                """;

        mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("complete"));
    }

    @Test
    @DisplayName("GET /tasks returns all tasks of current user")
    void testGetAllTasksEndToEnd() throws Exception {
        TaskEntity task1 = TaskEntity.builder()
                .title("Task 1")
                .content("Content 1")
                .publishedAt(LocalDateTime.now().minusDays(1))
                .status(TaskStatus.INCOMPLETE)
                .user(user)
                .build();
        TaskEntity task2 = TaskEntity.builder()
                .title("Task 2")
                .content("Content 2")
                .publishedAt(LocalDateTime.now())
                .status(TaskStatus.COMPLETE)
                .user(user)
                .build();
        taskRepository.save(task1);
        taskRepository.save(task2);

        mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /tasks?status=complete filters tasks by status")
    void testGetTasksFilteredByStatus() throws Exception {
        TaskEntity taskIncomplete = TaskEntity.builder()
                .title("Pending Task")
                .content("Pending")
                .status(TaskStatus.INCOMPLETE)
                .user(user)
                .build();
        TaskEntity taskComplete = TaskEntity.builder()
                .title("Done Task")
                .content("Done")
                .status(TaskStatus.COMPLETE)
                .user(user)
                .build();
        taskRepository.save(taskIncomplete);
        taskRepository.save(taskComplete);

        // Filter by complete
        mockMvc.perform(get("/tasks?status=complete")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Done Task"));

        // Filter by incomplate (custom spelling supported)
        mockMvc.perform(get("/tasks?status=incomplate")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Pending Task"));
    }

    @Test
    @DisplayName("GET /tasks/{id} returns single task by ID")
    void testGetTaskByIdEndToEnd() throws Exception {
        TaskEntity task = TaskEntity.builder()
                .title("Single Task")
                .content("Single Content")
                .status(TaskStatus.INCOMPLETE)
                .user(user)
                .build();
        task = taskRepository.save(task);

        mockMvc.perform(get("/tasks/" + task.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.getId()))
                .andExpect(jsonPath("$.title").value("Single Task"));
    }

    @Test
    @DisplayName("PUT /tasks/{id} updates title, content, published_at, status")
    void testUpdateTaskEndToEnd() throws Exception {
        TaskEntity task = TaskEntity.builder()
                .title("Old Title")
                .content("Old Content")
                .status(TaskStatus.INCOMPLETE)
                .user(user)
                .build();
        task = taskRepository.save(task);

        LocalDateTime updatedPublishedAt = LocalDateTime.of(2026, 12, 1, 9, 0, 0);
        TaskDto.UpdateTaskRequest updateRequest = TaskDto.UpdateTaskRequest.builder()
                .title("Updated Title")
                .content("Updated Content")
                .status(TaskStatus.COMPLETE)
                .publishedAt(updatedPublishedAt)
                .build();

        mockMvc.perform(put("/tasks/" + task.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.content").value("Updated Content"))
                .andExpect(jsonPath("$.status").value("complete"))
                .andExpect(jsonPath("$.publishedAt").value("2026-12-01T09:00:00"));

        TaskEntity refreshed = taskRepository.findById(task.getId()).orElseThrow();
        assertEquals("Updated Title", refreshed.getTitle());
        assertEquals(TaskStatus.COMPLETE, refreshed.getStatus());
    }

    @Test
    @DisplayName("DELETE /tasks/{id} deletes task from database")
    void testDeleteTaskEndToEnd() throws Exception {
        TaskEntity task = TaskEntity.builder()
                .title("Task to Delete")
                .status(TaskStatus.INCOMPLETE)
                .user(user)
                .build();
        task = taskRepository.save(task);

        mockMvc.perform(delete("/tasks/" + task.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Task deleted successfully"));

        assertFalse(taskRepository.existsById(task.getId()));
    }

    @Test
    @DisplayName("Task Isolation: User cannot access another user's task")
    void testTaskIsolationBetweenUsers() throws Exception {
        UserEntity otherUser = UserEntity.builder()
                .username("other")
                .email("other@example.com")
                .password(passwordEncoder.encode("pass123"))
                .role(Role.USER)
                .build();
        otherUser = userRepository.save(otherUser);

        TaskEntity otherTask = TaskEntity.builder()
                .title("Other User Task")
                .status(TaskStatus.INCOMPLETE)
                .user(otherUser)
                .build();
        otherTask = taskRepository.save(otherTask);

        // Current user attempts to read other user's task -> 404
        mockMvc.perform(get("/tasks/" + otherTask.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());

        // Current user attempts to delete other user's task -> 404
        mockMvc.perform(delete("/tasks/" + otherTask.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Unauthenticated request to /tasks returns 401 or 403")
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 401 || status == 403);
                });

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"No Auth\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 401 || status == 403);
                });
    }
}
