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
import com.example.passportinspector.model.type.DocumentStatus;
import com.example.passportinspector.model.type.PassportCheckStatus;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "passports")
public class PassportEntity extends BaseEntity {

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "ext_id", nullable = false, length = 64)
    private String extId;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_status", nullable = false, length = 20)
    private PassportCheckStatus checkStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_status", nullable = false, length = 20)
    private DocumentStatus documentStatus;

    @Column(name = "person_last_name", length = 100)
    private String personLastName;

    @Column(name = "person_first_name", length = 100)
    private String personFirstName;

    @Column(name = "person_middle_name", length = 100)
    private String personMiddleName;

    @Column(name = "doc_series_no", length = 4)
    private String docSeriesNo;

    @Column(name = "doc_no", length = 10)
    private String docNo;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "in_progress_since")
    private Instant inProgressSince;
}
