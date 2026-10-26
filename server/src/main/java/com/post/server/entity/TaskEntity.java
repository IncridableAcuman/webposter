package com.post.server.entity;

import com.post.server.entity.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.INCOMPLETE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @PrePersist
    public void prePersist() {
        if (this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = TaskStatus.INCOMPLETE;
        }
    }
}
