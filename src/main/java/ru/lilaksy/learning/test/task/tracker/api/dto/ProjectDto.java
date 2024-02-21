package ru.lilaksy.learning.test.task.tracker.api.dto;

import lombok.*;

import java.time.Instant;

@Builder
public record ProjectDto(Long id, String name, Instant createdAt) {
}
