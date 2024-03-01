package ru.lilaksy.learning.test.task.tracker.api.controllers;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.lilaksy.learning.test.task.tracker.api.controllers.helpers.HelperController;
import ru.lilaksy.learning.test.task.tracker.api.dto.TaskStateDto;
import ru.lilaksy.learning.test.task.tracker.api.exceptions.BadRequestException;
import ru.lilaksy.learning.test.task.tracker.api.factories.TaskStateDtoFactory;
import ru.lilaksy.learning.test.task.tracker.store.entities.ProjectEntity;
import ru.lilaksy.learning.test.task.tracker.store.entities.TaskStateEntity;
import ru.lilaksy.learning.test.task.tracker.store.repositories.TaskStateRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@Transactional
public class TaskStateController {

    private final TaskStateRepository taskStateRepository;

    private final TaskStateDtoFactory taskStateDtoFactory;

    private final HelperController helperController;

    public static final String CREATE_TASK_STATE = "/api/projects/{project_id}/task-states";
    public static final String EDIT_TASK_STATE = "/api/task-states/{task_state_id}";
    public static final String MOVE_TASK_STATE = "/api/task-states/{task_state_id}/move";
    public static final String GET_TASK_STATES = "/api/projects/{project_id}/task-states";
    public static final String DELETE_TASK_STATE = "/api/task-states/{task_state_id}";

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
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "task_state_name") String taskStateName
    ){

        if (taskStateName.trim().isEmpty()) {
            throw new BadRequestException("Name can not be empty!");
        }

        TaskStateEntity taskState = helperController.getTaskStateByIdOrThrowException(taskStateId);

        taskStateRepository
                .findTaskStateEntityByProjectIdAndNameContainsIgnoreCase(taskState.getProject().getId(), taskStateName)
                .filter(anotherTaskState -> !anotherTaskState.getId().equals(taskStateId))
                .ifPresent(e -> {
                    throw new BadRequestException(String.format("Task state \"%s\" already exists!", taskStateName));
                });

        taskState.setName(taskStateName);

        taskState = taskStateRepository.saveAndFlush(taskState);

        return taskStateDtoFactory.makeTaskStateDto(taskState);
    }

    @PatchMapping(MOVE_TASK_STATE)
    public TaskStateDto moveTaskState(
            @PathVariable(name = "task_state_id") Long taskStateId,
            @RequestParam(name = "new_left_task_state_id", required = false) Optional<Long> optionalLeftTaskStateId
    ){
        TaskStateEntity wannaBeChangedTaskState = helperController.getTaskStateByIdOrThrowException(taskStateId);

        ProjectEntity project = wannaBeChangedTaskState.getProject();

        if (project.getTaskStates().size() <= 1) {
            throw new BadRequestException("Can not change position in project with less than 2 task states.");

            //or if we don't want to show exception for client, we can use:
            //return taskStateDtoFactory.makeTaskStateDto(wannaBeChangedTaskState);
        }

        Optional<TaskStateEntity> currentRightTaskState = wannaBeChangedTaskState.getRightTaskState();

        Optional<TaskStateEntity> currentLeftTaskState = wannaBeChangedTaskState.getLeftTaskState();

        Optional<TaskStateEntity> leftTaskState = optionalLeftTaskStateId
                .map(leftTaskStateId -> {
                    if (taskStateId.equals(leftTaskStateId)){
                        throw new BadRequestException("Left task state ID is equals to changing task state.");
                    }

                    TaskStateEntity leftTaskStateEntity = helperController.getTaskStateByIdOrThrowException(leftTaskStateId);

                    if (!project.getId().equals(leftTaskStateEntity.getProject().getId())){
                        throw new BadRequestException("Task state position can not be changed within the same project!");
                    }

                    return leftTaskStateEntity;
                });

        Optional<TaskStateEntity> rightFromLeftTaskState;

        if (leftTaskState.isEmpty()){
            rightFromLeftTaskState = project.getTaskStates()
                    .stream()
                    .filter(e -> e.getLeftTaskState().isEmpty())
                    .findAny();
        }
        else{
            rightFromLeftTaskState = leftTaskState.get().getRightTaskState();
        }

        currentLeftTaskState.ifPresent(it -> {
            it.setRightTaskState(currentRightTaskState.orElse(null));
            taskStateRepository.saveAndFlush(it);
        });
        currentRightTaskState.ifPresent(it -> {
            it.setLeftTaskState(currentLeftTaskState.orElse(null));
            taskStateRepository.saveAndFlush(it);
        });

        if(rightFromLeftTaskState.isPresent()){
            rightFromLeftTaskState.get().setLeftTaskState(wannaBeChangedTaskState);
            wannaBeChangedTaskState.setRightTaskState(rightFromLeftTaskState.get());
        }
        else{
            wannaBeChangedTaskState.setRightTaskState(null);
        }

        if(leftTaskState.isPresent()){
            leftTaskState.get().setRightTaskState(wannaBeChangedTaskState);
            wannaBeChangedTaskState.setLeftTaskState(leftTaskState.get());
        }
        else{
            wannaBeChangedTaskState.setLeftTaskState(null);
        }

        wannaBeChangedTaskState = taskStateRepository.saveAndFlush(wannaBeChangedTaskState);
        leftTaskState.ifPresent(taskStateRepository::saveAndFlush);
        rightFromLeftTaskState.ifPresent(taskStateRepository::saveAndFlush);

        return taskStateDtoFactory.makeTaskStateDto(wannaBeChangedTaskState);
    }

    @DeleteMapping(DELETE_TASK_STATE)
    public Boolean deleteTaskState(@PathVariable(name = "task_state_id") Long taskStateId){

        helperController.getTaskStateByIdOrThrowException(taskStateId);

        taskStateRepository.deleteById(taskStateId);

        return true;
    }
}
