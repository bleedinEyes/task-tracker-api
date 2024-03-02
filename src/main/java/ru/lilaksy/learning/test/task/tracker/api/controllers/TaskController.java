package ru.lilaksy.learning.test.task.tracker.api.controllers;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.lilaksy.learning.test.task.tracker.api.controllers.helpers.HelperController;
import ru.lilaksy.learning.test.task.tracker.api.dto.TaskDto;
import ru.lilaksy.learning.test.task.tracker.api.exceptions.BadRequestException;
import ru.lilaksy.learning.test.task.tracker.api.factories.TaskDtoFactory;
import ru.lilaksy.learning.test.task.tracker.store.entities.TaskEntity;
import ru.lilaksy.learning.test.task.tracker.store.entities.TaskStateEntity;
import ru.lilaksy.learning.test.task.tracker.store.repositories.TaskRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@Transactional
public class TaskController {

    private final TaskRepository taskRepository;

    private final TaskDtoFactory taskDtoFactory;

    private final HelperController helperController;

    public static final String CREATE_TASK = "/api/task-states/{task_state_id}";
    public static final String EDIT_TASK = "/api/tasks/{task_id}";
    public static final String MOVE_TASK = "/api/tasks/{task_id}/move";
    public static final String GET_TASKS = "/api/task-states/{task_state_id}";
    public static final String DELETE_TASK = "/api/tasks/{task_id}";

    @GetMapping(GET_TASKS)
    public List<TaskDto> getTasks(@PathVariable(name = "task_state_id") Long taskStateId) {

        TaskStateEntity taskStateEntity = helperController.getTaskStateByIdOrThrowException(taskStateId);

        return taskStateEntity
                .getTasks()
                .stream()
                .map(taskDtoFactory::makeTaskDto)
                .collect(Collectors.toList());
    }

    @PostMapping(CREATE_TASK)
    public TaskDto createTask(
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "task_name") String taskName,
            @RequestParam(name = "task_description", required = false) Optional<String> optionalTaskDescription
    ) {

        if (taskName.trim().isEmpty()) {
            throw new BadRequestException("Name can not be empty!");
        }

        TaskStateEntity taskStateEntity = helperController.getTaskStateByIdOrThrowException(taskStateId);

        taskStateEntity.getTasks()
                .stream()
                .map(TaskEntity::getName)
                .filter(anotherTaskName -> anotherTaskName.equalsIgnoreCase(taskName))
                .findAny()
                .ifPresent(e -> {
                    throw new BadRequestException(String.format("Task \"%s\" already exists!", taskName));
                });

        Optional<TaskEntity> optionalAnotherTask = taskRepository.findTaskEntityByRightTaskIdIsNullAndTaskStateId(taskStateId);

        String taskDescription = optionalTaskDescription.orElse(null);

        TaskEntity task = taskRepository.saveAndFlush(
                TaskEntity.builder()
                        .name(taskName)
                        .description(taskDescription)
                        .taskState(taskStateEntity)
                        .build()
        );

        optionalAnotherTask
                .ifPresent(anotherTask -> {
                    task.setLeftTask(anotherTask);
                    anotherTask.setRightTask(task);
                    taskRepository.saveAndFlush(anotherTask);
                });

        final TaskEntity savedTask = taskRepository.saveAndFlush(task);

        return taskDtoFactory.makeTaskDto(savedTask);
    }

    @PatchMapping(EDIT_TASK)
    public TaskDto editTask(@PathVariable(name = "task_id") Long taskId,
                            @RequestParam(name = "task_name", required = false) Optional<String> optionalTaskName,
                            @RequestParam(name = "task_description", required = false) Optional<String> optionalTaskDescription){
        TaskEntity task = helperController.getTaskByIdOrThrowException(taskId);

        if (optionalTaskName.isEmpty() && optionalTaskDescription.isEmpty()) {
            return taskDtoFactory.makeTaskDto(task);
        }

        if (optionalTaskName.isPresent() && optionalTaskName.get().trim().isEmpty()) {
            throw new BadRequestException("Name can not be empty!");
        }

        if (optionalTaskName.isPresent() && !optionalTaskName.get().equals(task.getName())){
            taskRepository.findTaskEntityByTaskStateIdAndNameContainsIgnoreCase(task.getTaskState().getId(), optionalTaskName.get())
                    .filter(anotherTask -> !anotherTask.getId().equals(taskId))
                    .ifPresent(e -> {
                        throw new BadRequestException(String.format("Task \"%s\" already exists.", optionalTaskName));
                    });
            task.setName(optionalTaskName.get());
        }

        optionalTaskDescription.ifPresent(task::setDescription);

        task = taskRepository.saveAndFlush(task);

        return taskDtoFactory.makeTaskDto(task);
    }
}
