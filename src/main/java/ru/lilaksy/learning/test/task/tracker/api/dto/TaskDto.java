package ru.lilaksy.learning.test.task.tracker.api.dto;

import lombok.*;

import java.time.Instant;

@Builder
public record TaskDto(Long id, String name, String description, Instant createdAt) {
}
