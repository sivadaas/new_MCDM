package com.company.fucomhgra.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name="reference_data")
public class ReferenceData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String technology;

    @Column(nullable = false)
    private String criterion;

    @Column(name = "min_value")
    private Double minValue;

    @Column(name = "max_value")
    private Double maxValue;

    @Column(name = "typical_value")
    private Double typicalValue;

    @Column
    private String unit;

    @Column
    private String source;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
