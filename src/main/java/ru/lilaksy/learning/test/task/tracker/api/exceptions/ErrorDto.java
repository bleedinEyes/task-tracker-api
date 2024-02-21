package ru.lilaksy.learning.test.task.tracker.api.exceptions;

import lombok.*;

@Builder
public record ErrorDto(String error, String errorDescription) {
}
