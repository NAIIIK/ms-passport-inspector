package com.example.passportinspector.service;

import com.example.passportinspector.mapper.PassportEntityMapper;
import com.example.passportinspector.model.dto.CheckInitResponseDto;
import com.example.passportinspector.model.dto.SingleCheckRequestDto;
import com.example.passportinspector.model.type.DocumentStatus;
import com.example.passportinspector.model.type.JobStatus;
import com.example.passportinspector.model.type.PassportCheckStatus;
import com.example.passportinspector.repository.CsvTaskRepository;
import com.example.passportinspector.repository.JobRepository;
import com.example.passportinspector.repository.PassportRepository;
import com.example.passportinspector.repository.entity.CsvTaskEntity;
import com.example.passportinspector.repository.entity.JobEntity;
import com.example.passportinspector.repository.entity.PassportEntity;
import com.example.passportinspector.model.dto.BatchCheckResultDto;
import com.example.passportinspector.model.dto.SingleCheckResultDto;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassportVerificationOrchestratorServiceTest {

    private static final String MERCHANT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final UUID MERCHANT_UUID = UUID.fromString(MERCHANT_ID);
    private static final String TRACE_ID = "service-test-trace-001";

    @Mock
    private MinioService minioService;

    @Mock
    private PassportEntityMapper passportEntityMapper;

    @Mock
    private PassportRepository passportRepository;

    @Mock
    private CsvTaskRepository csvTaskRepository;

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private PassportVerificationOrchestratorService service;

    @Test
    void singleCheckInitShouldCreateJobAndPassport() {
        SingleCheckRequestDto request = validSingleRequest();

        when(passportEntityMapper.fromSingleRequest(any(UUID.class), eq(MERCHANT_UUID), eq(request)))
                .thenAnswer(invocation -> {
                    UUID capturedJobId = invocation.getArgument(0);
                    return PassportEntity.builder()
                            .jobId(capturedJobId)
                            .merchantId(MERCHANT_UUID)
                            .extId(request.getExtId())
                            .checkStatus(PassportCheckStatus.UNKNOWN)
                            .documentStatus(DocumentStatus.UNKNOWN)
                            .build();
                });

        CheckInitResponseDto response;
        try (MDC.MDCCloseable ignored = MDC.putCloseable("traceId", TRACE_ID)) {
            response = service.singleCheckInit(request, MERCHANT_ID);
        }

        assertThat(response.getJobId()).isNotNull();
        assertThat(response.getCheckStatus()).isEqualTo(JobStatus.IN_PROGRESS);
        assertThat(response.getErrorCause()).isNull();

        ArgumentCaptor<JobEntity> jobCaptor = ArgumentCaptor.forClass(JobEntity.class);
        ArgumentCaptor<PassportEntity> passportCaptor = ArgumentCaptor.forClass(PassportEntity.class);

        verify(jobRepository).save(jobCaptor.capture());
        verify(passportRepository).save(passportCaptor.capture());

        assertThat(jobCaptor.getValue().getMerchantId()).isEqualTo(MERCHANT_UUID);
        assertThat(jobCaptor.getValue().getJobStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(jobCaptor.getValue().getTraceId()).isEqualTo(TRACE_ID);
        assertThat(passportCaptor.getValue().getTraceId()).isEqualTo(TRACE_ID);
        assertThat(passportCaptor.getValue().getJobId()).isEqualTo(jobCaptor.getValue().getJobId());
    }

    @Test
    void batchCheckInitShouldValidateCsvUploadFileAndCreateCsvTask() {
        MockMultipartFile file = new MockMultipartFile(
                "inputFile",
                "passports.csv",
                "text/csv",
                """
                        id,person_last_name,person_first_name,person_middle_name,doc_series_no,doc_no
                        client-001,Иванов,Иван,Иванович,0310,559835
                        """.getBytes(StandardCharsets.UTF_8));

        CheckInitResponseDto response;
        try (MDC.MDCCloseable ignored = MDC.putCloseable("traceId", TRACE_ID)) {
            response = service.batchCheckInit(file, MERCHANT_ID);
        }

        assertThat(response.getJobId()).isNotNull();
        assertThat(response.getCheckStatus()).isEqualTo(JobStatus.IN_PROGRESS);
        assertThat(response.getErrorCause()).isNull();

        ArgumentCaptor<JobEntity> jobCaptor = ArgumentCaptor.forClass(JobEntity.class);
        ArgumentCaptor<CsvTaskEntity> csvTaskCaptor = ArgumentCaptor.forClass(CsvTaskEntity.class);

        verify(minioService).upload(any(String.class), eq(file));
        verify(jobRepository).save(jobCaptor.capture());
        verify(csvTaskRepository).save(csvTaskCaptor.capture());

        assertThat(jobCaptor.getValue().getJobStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(jobCaptor.getValue().getTraceId()).isEqualTo(TRACE_ID);
        assertThat(csvTaskCaptor.getValue().getStatus().name()).isEqualTo("NEW");
        assertThat(csvTaskCaptor.getValue().getTraceId()).isEqualTo(TRACE_ID);
        assertThat(csvTaskCaptor.getValue().getFileName()).startsWith("batch/" + MERCHANT_ID + "/");
    }

    @Test
    void batchCheckInitShouldReturnFailedWhenCsvIsInvalid() {
        MockMultipartFile file = new MockMultipartFile(
                "inputFile",
                "passports.txt",
                "text/plain",
                "not a csv".getBytes(StandardCharsets.UTF_8));

        CheckInitResponseDto response = service.batchCheckInit(file, MERCHANT_ID);

        assertThat(response.getJobId()).isNotNull();
        assertThat(response.getCheckStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(response.getErrorCause()).contains("Only .csv files are allowed");

        verifyNoInteractions(minioService, jobRepository, csvTaskRepository, passportRepository);
    }

    @Test
    void singleCheckResultShouldReturnJobNotFoundWhenJobDoesNotExist() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findByJobIdAndMerchantId(jobId, MERCHANT_UUID)).thenReturn(Optional.empty());

        SingleCheckResultDto result = service.singleCheckResult(jobId, MERCHANT_ID);

        assertThat(result.getCheckStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(result.getErrorCause()).isEqualTo("Job not found");
    }

    @Test
    void singleCheckResultShouldReturnInProgressWhenJobIsPending() {
        UUID jobId = UUID.randomUUID();
        JobEntity job = JobEntity.builder().jobId(jobId).jobStatus(JobStatus.PENDING).build();
        when(jobRepository.findByJobIdAndMerchantId(jobId, MERCHANT_UUID)).thenReturn(Optional.of(job));

        SingleCheckResultDto result = service.singleCheckResult(jobId, MERCHANT_ID);

        assertThat(result.getCheckStatus()).isEqualTo(JobStatus.IN_PROGRESS);
        assertThat(result.getErrorCause()).isNull();
    }

    @Test
    void singleCheckResultShouldReturnVerificationFailedWhenJobStatusIsFailed() {
        UUID jobId = UUID.randomUUID();
        JobEntity job = JobEntity.builder().jobId(jobId).jobStatus(JobStatus.FAILED).build();
        when(jobRepository.findByJobIdAndMerchantId(jobId, MERCHANT_UUID)).thenReturn(Optional.of(job));

        SingleCheckResultDto result = service.singleCheckResult(jobId, MERCHANT_ID);

        assertThat(result.getCheckStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(result.getErrorCause()).isEqualTo("Verification failed");
    }

    @Test
    void singleCheckResultShouldReturnJobDataMissingWhenCompletedButPassportMissing() {
        UUID jobId = UUID.randomUUID();
        JobEntity job = JobEntity.builder().jobId(jobId).jobStatus(JobStatus.COMPLETED).build();
        when(jobRepository.findByJobIdAndMerchantId(jobId, MERCHANT_UUID)).thenReturn(Optional.of(job));
        when(passportRepository.findFirstByJobId(jobId)).thenReturn(Optional.empty());

        SingleCheckResultDto result = service.singleCheckResult(jobId, MERCHANT_ID);

        assertThat(result.getCheckStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(result.getErrorCause()).isEqualTo("Job data missing");
    }

    @Test
    void singleCheckResultShouldReturnCompletedWithExtIdWhenDocumentInvalid() {
        UUID jobId = UUID.randomUUID();
        JobEntity job = JobEntity.builder().jobId(jobId).jobStatus(JobStatus.COMPLETED).build();
        PassportEntity passport = PassportEntity.builder()
                .jobId(jobId)
                .extId("client-001")
                .documentStatus(DocumentStatus.INVALID)
                .build();
        when(jobRepository.findByJobIdAndMerchantId(jobId, MERCHANT_UUID)).thenReturn(Optional.of(job));
        when(passportRepository.findFirstByJobId(jobId)).thenReturn(Optional.of(passport));

        SingleCheckResultDto result = service.singleCheckResult(jobId, MERCHANT_ID);

        assertThat(result.getCheckStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(result.getExtId()).isEqualTo("client-001");
        assertThat(result.getErrorCause()).isNull();
    }

    @Test
    void singleCheckResultShouldReturnCompletedWithoutExtIdWhenDocumentValid() {
        UUID jobId = UUID.randomUUID();
        JobEntity job = JobEntity.builder().jobId(jobId).jobStatus(JobStatus.COMPLETED).build();
        PassportEntity passport = PassportEntity.builder()
                .jobId(jobId)
                .extId("client-001")
                .documentStatus(DocumentStatus.VALID)
                .build();
        when(jobRepository.findByJobIdAndMerchantId(jobId, MERCHANT_UUID)).thenReturn(Optional.of(job));
        when(passportRepository.findFirstByJobId(jobId)).thenReturn(Optional.of(passport));

        SingleCheckResultDto result = service.singleCheckResult(jobId, MERCHANT_ID);

        assertThat(result.getCheckStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(result.getExtId()).isNull();
    }

    @Test
    void batchCheckResultShouldReturnJobNotFoundWhenJobDoesNotExist() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findByJobIdAndMerchantId(jobId, MERCHANT_UUID)).thenReturn(Optional.empty());

        BatchCheckResultDto result = service.batchCheckResult(jobId, MERCHANT_ID);

        assertThat(result.getCheckStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(result.getErrorCause()).isEqualTo("Job not found");
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void batchCheckResultShouldReturnVerificationFailedWhenJobStatusIsFailed() {
        UUID jobId = UUID.randomUUID();
        JobEntity job = JobEntity.builder().jobId(jobId).jobStatus(JobStatus.FAILED).build();
        when(jobRepository.findByJobIdAndMerchantId(jobId, MERCHANT_UUID)).thenReturn(Optional.of(job));

        BatchCheckResultDto result = service.batchCheckResult(jobId, MERCHANT_ID);

        assertThat(result.getCheckStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(result.getErrorCause()).isEqualTo("Verification failed");
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void batchCheckResultShouldReturnInvalidExtIdsWhenCompleted() {
        UUID jobId = UUID.randomUUID();
        JobEntity job = JobEntity.builder().jobId(jobId).jobStatus(JobStatus.COMPLETED).build();
        PassportEntity invalidPassport = PassportEntity.builder()
                .jobId(jobId)
                .extId("client-002")
                .documentStatus(DocumentStatus.INVALID)
                .build();
        when(jobRepository.findByJobIdAndMerchantId(jobId, MERCHANT_UUID)).thenReturn(Optional.of(job));
        when(passportRepository.findByJobIdAndMerchantIdAndDocumentStatus(jobId, MERCHANT_UUID, DocumentStatus.INVALID))
                .thenReturn(List.of(invalidPassport));

        BatchCheckResultDto result = service.batchCheckResult(jobId, MERCHANT_ID);

        assertThat(result.getCheckStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(result.getData()).containsExactly("client-002");
    }

    private SingleCheckRequestDto validSingleRequest() {
        return SingleCheckRequestDto.builder()
                .extId("client-001")
                .personLastName("Иванов")
                .personFirstName("Иван")
                .personMiddleName("Иванович")
                .docSeriesNo("0310")
                .docNo("559835")
                .build();
    }
}
