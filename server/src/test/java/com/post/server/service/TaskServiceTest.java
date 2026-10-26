package com.post.server.service;

import com.post.server.dto.TaskDto;
import com.post.server.entity.TaskEntity;
import com.post.server.entity.UserEntity;
import com.post.server.entity.enums.Role;
import com.post.server.entity.enums.TaskStatus;
import com.post.server.exception.CustomNotFoundException;
import com.post.server.repository.TaskRepository;
import com.post.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    private UserEntity user;
    private TaskEntity task;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder()
                .id(1L)
                .username("john")
                .email("john@example.com")
                .role(Role.USER)
                .build();

        task = TaskEntity.builder()
                .id(100L)
                .title("Initial Task")
                .content("Initial Content")
                .publishedAt(LocalDateTime.now())
                .status(TaskStatus.INCOMPLETE)
                .user(user)
                .build();
    }

    @Test
    @DisplayName("createTask creates task with default INCOMPLETE status and now timestamp when omitted")
    void createTask_defaults() {
        TaskDto.CreateTaskRequest request = TaskDto.CreateTaskRequest.builder()
                .title("New Task")
                .content("Task Content")
                .build();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> {
            TaskEntity entity = invocation.getArgument(0);
            entity.setId(101L);
            return entity;
        });

        TaskDto.TaskResponse response = taskService.createTask(request, user);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals("New Task", response.getTitle());
        assertEquals("Task Content", response.getContent());
        assertEquals(TaskStatus.INCOMPLETE, response.getStatus());
        assertNotNull(response.getPublishedAt());
        assertEquals(1L, response.getUserId());
    }

    @Test
    @DisplayName("createTask creates task with custom status (COMPLETE) and custom publishedAt")
    void createTask_customValues() {
        LocalDateTime customTime = LocalDateTime.of(2026, 1, 1, 12, 0);
        TaskDto.CreateTaskRequest request = TaskDto.CreateTaskRequest.builder()
                .title("Complete Task")
                .content("Done")
                .status(TaskStatus.COMPLETE)
                .publishedAt(customTime)
                .build();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> {
            TaskEntity entity = invocation.getArgument(0);
            entity.setId(102L);
            return entity;
        });

        TaskDto.TaskResponse response = taskService.createTask(request, user);

        assertNotNull(response);
        assertEquals("Complete Task", response.getTitle());
        assertEquals(TaskStatus.COMPLETE, response.getStatus());
        assertEquals(customTime, response.getPublishedAt());
    }

    @Test
    @DisplayName("getAllTasks returns only user's tasks for regular USER")
    void getAllTasks_userOnly() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(taskRepository.findByUserOrderByPublishedAtDesc(user)).thenReturn(List.of(task));

        List<TaskDto.TaskResponse> result = taskService.getAllTasks(user, null);

        assertEquals(1, result.size());
        assertEquals("Initial Task", result.get(0).getTitle());
        verify(taskRepository).findByUserOrderByPublishedAtDesc(user);
    }

    @Test
    @DisplayName("getAllTasks filters by status when status parameter is provided")
    void getAllTasks_filteredByStatus() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(taskRepository.findByUserAndStatusOrderByPublishedAtDesc(user, TaskStatus.COMPLETE))
                .thenReturn(List.of());

        List<TaskDto.TaskResponse> result = taskService.getAllTasks(user, TaskStatus.COMPLETE);

        assertTrue(result.isEmpty());
        verify(taskRepository).findByUserAndStatusOrderByPublishedAtDesc(user, TaskStatus.COMPLETE);
    }

    @Test
    @DisplayName("getTaskById returns task when user is the owner")
    void getTaskById_ownerSuccess() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

        TaskDto.TaskResponse response = taskService.getTaskById(100L, user);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Initial Task", response.getTitle());
    }

    @Test
    @DisplayName("getTaskById throws CustomNotFoundException when task does not exist")
    void getTaskById_notFound() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CustomNotFoundException.class, () -> taskService.getTaskById(999L, user));
    }

    @Test
    @DisplayName("getTaskById throws CustomNotFoundException when task belongs to another user")
    void getTaskById_otherUserAccessDenied() {
        UserEntity anotherUser = UserEntity.builder().id(2L).role(Role.USER).build();
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

        assertThrows(CustomNotFoundException.class, () -> taskService.getTaskById(100L, anotherUser));
    }

    @Test
    @DisplayName("updateTask updates title, content, publishedAt and status")
    void updateTask_success() {
        LocalDateTime newTime = LocalDateTime.of(2026, 5, 10, 10, 0);
        TaskDto.UpdateTaskRequest updateRequest = TaskDto.UpdateTaskRequest.builder()
                .title("Updated Title")
                .content("Updated Content")
                .status(TaskStatus.COMPLETE)
                .publishedAt(newTime)
                .build();

        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(TaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskDto.TaskResponse response = taskService.updateTask(100L, updateRequest, user);

        assertNotNull(response);
        assertEquals("Updated Title", response.getTitle());
        assertEquals("Updated Content", response.getContent());
        assertEquals(TaskStatus.COMPLETE, response.getStatus());
        assertEquals(newTime, response.getPublishedAt());
    }

    @Test
    @DisplayName("updateTask throws CustomNotFoundException when task does not exist")
    void updateTask_notFound() {
        TaskDto.UpdateTaskRequest updateRequest = TaskDto.UpdateTaskRequest.builder()
                .title("Updated")
                .build();

        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CustomNotFoundException.class, () -> taskService.updateTask(999L, updateRequest, user));
    }

    @Test
    @DisplayName("deleteTask deletes task from repository when owned by user")
    void deleteTask_success() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

        taskService.deleteTask(100L, user);

        verify(taskRepository).delete(task);
    }

    @Test
    @DisplayName("deleteTask throws CustomNotFoundException when task does not exist")
    void deleteTask_notFound() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CustomNotFoundException.class, () -> taskService.deleteTask(999L, user));
        verify(taskRepository, never()).delete(any());
    }
}
