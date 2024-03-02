package ru.lilaksy.learning.test.task.tracker.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.lilaksy.learning.test.task.tracker.store.entities.TaskEntity;
import ru.lilaksy.learning.test.task.tracker.store.entities.TaskStateEntity;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    Optional<TaskEntity> findTaskEntityByRightTaskIdIsNullAndTaskStateId(Long taskStateId);

    Optional<TaskEntity> findTaskEntityByTaskStateIdAndNameContainsIgnoreCase(Long taskStateId, String taskName);
}
