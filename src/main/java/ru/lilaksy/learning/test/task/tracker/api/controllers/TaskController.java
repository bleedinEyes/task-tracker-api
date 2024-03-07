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

    @PatchMapping(MOVE_TASK)
    public TaskDto moveTask(@PathVariable(name = "task_id") Long taskId,
                            @RequestParam(name = "new_left_task_id", required = false) Optional<Long> optionalLeftTaskId){
        TaskEntity wannaBeChangedTask = helperController.getTaskByIdOrThrowException(taskId);

        TaskStateEntity taskState = wannaBeChangedTask.getTaskState();

        if (taskState.getTasks().size() <= 1) {
            throw new BadRequestException("Can not change position in task state with less than 2 tasks.");

            //or if we don't want to send exception for client, we can use:
            //return taskDtoFactory.makeTaskDto(wannaBeChangedTask);
        }

        Optional<Long> optionalCurrentLeftTaskId = wannaBeChangedTask.getLeftTask().map(TaskEntity::getId);
        if (optionalCurrentLeftTaskId.equals(optionalLeftTaskId)){
            return taskDtoFactory.makeTaskDto(wannaBeChangedTask);
        }

        Optional<TaskEntity> optionalLeftTask = optionalLeftTaskId
                .map(leftTaskId -> {
                    if (taskId.equals(leftTaskId)){
                        throw new BadRequestException("Left task ID is equals to changing task.");
                    }

                    TaskEntity leftTaskEntity = helperController.getTaskByIdOrThrowException(leftTaskId);

                    if (!taskState.getId().equals(leftTaskEntity.getTaskState().getId())){
                        throw new BadRequestException("Task position can not be changed within the same task state!");
                    }

                    return leftTaskEntity;
                });

        Optional<TaskEntity> optionalRightFromLeftTask;

        if (optionalLeftTask.isEmpty()){
            optionalRightFromLeftTask = taskState.getTasks()
                    .stream()
                    .filter(e -> e.getLeftTask().isEmpty())
                    .findAny();
        }
        else{
            optionalRightFromLeftTask = optionalLeftTask.get().getRightTask();
        }

        Optional<TaskEntity> optionalCurrentRightTask = wannaBeChangedTask.getRightTask();
        Optional<TaskEntity> optionalCurrentLeftTask = wannaBeChangedTask.getLeftTask();

        if (optionalCurrentLeftTask.isPresent()) {
            TaskEntity currentLeftTask = optionalCurrentLeftTask.get();
            wannaBeChangedTask.setRightTask(null);
            currentLeftTask.setRightTask(optionalCurrentRightTask.orElse(null));
            wannaBeChangedTask = taskRepository.saveAndFlush(wannaBeChangedTask);
            currentLeftTask = taskRepository.saveAndFlush(currentLeftTask);
        }

        if (optionalCurrentRightTask.isPresent()) {
            TaskEntity currentRightTask = optionalCurrentRightTask.get();
            wannaBeChangedTask.setLeftTask(null);
            currentRightTask.setLeftTask(optionalCurrentLeftTask.orElse(null));
            wannaBeChangedTask = taskRepository.saveAndFlush(wannaBeChangedTask);
            currentRightTask = taskRepository.saveAndFlush(currentRightTask);
        }

        TaskEntity settingRight = null;
        TaskEntity settingLeft = null;

        if(optionalRightFromLeftTask.isPresent()){
            TaskEntity newRightTask = optionalRightFromLeftTask.get();
            newRightTask.setLeftTask(wannaBeChangedTask);
            settingRight = newRightTask;
            newRightTask = taskRepository.saveAndFlush(newRightTask);
        }

        if(optionalLeftTask.isPresent()){
            TaskEntity newLeftTask = optionalLeftTask.get();
            newLeftTask.setRightTask(wannaBeChangedTask);
            settingLeft = newLeftTask;
            newLeftTask = taskRepository.saveAndFlush(newLeftTask);
        }

        wannaBeChangedTask.setRightTask(settingRight);
        wannaBeChangedTask.setLeftTask(settingLeft);
        wannaBeChangedTask = taskRepository.saveAndFlush(wannaBeChangedTask);

        return taskDtoFactory.makeTaskDto(wannaBeChangedTask);
    }

    @DeleteMapping(DELETE_TASK)
    public Boolean deleteTask(@PathVariable(name = "task_id") Long taskId){
        TaskEntity task = helperController.getTaskByIdOrThrowException(taskId);

        Optional<TaskEntity> optionalLeftTask = task.getLeftTask();
        Optional<TaskEntity> optionalRightTask = task.getRightTask();

        if(optionalLeftTask.isPresent() && optionalRightTask.isPresent()){
            task.setLeftTask(null);
            task.setRightTask(null);

            task = taskRepository.saveAndFlush(task);

            TaskEntity leftTask = optionalLeftTask.get();
            TaskEntity rightTask = optionalRightTask.get();

            leftTask.setRightTask(rightTask);
            rightTask.setLeftTask(leftTask);

            leftTask = taskRepository.saveAndFlush(leftTask);
            rightTask = taskRepository.saveAndFlush(rightTask);
        }
        else {
            optionalLeftTask.ifPresent(e -> {
                e.setRightTask(null);
                e = taskRepository.saveAndFlush(e);
            });

            optionalRightTask.ifPresent(e -> {
                e.setLeftTask(null);
                e = taskRepository.saveAndFlush(e);
            });
        }

        taskRepository.deleteById(taskId);

        return true;
    }
}
