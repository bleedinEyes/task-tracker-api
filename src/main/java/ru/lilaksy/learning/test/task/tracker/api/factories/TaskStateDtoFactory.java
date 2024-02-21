package ru.lilaksy.learning.test.task.tracker.api.factories;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.lilaksy.learning.test.task.tracker.api.dto.ProjectDto;
import ru.lilaksy.learning.test.task.tracker.api.dto.TaskStateDto;
import ru.lilaksy.learning.test.task.tracker.store.entities.ProjectEntity;
import ru.lilaksy.learning.test.task.tracker.store.entities.TaskStateEntity;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaskStateDtoFactory {

    private final TaskDtoFactory taskDtoFactory;

    public TaskStateDto makeTaskStateDto(TaskStateEntity entity) {
        return TaskStateDto.builder()
                .name(entity.getName())
                .id(entity.getId())
                .createdAt(entity.getCreatedAt())
                .leftTaskStateId(entity.getLeftTaskState().map(TaskStateEntity::getId).orElse(null))
                .rightTaskStateId(entity.getRightTaskState().map(TaskStateEntity::getId).orElse(null))
                .tasks(entity
                        .getTasks()
                        .stream()
                        .map(taskDtoFactory::makeTaskDto)
                        .collect(Collectors.toList()))
                .build();
    }
}
