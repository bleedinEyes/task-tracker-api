package ru.lilaksy.learning.test.task.tracker.api.factories;

import org.springframework.stereotype.Component;
import ru.lilaksy.learning.test.task.tracker.api.dto.ProjectDto;
import ru.lilaksy.learning.test.task.tracker.store.entities.ProjectEntity;

@Component
public class ProjectDtoFactory {

    public ProjectDto makeProjectDto (ProjectEntity entity) {
        return ProjectDto.builder()
                .name(entity.getName())
                .id(entity.getId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
