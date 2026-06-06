package com.company.fucomhgra.repository;

import com.company.fucomhgra.entity.ReferenceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReferenceDataRepository extends JpaRepository<ReferenceData,Long>{

    List<ReferenceData> findByTechnology(String technology);//all data for one techno

    List<ReferenceData> findByCriterion(String criterion);//all data for one techno across all techno

    Optional<ReferenceData> findByTechnologyAndCriterion(// specific value for one techno + criterion
            String technology, String criterion
    );
    // Get all technologies
    List<ReferenceData> findAllByOrderByTechnologyAsc();
}