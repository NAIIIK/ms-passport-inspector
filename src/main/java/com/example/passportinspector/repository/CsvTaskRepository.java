package com.example.passportinspector.repository;

import com.example.passportinspector.model.type.CsvTaskStatus;
import com.example.passportinspector.repository.entity.CsvTaskEntity;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CsvTaskRepository extends JpaRepository<CsvTaskEntity, UUID> {

    @Query(value = """
            select *
            from csv_tasks
            where status = :status
            order by created_at asc
            limit 1
            for update skip locked
            """,
            nativeQuery = true
    )
    Optional<CsvTaskEntity> findFirstForProcessing(@Param("status") String status);

    long countByStatus(CsvTaskStatus status);
}
