package ru.lilaksy.learning.test.task.tracker.api.controllers.helpers;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.lilaksy.learning.test.task.tracker.api.exceptions.NotFoundException;
import ru.lilaksy.learning.test.task.tracker.store.entities.ProjectEntity;
import ru.lilaksy.learning.test.task.tracker.store.entities.TaskEntity;
import ru.lilaksy.learning.test.task.tracker.store.entities.TaskStateEntity;
import ru.lilaksy.learning.test.task.tracker.store.repositories.ProjectRepository;
import ru.lilaksy.learning.test.task.tracker.store.repositories.TaskRepository;
import ru.lilaksy.learning.test.task.tracker.store.repositories.TaskStateRepository;

@RequiredArgsConstructor
@Component
@Transactional
public class HelperController {

    private final ProjectRepository projectRepository;

    private final TaskStateRepository taskStateRepository;

    private final TaskRepository taskRepository;

    public ProjectEntity getProjectByIdOrThrowException(Long projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(
                        () -> new NotFoundException(
                                String.format("Project with id \"%s\" does not exists.", projectId)
                        )
                );
    }

    public TaskStateEntity getTaskStateByIdOrThrowException(Long taskStateId) {
        return taskStateRepository
                .findById(taskStateId)
                .orElseThrow(
                        () -> new NotFoundException(
                                String.format("Task state with id \"%s\" does not exists.", taskStateId)
                        )
                );
    }

    public TaskEntity getTaskByIdOrThrowException(Long taskId) {
        return taskRepository
                .findById(taskId)
                .orElseThrow(
                        () -> new NotFoundException(
                                String.format("Task with id \"%s\" does not exists.", taskId)
                        )
                );
    }
}
