package com.example.passportinspector.repository;

import com.example.passportinspector.repository.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    Optional<JobEntity> findByJobId(UUID jobId);

    Optional<JobEntity> findByJobIdAndMerchantId(UUID jobId, UUID merchantId);
}
