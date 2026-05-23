package com.company.fucomhgra.service;

import com.company.fucomhgra.entity.Project;
import com.company.fucomhgra.entity.User;
import com.company.fucomhgra.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AuditLogService auditLogService;

    // ─────────────────────────────────────────────
    // Create a new project
    // ─────────────────────────────────────────────
    public Project createProject(User user, String name, String description) {

        // Check if project with same name already exists for this user
        if (projectRepository.existsByNameAndUserId(name, user.getId())) {
            throw new IllegalArgumentException(
                    "Project '" + name + "' already exists"
            );
        }

        Project project = new Project();
        project.setUser(user);
        project.setName(name);
        project.setDescription(description);

        Project saved = projectRepository.save(project);

        // Log the action
        auditLogService.log(user, "CREATED_PROJECT", "Project: " + name);

        return saved;
    }

    // ─────────────────────────────────────────────
    // Get all projects for a user
    // ─────────────────────────────────────────────
    public List<Project> getUserProjects(User user) {
        return projectRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    // ─────────────────────────────────────────────
    // Get one project by ID
    // ─────────────────────────────────────────────
    public Project getProjectById(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException(
                        "Project not found with id: " + projectId
                ));

        // Make sure project belongs to this user
        if (!project.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        return project;
    }

    // ─────────────────────────────────────────────
    // Delete a project
    // ─────────────────────────────────────────────
    public void deleteProject(Long projectId, User user) {
        Project project = getProjectById(projectId, user);
        projectRepository.delete(project);
        auditLogService.log(user, "DELETED_PROJECT", "Project: " + project.getName());
    }
}