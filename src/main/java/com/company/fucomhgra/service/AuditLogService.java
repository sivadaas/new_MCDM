package com.company.fucomhgra.service;

import com.company.fucomhgra.entity.AuditLog;
import com.company.fucomhgra.entity.User;
import com.company.fucomhgra.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    // ─────────────────────────────────────────────
    // Log any user action
    // ─────────────────────────────────────────────
    public void log(User user, String action, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(user);
        auditLog.setAction(action);
        auditLog.setDetails(details);
        auditLogRepository.save(auditLog);
    }

    // ─────────────────────────────────────────────
    // Get all logs for a user
    // ─────────────────────────────────────────────
    public List<AuditLog> getUserLogs(User user) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    // ─────────────────────────────────────────────
    // Get all logs by action type
    // ─────────────────────────────────────────────
    public List<AuditLog> getLogsByAction(String action) {
        return auditLogRepository.findByAction(action);
    }
}