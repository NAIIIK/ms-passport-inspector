package com.example.repository;

import com.example.passportinspector.PassportInspectorApplication;
import com.example.passportinspector.model.type.DocumentStatus;
import com.example.passportinspector.model.type.JobStatus;
import com.example.passportinspector.model.type.JobType;
import com.example.passportinspector.model.type.PassportCheckStatus;
import com.example.passportinspector.repository.JobRepository;
import com.example.passportinspector.repository.PassportRepository;
import com.example.passportinspector.repository.entity.JobEntity;
import com.example.passportinspector.repository.entity.PassportEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PassportInspectorApplication.class)
class PassportRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("passport_inspector_test")
            .withUsername("developer")
            .withPassword("developer");

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PassportRepository passportRepository;

    @Test
    void findFirstForProcessingShouldReturnUnknownPassportFromRealPostgres() {
        UUID jobId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        jobRepository.save(JobEntity.builder()
                .jobId(jobId)
                .merchantId(merchantId)
                .jobType(JobType.SINGLE)
                .jobStatus(JobStatus.PENDING)
                .traceId("repository-test-trace-001")
                .build());

        passportRepository.saveAndFlush(PassportEntity.builder()
                .jobId(jobId)
                .merchantId(merchantId)
                .extId("client-001")
                .personLastName("ИВАНОВ")
                .personFirstName("ИВАН")
                .personMiddleName("ИВАНОВИЧ")
                .docSeriesNo("0310")
                .docNo("559835")
                .checkStatus(PassportCheckStatus.UNKNOWN)
                .documentStatus(DocumentStatus.UNKNOWN)
                .traceId("repository-test-trace-001")
                .build());

        Optional<PassportEntity> found = passportRepository.findFirstForProcessing(PassportCheckStatus.UNKNOWN.name());

        assertThat(found).isPresent();
        assertThat(found.get().getExtId()).isEqualTo("client-001");
        assertThat(passportRepository.countByCheckStatus(PassportCheckStatus.UNKNOWN)).isEqualTo(1);
        assertThat(passportRepository.countByDocumentStatus(DocumentStatus.UNKNOWN)).isEqualTo(1);
    }
}
