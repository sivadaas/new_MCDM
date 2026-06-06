package com.company.fucomhgra.service;

import com.company.fucomhgra.entity.Analysis;
import com.company.fucomhgra.entity.Project;
import com.company.fucomhgra.entity.User;
import com.company.fucomhgra.repository.AnalysisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalysisService {

    @Autowired
    private AnalysisRepository analysisRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private AuditLogService auditLogService;

    // ─────────────────────────────────────────────
    // Save analysis result to database
    // ─────────────────────────────────────────────
    public Analysis saveAnalysis(
            User user,
            Long projectId,
            Map<String, Object> inputData,
            Map<String, Object> resultData
    ) {
        // Get project and verify ownership
        Project project = projectService.getProjectById(projectId, user);

        Analysis analysis = new Analysis();
        analysis.setProject(project);
        analysis.setInputData(inputData);
        analysis.setResultData(resultData);

        Analysis saved = analysisRepository.save(analysis);

        // Log the action
        auditLogService.log(
                user,
                "RAN_ANALYSIS",
                "Project: " + project.getName() +
                        " | Analysis ID: " + saved.getId()
        );

        return saved;
    }

    // ─────────────────────────────────────────────
    // Get all analyses for a project
    // ─────────────────────────────────────────────
    public List<Analysis> getProjectAnalyses(Long projectId, User user) {
        // Verify project ownership first
        projectService.getProjectById(projectId, user);
        return analysisRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    // ─────────────────────────────────────────────
    // Get one analysis by ID
    // ─────────────────────────────────────────────
    public Analysis getAnalysisById(Long analysisId, User user) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new RuntimeException(
                        "Analysis not found with id: " + analysisId
                ));

        // Verify ownership through project
        if (!analysis.getProject().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        return analysis;
    }

    // ─────────────────────────────────────────────
    // Get all analyses by a user across all projects
    // ─────────────────────────────────────────────
    public List<Analysis> getAllUserAnalyses(User user) {
        return analysisRepository
                .findByProjectUserIdOrderByCreatedAtDesc(user.getId());
    }

    // ─────────────────────────────────────────────
    // Delete analysis
    // ─────────────────────────────────────────────
    public void deleteAnalysis(Long analysisId, User user) {
        Analysis analysis = getAnalysisById(analysisId, user);
        analysisRepository.delete(analysis);
        auditLogService.log(
                user,
                "DELETED_ANALYSIS",
                "Analysis ID: " + analysisId
        );
    }

    // for sharable link
    public String generateShareToken(Long analysisId, User user) {
        Analysis analysis = getAnalysisById(analysisId, user);

        // Generate unique token
        String token = UUID.randomUUID().toString().replace("-", "");

        analysis.setShareToken(token);
        analysisRepository.save(analysis);

        auditLogService.log(
                user,
                "SHARED_LINK",
                "Analysis ID: " + analysisId
        );

        return token;
    }
    public Analysis getAnalysisByShareToken(String shareToken) {
        return analysisRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new RuntimeException(
                        "Invalid or expired share link"
                ));
    }
}