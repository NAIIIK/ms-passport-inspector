package com.example.passportinspector.repository;

import com.example.passportinspector.model.type.DocumentStatus;
import com.example.passportinspector.model.type.PassportCheckStatus;
import com.example.passportinspector.repository.entity.PassportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PassportRepository extends JpaRepository<PassportEntity, UUID> {

    @Query(value = """
            select *
            from passports
            where check_status = :checkStatus
            order by created_at asc
            limit 1
            for update skip locked
            """,
            nativeQuery = true
    )
    Optional<PassportEntity> findFirstForProcessing(@Param("checkStatus") String checkStatus);

    Optional<PassportEntity> findFirstByJobId(UUID jobId);

    List<PassportEntity> findByJobIdAndMerchantIdAndDocumentStatus(
            UUID jobId,
            UUID merchantId,
            DocumentStatus documentStatus
    );

    long countByJobId(UUID jobId);

    long countByJobIdAndCheckStatusNot(UUID jobId, PassportCheckStatus checkStatus);
}