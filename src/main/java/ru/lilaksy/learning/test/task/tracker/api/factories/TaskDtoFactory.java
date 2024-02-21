package ru.lilaksy.learning.test.task.tracker.api.factories;

import org.springframework.stereotype.Component;
import ru.lilaksy.learning.test.task.tracker.api.dto.TaskDto;
import ru.lilaksy.learning.test.task.tracker.store.entities.TaskEntity;

@Component
public class TaskDtoFactory {

    public TaskDto makeTaskDto (TaskEntity entity) {
        return TaskDto.builder()
                .name(entity.getName())
                .id(entity.getId())
                .createdAt(entity.getCreatedAt())
                .description(entity.getDescription())
                .build();
    }
}
