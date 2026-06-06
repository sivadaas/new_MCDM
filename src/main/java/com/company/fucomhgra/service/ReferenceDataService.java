package com.company.fucomhgra.service;

import com.company.fucomhgra.entity.ReferenceData;
import com.company.fucomhgra.repository.ReferenceDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
@Service
public class ReferenceDataService {
    @Autowired
    private ReferenceDataRepository referenceDataRepository;
    // we need to create a full decision matrix with the help of reference data table

    public Map<String,Map<String,Double>> getDefaultDecisionMatrix(){
        List<ReferenceData> allData = referenceDataRepository.findAllByOrderByTechnologyAsc();

        Map<String, Map<String, Double>> matrix = new LinkedHashMap<>();

        for (ReferenceData data : allData) {
            matrix
                    .computeIfAbsent(data.getTechnology(), k -> new LinkedHashMap<>())
                    .put(data.getCriterion(), data.getTypicalValue());
        }
        return matrix;
    }
    // GET all data for one techno
    public List<ReferenceData> getTechnologyData(String technology) {
        return referenceDataRepository.findByTechnology(technology);
    }
    // getting all data grouped by technology
    public Map<String, Map<String, Object>> getAllReferenceData() {
        List<ReferenceData> allData = referenceDataRepository.findAllByOrderByTechnologyAsc();

        Map<String, Map<String, Object>> result = new LinkedHashMap<>();

        for (ReferenceData data : allData) {
            result.computeIfAbsent(data.getTechnology(), k -> new LinkedHashMap<>())
                    .put(data.getCriterion(), Map.of(
                            "minValue",     data.getMinValue(),
                            "maxValue",     data.getMaxValue(),
                            "typicalValue", data.getTypicalValue(),
                            "unit",         data.getUnit(),
                            "source",       data.getSource()
                    ));
        }
        return result;
    }

    //get typical value for specific techno_+ criterion
    public Double getTypicalValue(String technology, String criterion) {
        return referenceDataRepository
                .findByTechnologyAndCriterion(technology, criterion)
                .map(ReferenceData::getTypicalValue)
                .orElseThrow(() -> new RuntimeException(
                        "No reference data found for " + technology + " / " + criterion
                ));
    }
}
