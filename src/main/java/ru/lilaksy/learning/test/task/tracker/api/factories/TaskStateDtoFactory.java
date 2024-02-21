package ru.lilaksy.learning.test.task.tracker.api.factories;

import org.springframework.stereotype.Component;
import ru.lilaksy.learning.test.task.tracker.api.dto.ProjectDto;
import ru.lilaksy.learning.test.task.tracker.api.dto.TaskStateDto;
import ru.lilaksy.learning.test.task.tracker.store.entities.ProjectEntity;
import ru.lilaksy.learning.test.task.tracker.store.entities.TaskStateEntity;

@Component
public class TaskStateDtoFactory {

    public TaskStateDto makeTaskStateDto (TaskStateEntity entity) {
        return TaskStateDto.builder()
                .name(entity.getName())
                .id(entity.getId())
                .createdAt(entity.getCreatedAt())
                .position(entity.getPosition())
                .build();
    }
}
