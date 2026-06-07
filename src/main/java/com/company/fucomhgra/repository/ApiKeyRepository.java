package com.company.fucomhgra.repository;

import com.company.fucomhgra.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    // Find key by its value
    Optional<ApiKey> findByKeyValue(String keyValue);

    // Find all keys for a user
    List<ApiKey> findByUserId(Long userId);

    // Find only active keys for a user
    List<ApiKey> findByUserIdAndIsActiveTrue(Long userId);

    // Check if key exists and is active
    Optional<ApiKey> findByKeyValueAndIsActiveTrue(String keyValue);
}