package ru.lilaksy.learning.test.task.tracker.api.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Builder
public record TaskStateDto(
        Long id,
        String name,
        Long leftTaskStateId,
        Long rightTaskStateId,
        Instant createdAt,
        List<TaskDto> tasks
    ) {
}
