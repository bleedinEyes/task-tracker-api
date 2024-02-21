package ru.lilaksy.learning.test.task.tracker.api.dto;

import lombok.*;

import java.time.Instant;

@Builder
public record TaskStateDto(Long id, String name, Integer position, Instant createdAt) {
}
