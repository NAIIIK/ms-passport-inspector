package com.example.passportinspector.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.passportinspector.model.type.CsvTaskStatus;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "csv_tasks")
public class CsvTaskEntity extends BaseEntity {
    /**
     * Публичный идентификатор batch job-а.
     */
    @Column(name = "job_id", nullable = false, unique = true)
    private UUID jobId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    /**
     * Object name файла в MinIO.
     */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CsvTaskStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "in_progress_since")
    private Instant inProgressSince;
}