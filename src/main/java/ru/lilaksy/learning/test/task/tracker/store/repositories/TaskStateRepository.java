package ru.lilaksy.learning.test.task.tracker.store.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.lilaksy.learning.test.task.tracker.store.entities.ProjectEntity;
import ru.lilaksy.learning.test.task.tracker.store.entities.TaskStateEntity;

import java.util.Optional;
import java.util.stream.Stream;

public interface TaskStateRepository extends JpaRepository<TaskStateEntity, Long> {

    Optional<TaskStateEntity> findTaskStateEntityByRightTaskStateIdIsNullAndProjectId(Long project_id);

    Optional<TaskStateEntity> findTaskStateEntityByProjectIdAndNameContainsIgnoreCase(Long projectId, String taskStateName);
}
