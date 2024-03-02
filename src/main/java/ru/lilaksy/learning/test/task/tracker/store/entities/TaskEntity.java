package ru.lilaksy.learning.test.task.tracker.store.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "task")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class TaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @OneToOne
    private TaskEntity leftTask;

    @OneToOne
    private TaskEntity rightTask;

    @ManyToOne
    private TaskStateEntity taskState;

    @Builder.Default
    private Instant createdAt = Instant.now();

    public Optional<TaskEntity> getLeftTask(){
        return Optional.ofNullable(leftTask);
    }

    public Optional<TaskEntity> getRightTask(){
        return Optional.ofNullable(rightTask);
    }
}
