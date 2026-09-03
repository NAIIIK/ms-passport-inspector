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
import com.example.passportinspector.model.type.JobStatus;
import com.example.passportinspector.model.type.JobType;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "jobs")
public class JobEntity extends BaseEntity {

    @Column(name = "job_id", nullable = false, unique = true)
    private UUID jobId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 20)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_status", nullable = false, length = 20)
    private JobStatus jobStatus;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "in_progress_since")
    private Instant inProgressSince;
}