package com.company.fucomhgra.controller;

import com.company.fucomhgra.entity.Project;
import com.company.fucomhgra.entity.User;
import com.company.fucomhgra.service.AuthService;
import com.company.fucomhgra.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private AuthService authService;

    // ─────────────────────────────────────────────
    // Create Project
    // POST /api/projects
    // ─────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> createProject(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {
        try {
            User user = getUserFromHeader(authHeader);
            String name        = body.get("name");
            String description = body.get("description");

            if (name == null || name.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", 400,
                        "message", "Project name is required"
                ));
            }

            Project project = projectService.createProject(user, name, description);

            return ResponseEntity.ok(Map.of(
                    "id",          project.getId(),
                    "name",        project.getName(),
                    "description", project.getDescription() != null ? project.getDescription() : "",
                    "createdAt",   project.getCreatedAt().toString()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", e.getMessage()
            ));
        }
    }

    // ─────────────────────────────────────────────
    // Get All Projects
    // GET /api/projects
    // ─────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> getAllProjects(
            @RequestHeader("Authorization") String authHeader) {
        try {
            User user = getUserFromHeader(authHeader);
            List<Project> projects = projectService.getUserProjects(user);

            List<Map<String, Object>> response = projects.stream()
                    .map(p -> Map.of(
                            "id",          (Object) p.getId(),
                            "name",        p.getName(),
                            "description", p.getDescription() != null ? p.getDescription() : "",
                            "createdAt",   p.getCreatedAt().toString()
                    ))
                    .toList();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", e.getMessage()
            ));
        }
    }

    // ─────────────────────────────────────────────
    // Get One Project
    // GET /api/projects/{id}
    // ─────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getProject(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        try {
            User user = getUserFromHeader(authHeader);
            Project project = projectService.getProjectById(id, user);

            return ResponseEntity.ok(Map.of(
                    "id",          project.getId(),
                    "name",        project.getName(),
                    "description", project.getDescription() != null ? project.getDescription() : "",
                    "createdAt",   project.getCreatedAt().toString()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "status", 403,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", e.getMessage()
            ));
        }
    }

    // ─────────────────────────────────────────────
    // Delete Project
    // DELETE /api/projects/{id}
    // ─────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        try {
            User user = getUserFromHeader(authHeader);
            projectService.deleteProject(id, user);

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Project deleted successfully"
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "status", 403,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", e.getMessage()
            ));
        }
    }

    // ─────────────────────────────────────────────
    // Helper — extract user from JWT token
    // ─────────────────────────────────────────────
    private User getUserFromHeader(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return authService.getUserFromToken(token);
    }
}