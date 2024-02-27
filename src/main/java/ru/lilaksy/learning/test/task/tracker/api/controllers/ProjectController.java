package ru.lilaksy.learning.test.task.tracker.api.controllers;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.lilaksy.learning.test.task.tracker.api.controllers.helpers.HelperController;
import ru.lilaksy.learning.test.task.tracker.api.dto.ProjectDto;
import ru.lilaksy.learning.test.task.tracker.api.exceptions.BadRequestException;
import ru.lilaksy.learning.test.task.tracker.api.exceptions.NotFoundException;
import ru.lilaksy.learning.test.task.tracker.api.factories.ProjectDtoFactory;
import ru.lilaksy.learning.test.task.tracker.store.entities.ProjectEntity;
import ru.lilaksy.learning.test.task.tracker.store.repositories.ProjectRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@RestController
@Transactional
public class ProjectController {

    private final ProjectDtoFactory projectDtoFactory;

    private final ProjectRepository projectRepository;

    private final HelperController helperController;

    public static final String CREATE_PROJECT = "/api/projects";
    public static final String EDIT_PROJECT = "/api/projects/{project_id}";
    public static final String FETCH_PROJECTS = "/api/projects";
    public static final String DELETE_PROJECT = "/api/projects/{project_id}";

    @GetMapping(FETCH_PROJECTS)
    public List<ProjectDto> fetchProjects(@RequestParam(value = "prefix_name", required = false) Optional<String> optionalPrefixName) {

        optionalPrefixName = optionalPrefixName.filter(prefixName -> !prefixName.trim().isEmpty());

        Stream<ProjectEntity> projectStream = optionalPrefixName
                .map(projectRepository::streamAllByNameStartsWithIgnoreCase)
                .orElseGet(projectRepository::streamAllBy);

        return projectStream
                .map(projectDtoFactory::makeProjectDto)
                .collect(Collectors.toList());
    }

    @PostMapping(CREATE_PROJECT)
    public ProjectDto createProject(@RequestParam String project_name) {

        if (project_name.trim().isEmpty()) {
            throw new BadRequestException("Name can not be empty!");
        }

        projectRepository
                .findByName(project_name)
                .ifPresent(project -> {
                    throw new BadRequestException(String.format("Project \"%s\" is already exists.", project_name));
                });

        ProjectEntity project = projectRepository.saveAndFlush(
                ProjectEntity.builder()
                        .name(project_name)
                        .build()
        );

        return projectDtoFactory.makeProjectDto(project);
    }

    @PatchMapping(EDIT_PROJECT)
    public ProjectDto editProject(@RequestParam String project_name, @PathVariable("project_id") Long projectId) {

        if (project_name.trim().isEmpty()) {
            throw new BadRequestException("Name can not be empty!");
        }

        ProjectEntity project = helperController.getProjectByIdOrThrowException(projectId);

        projectRepository
                .findByName(project_name)
                .filter(anotherProject -> !Objects.equals(anotherProject.getId(), projectId))
                .ifPresent(anotherProject -> {
                    throw new BadRequestException(String.format("Project \"%s\" is already exists.", project_name));
                });

        project.setName(project_name);

        project = projectRepository.saveAndFlush(project);

        return projectDtoFactory.makeProjectDto(project);
    }

    @DeleteMapping(DELETE_PROJECT)
    public Boolean deleteProject(@PathVariable("project_id") Long projectId) {

        helperController.getProjectByIdOrThrowException(projectId);

        projectRepository.deleteById(projectId);

        return true;
    }

}
