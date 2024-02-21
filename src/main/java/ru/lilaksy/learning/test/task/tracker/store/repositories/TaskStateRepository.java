package ru.lilaksy.learning.test.task.tracker.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.lilaksy.learning.test.task.tracker.store.entities.TaskStateEntity;

public interface TaskStateRepository extends JpaRepository<TaskStateEntity, Long> {
}
