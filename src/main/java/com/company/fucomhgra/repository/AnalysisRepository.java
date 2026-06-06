package com.company.fucomhgra.repository;

import com.company.fucomhgra.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Optional<Analysis> findByShareToken(String shareToken);

    List<Analysis> findByProjectId(Long projectId);
    List<Analysis> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    List<Analysis> findByProjectUserIdOrderByCreatedAtDesc(Long userId);
}
