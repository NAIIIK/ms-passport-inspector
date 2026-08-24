package com.example.passportinspector.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import com.example.passportinspector.model.type.CsvTaskStatus;
import com.example.passportinspector.model.type.DocumentStatus;
import com.example.passportinspector.model.type.JobStatus;
import com.example.passportinspector.model.type.PassportCheckStatus;
import com.example.passportinspector.repository.CsvTaskRepository;
import com.example.passportinspector.repository.JobRepository;
import com.example.passportinspector.repository.PassportRepository;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
public class PassportMetricsConfig {

    private final MeterRegistry meterRegistry;
    private final JobRepository jobRepository;
    private final PassportRepository passportRepository;
    private final CsvTaskRepository csvTaskRepository;

    @PostConstruct
    public void registerBusinessGauges() {
        for (JobStatus status : JobStatus.values()) {
            Gauge.builder("passport.jobs", jobRepository, repository -> repository.countByJobStatus(status))
                    .description("Current number of passport jobs by status")
                    .tag("status", status.name())
                    .register(meterRegistry);
        }

        for (PassportCheckStatus status : PassportCheckStatus.values()) {
            Gauge.builder("passport.checks", passportRepository, repository -> repository.countByCheckStatus(status))
                    .description("Current number of passport checks by processing status")
                    .tag("status", status.name())
                    .register(meterRegistry);
        }

        for (DocumentStatus status : DocumentStatus.values()) {
            Gauge.builder("passport.documents", passportRepository, repository -> repository.countByDocumentStatus(status))
                    .description("Current number of passport checks by document status")
                    .tag("status", status.name())
                    .register(meterRegistry);
        }

        for (CsvTaskStatus status : CsvTaskStatus.values()) {
            Gauge.builder("passport.csv.tasks", csvTaskRepository, repository -> repository.countByStatus(status))
                    .description("Current number of CSV tasks by status")
                    .tag("status", status.name())
                    .register(meterRegistry);
        }
    }
}
