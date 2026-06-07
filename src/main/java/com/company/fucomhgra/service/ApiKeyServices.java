package com.company.fucomhgra.service;

import com.company.fucomhgra.entity.ApiKey;
import com.company.fucomhgra.entity.User;
import com.company.fucomhgra.repository.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ApiKeyServices {

    @Autowired private ApiKeyRepository apiKeyRepository;
    @Autowired private AuditLogService auditLogService;

    // ─────────────────────────────────────────────
    // Generate a new API key for a user
    // ─────────────────────────────────────────────
    public ApiKey generateApiKey(User user, String keyName) {

        // Generate unique key: sk-wwr-{random}
        String keyValue = "sk-wwr-" + UUID.randomUUID()
                .toString().replace("-", "");

        ApiKey apiKey = new ApiKey();
        apiKey.setUser(user);
        apiKey.setKeyValue(keyValue);
        apiKey.setName(keyName != null ? keyName : "Default Key");
        apiKey.setIsActive(true);

        ApiKey saved = apiKeyRepository.save(apiKey);

        auditLogService.log(
                user,
                "GENERATED_API_KEY",
                "Key name: " + apiKey.getName()
        );

        return saved;
    }

    // ─────────────────────────────────────────────
    // Get all API keys for a user
    // ─────────────────────────────────────────────
    public List<ApiKey> getUserApiKeys(User user) {
        return apiKeyRepository.findByUserId(user.getId());
    }

    // ─────────────────────────────────────────────
    // Validate API key and return user
    // ─────────────────────────────────────────────
    public User validateApiKey(String keyValue) {
        ApiKey apiKey = apiKeyRepository
                .findByKeyValueAndIsActiveTrue(keyValue)
                .orElseThrow(() -> new RuntimeException(
                        "Invalid or inactive API key"
                ));

        // Update last used timestamp
        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);

        return apiKey.getUser();
    }

    // ─────────────────────────────────────────────
    // Revoke (deactivate) an API key
    // ─────────────────────────────────────────────
    public void revokeApiKey(Long keyId, User user) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new RuntimeException(
                        "API key not found"
                ));

        // Verify ownership
        if (!apiKey.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        apiKey.setIsActive(false);
        apiKeyRepository.save(apiKey);

        auditLogService.log(
                user,
                "REVOKED_API_KEY",
                "Key name: " + apiKey.getName()
        );
    }

    // ─────────────────────────────────────────────
    // Delete an API key permanently
    // ─────────────────────────────────────────────
    public void deleteApiKey(Long keyId, User user) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new RuntimeException(
                        "API key not found"
                ));

        if (!apiKey.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        apiKeyRepository.delete(apiKey);

        auditLogService.log(
                user,
                "DELETED_API_KEY",
                "Key name: " + apiKey.getName()
        );
    }
}