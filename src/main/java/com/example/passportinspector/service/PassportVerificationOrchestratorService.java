package com.example.passportinspector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.passportinspector.exception.CsvValidationException;
import com.example.passportinspector.exception.FileUploadException;
import com.example.passportinspector.mapper.PassportEntityMapper;
import com.example.passportinspector.model.AppConstants;
import com.example.passportinspector.model.dto.BatchCheckResultDto;
import com.example.passportinspector.model.dto.CheckInitResponseDto;
import com.example.passportinspector.model.dto.SingleCheckRequestDto;
import com.example.passportinspector.model.dto.SingleCheckResultDto;
import com.example.passportinspector.model.type.CsvTaskStatus;
import com.example.passportinspector.model.type.DocumentStatus;
import com.example.passportinspector.model.type.JobStatus;
import com.example.passportinspector.model.type.JobType;
import com.example.passportinspector.repository.CsvTaskRepository;
import com.example.passportinspector.repository.JobRepository;
import com.example.passportinspector.repository.PassportRepository;
import com.example.passportinspector.repository.entity.CsvTaskEntity;
import com.example.passportinspector.repository.entity.JobEntity;
import com.example.passportinspector.repository.entity.PassportEntity;
import com.example.passportinspector.util.CsvValidationUtil;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PassportVerificationOrchestratorService {

    private static final String JOB_NOT_FOUND_MSG = "Job not found";
    private static final String VERIFICATION_FAILED_MSG = "Verification failed";

    private final MinioService minioService;
    private final PassportEntityMapper passportEntityMapper;
    private final PassportRepository passportRepository;
    private final CsvTaskRepository csvTaskRepository;
    private final JobRepository jobRepository;

    @Transactional
    public CheckInitResponseDto singleCheckInit(SingleCheckRequestDto request, String rawMerchantId) {
        UUID jobId = UUID.randomUUID();
        UUID merchantId = UUID.fromString(rawMerchantId);

        JobEntity job = JobEntity.builder()
                .jobId(jobId)
                .merchantId(merchantId)
                .jobType(JobType.SINGLE)
                .jobStatus(JobStatus.PENDING)
                .build();

        PassportEntity passport = passportEntityMapper.fromSingleRequest(jobId, merchantId, request);

        String traceId = MDC.get("traceId");
        job.setTraceId(traceId);
        passport.setTraceId(traceId);

        jobRepository.save(job);
        passportRepository.save(passport);

        log.info("Single passport check job created. jobId={}, merchantId={}, extId={}",
                jobId, merchantId, request.getExtId());

        return CheckInitResponseDto.builder()
                .jobId(jobId)
                .checkStatus(JobStatus.IN_PROGRESS)
                .build();
    }

    public SingleCheckResultDto singleCheckResult(UUID jobId, String rawMerchantId) {
        UUID merchantId = UUID.fromString(rawMerchantId);

        return jobRepository.findByJobIdAndMerchantId(jobId, merchantId)
                .map(job -> buildSingleResult(jobId, job.getJobStatus()))
                .orElseGet(() -> SingleCheckResultDto.builder()
                        .checkStatus(JobStatus.FAILED)
                        .errorCause(JOB_NOT_FOUND_MSG)
                        .build());
    }

    @Transactional
    public CheckInitResponseDto batchCheckInit(MultipartFile file, String rawMerchantId) {
        UUID jobId = UUID.randomUUID();
        UUID merchantId = UUID.fromString(rawMerchantId);

        try {
            CsvValidationUtil.validate(file);

            String objectName = buildCsvObjectName(merchantId, jobId);
            minioService.upload(objectName, file);

            JobEntity job = JobEntity.builder()
                    .jobId(jobId)
                    .merchantId(merchantId)
                    .jobType(JobType.BATCH)
                    .jobStatus(JobStatus.PENDING)
                    .build();

            String traceId = MDC.get("traceId");
            job.setTraceId(traceId);

            CsvTaskEntity csvTask = CsvTaskEntity.builder()
                    .jobId(jobId)
                    .merchantId(merchantId)
                    .traceId(traceId)
                    .fileName(objectName)
                    .status(CsvTaskStatus.NEW)
                    .build();

            jobRepository.save(job);
            csvTaskRepository.save(csvTask);

            log.info("Batch passport check job created. jobId={}, merchantId={}, objectName={}",
                    jobId, merchantId, objectName);

            return CheckInitResponseDto.builder()
                    .jobId(jobId)
                    .checkStatus(JobStatus.IN_PROGRESS)
                    .build();

        } catch (CsvValidationException | FileUploadException e) {
            log.warn("Batch passport check job failed on init. jobId={}, merchantId={}, reason={}",
                    jobId, merchantId, e.getMessage());

            return CheckInitResponseDto.builder()
                    .jobId(jobId)
                    .checkStatus(JobStatus.FAILED)
                    .errorCause(e.getMessage())
                    .build();
        }
    }

    public BatchCheckResultDto batchCheckResult(UUID jobId, String rawMerchantId) {
        UUID merchantId = UUID.fromString(rawMerchantId);

        return jobRepository.findByJobIdAndMerchantId(jobId, merchantId)
                .map(job -> buildBatchResult(jobId, merchantId, job.getJobStatus()))
                .orElseGet(() -> BatchCheckResultDto.builder()
                        .checkStatus(JobStatus.FAILED)
                        .errorCause(JOB_NOT_FOUND_MSG)
                        .data(List.of())
                        .build());
    }

    private SingleCheckResultDto buildSingleResult(UUID jobId, JobStatus jobStatus) {
        if (jobStatus == JobStatus.COMPLETED) {
            return passportRepository.findFirstByJobId(jobId)
                    .map(passport -> {
                        if (passport.getDocumentStatus() == DocumentStatus.INVALID) {
                            return SingleCheckResultDto.builder()
                                    .checkStatus(JobStatus.COMPLETED)
                                    .extId(passport.getExtId())
                                    .build();
                        }

                        return SingleCheckResultDto.builder()
                                .checkStatus(JobStatus.COMPLETED)
                                .build();
                    })
                    .orElseGet(() -> SingleCheckResultDto.builder()
                            .checkStatus(JobStatus.FAILED)
                            .errorCause("Job data missing")
                            .build());
        }

        if (jobStatus == JobStatus.FAILED) {
            return SingleCheckResultDto.builder()
                    .checkStatus(JobStatus.FAILED)
                    .errorCause(VERIFICATION_FAILED_MSG)
                    .build();
        }

        return SingleCheckResultDto.builder()
                .checkStatus(JobStatus.IN_PROGRESS)
                .build();
    }

    private BatchCheckResultDto buildBatchResult(UUID jobId, UUID merchantId, JobStatus jobStatus) {
        if (jobStatus == JobStatus.COMPLETED) {
            List<String> invalidExtIds = passportRepository
                    .findByJobIdAndMerchantIdAndDocumentStatus(jobId, merchantId, DocumentStatus.INVALID)
                    .stream()
                    .map(PassportEntity::getExtId)
                    .toList();

            return BatchCheckResultDto.builder()
                    .checkStatus(JobStatus.COMPLETED)
                    .data(invalidExtIds)
                    .build();
        }

        if (jobStatus == JobStatus.FAILED) {
            return BatchCheckResultDto.builder()
                    .checkStatus(JobStatus.FAILED)
                    .errorCause(VERIFICATION_FAILED_MSG)
                    .data(List.of())
                    .build();
        }

        return BatchCheckResultDto.builder()
                .checkStatus(JobStatus.IN_PROGRESS)
                .data(List.of())
                .build();
    }

    private String buildCsvObjectName(UUID merchantId, UUID jobId) {
        return "batch/" + merchantId + "/" + jobId + AppConstants.CSV_EXTENSION;
    }
}
