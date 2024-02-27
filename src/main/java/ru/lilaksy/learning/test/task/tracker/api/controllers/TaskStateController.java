package ru.lilaksy.learning.test.task.tracker.api.controllers;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.lilaksy.learning.test.task.tracker.api.controllers.helpers.HelperController;
import ru.lilaksy.learning.test.task.tracker.api.dto.ProjectDto;
import ru.lilaksy.learning.test.task.tracker.api.dto.TaskStateDto;
import ru.lilaksy.learning.test.task.tracker.api.exceptions.BadRequestException;
import ru.lilaksy.learning.test.task.tracker.api.exceptions.NotFoundException;
import ru.lilaksy.learning.test.task.tracker.api.factories.TaskStateDtoFactory;
import ru.lilaksy.learning.test.task.tracker.store.entities.ProjectEntity;
import ru.lilaksy.learning.test.task.tracker.store.entities.TaskStateEntity;
import ru.lilaksy.learning.test.task.tracker.store.repositories.TaskStateRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@RestController
@Transactional
public class TaskStateController {

    private final TaskStateRepository taskStateRepository;

    private final TaskStateDtoFactory taskStateDtoFactory;

    private final HelperController helperController;

    public static final String CREATE_TASK_STATE = "/api/projects/{project_id}/task-states";
    public static final String EDIT_TASK_STATE = "/api/projects/{project_id}/task-states/{task_state_id}";
    public static final String GET_TASK_STATES = "/api/projects/{project_id}/task-states";
    public static final String DELETE_TASK_STATE = "/api/projects/{project_id}/task-states/{task_state_id}";

    @GetMapping(GET_TASK_STATES)
    public List<TaskStateDto> getTaskStates(@PathVariable(name = "project_id") Long projectId) {

        ProjectEntity projectEntity = helperController.getProjectByIdOrThrowException(projectId);

        return projectEntity
                .getTaskStates()
                .stream()
                .map(taskStateDtoFactory::makeTaskStateDto)
                .collect(Collectors.toList());
    }

    @PostMapping(CREATE_TASK_STATE)
    public TaskStateDto createTaskState(
            @PathVariable(name = "project_id") Long projectId,
            @RequestParam(name = "task_state_name") String taskStateName
    ) {

        if (taskStateName.trim().isEmpty()) {
            throw new BadRequestException("Name can not be empty!");
        }

        ProjectEntity project = helperController.getProjectByIdOrThrowException(projectId);

        project.getTaskStates()
                .stream()
                .map(TaskStateEntity::getName)
                .filter(anotherTaskStateName -> anotherTaskStateName.equalsIgnoreCase(taskStateName))
                .findAny()
                .ifPresent(e -> {
                    throw new BadRequestException(String.format("Task state \"%s\" already exists!", taskStateName));
                });

        Optional<TaskStateEntity> optionalAnotherTaskState = taskStateRepository.findTaskStateEntityByRightTaskStateIdIsNullAndProjectId(projectId);

        TaskStateEntity taskState = taskStateRepository.saveAndFlush(
                TaskStateEntity.builder()
                        .name(taskStateName)
                        .project(project)
                        .build()
        );

        optionalAnotherTaskState
                .ifPresent(anotherTaskState -> {
                    taskState.setLeftTaskState(anotherTaskState);
                    anotherTaskState.setRightTaskState(taskState);
                    taskStateRepository.saveAndFlush(anotherTaskState);
                });

        final TaskStateEntity savedTaskState = taskStateRepository.saveAndFlush(taskState);

        return taskStateDtoFactory.makeTaskStateDto(savedTaskState);
    }

    @PatchMapping(EDIT_TASK_STATE)
    public TaskStateDto editTaskState(
            @PathVariable(name = "project_id") Long projectId,
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "task_state_name") String taskStateName
    ){

        if (taskStateName.trim().isEmpty()) {
            throw new BadRequestException("Name can not be empty!");
        }

        ProjectEntity project = helperController.getProjectByIdOrThrowException(projectId);

        TaskStateEntity taskState = helperController.getTaskStateByIdOrThrowException(taskStateId);

        project.getTaskStates()
                .stream()
                .map(TaskStateEntity::getName)
                .filter(anotherTaskStateName -> anotherTaskStateName.equalsIgnoreCase(taskStateName))
                .findAny()
                .ifPresent(e -> {
                    throw new BadRequestException(String.format("Task state \"%s\" already exists!", taskStateName));
                });

        taskState.setName(taskStateName);

        /*
        * //TODO: create logic to drag task state so another task states dragged automaticly,
        *    example: 1(2 on a right side), 2(1 on a left side and 3 on a right side),
        *    3(2 on a left side and 4 on a right side), 4(3 on a left side) ->
        *    1(3 on a right side), 3(1 on a left side and 2 on a right side),
        *    2(3 on a left side and 4 on a right side), 4(2 on a left side)
        * */

        return null;
    }

    @DeleteMapping(DELETE_TASK_STATE)
    public Boolean deleteTaskState(
            @PathVariable(name = "project_id") Long projectId,
            @PathVariable(name = "task_state_id") Long taskStateId
    ){
        helperController.getProjectByIdOrThrowException(projectId);

        helperController.getTaskStateByIdOrThrowException(taskStateId);

        taskStateRepository.deleteById(taskStateId);

        return true;
    }
}
