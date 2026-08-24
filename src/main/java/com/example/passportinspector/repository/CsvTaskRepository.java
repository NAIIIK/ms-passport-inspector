package com.example.passportinspector.repository;

import com.example.passportinspector.model.type.CsvTaskStatus;
import com.example.passportinspector.repository.entity.CsvTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CsvTaskRepository extends JpaRepository<CsvTaskEntity, UUID> {
    Optional<CsvTaskEntity> findFirstByStatusOrderByCreatedAtAsc(CsvTaskStatus status);
}
