package com.company.fucomhgra.repository;

import com.company.fucomhgra.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByUserId(Long userId);
    List<Project> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByNameAndUserId(String name, Long userId);
}