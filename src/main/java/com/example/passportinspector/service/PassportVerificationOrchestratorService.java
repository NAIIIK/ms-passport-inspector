package com.example.passportinspector.service;

import com.example.passportinspector.exception.CsvValidationException;
import com.example.passportinspector.exception.FileUploadException;
import com.example.passportinspector.model.AppConstants;
import com.example.passportinspector.model.dto.BatchCheckResultDto;
import com.example.passportinspector.model.dto.CheckInitResponseDto;
import com.example.passportinspector.model.dto.SingleCheckRequestDto;
import com.example.passportinspector.model.dto.SingleCheckResultDto;
import com.example.passportinspector.model.type.CheckStatus;
import com.example.passportinspector.util.CsvValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PassportVerificationOrchestratorService {

    private final MinioService minioService;

    private final Map<UUID, SingleCheckState> singleChecks = new ConcurrentHashMap<>();
    private final Map<UUID, BatchCheckState> batchChecks = new ConcurrentHashMap<>();

    public CheckInitResponseDto singleCheckInit(SingleCheckRequestDto request, String rawMerchantId) {

        UUID jobId = UUID.randomUUID();
        UUID merchantId = UUID.fromString(rawMerchantId);

        singleChecks.put(jobId, new SingleCheckState(
                merchantId,
                request.getExtId(),
                request.getDocNo(),
                Instant.now()
        ));

        log.info(
                "Single check accepted. jobId={}, merchantId={}, extId={}",
                jobId,
                merchantId,
                request.getExtId()
        );

        return CheckInitResponseDto.builder()
                .jobId(jobId)
                .checkStatus(CheckStatus.IN_PROGRESS)
                .build();
    }

    public SingleCheckResultDto singleCheckResult(UUID jobId, String rawMerchantId) {
        UUID merchantId = UUID.fromString(rawMerchantId);
        SingleCheckState state = singleChecks.get(jobId);

        if (state == null || !state.merchantId().equals(merchantId)) {
            return SingleCheckResultDto.builder()
                    .checkStatus(CheckStatus.FAILED)
                    .build();
        }

        return SingleCheckResultDto.builder()
                .checkStatus(CheckStatus.IN_PROGRESS)
                .build();
    }

    public CheckInitResponseDto batchCheckInit(MultipartFile file, String rawMerchantId) {
        UUID jobId = UUID.randomUUID();

        try {
            UUID merchantId = UUID.fromString(rawMerchantId);

            CsvValidationUtil.validate(file);

            String objectName = buildCsvObjectName(merchantId, jobId);
            minioService.upload(objectName, file);

            batchChecks.put(jobId, new BatchCheckState(
                    merchantId,
                    objectName,
                    Instant.now()
            ));

            log.info(
                    "Batch check accepted. jobId={}, merchantId={}, objectName={}",
                    jobId,
                    merchantId,
                    objectName
            );

            return CheckInitResponseDto.builder()
                    .jobId(jobId)
                    .checkStatus(CheckStatus.IN_PROGRESS)
                    .build();
        } catch (CsvValidationException | FileUploadException e) {
            log.warn("Batch check failed. jobId={}, reason={}", jobId, e.getMessage());

            return CheckInitResponseDto.builder()
                    .jobId(jobId)
                    .checkStatus(CheckStatus.FAILED)
                    .errorCause(e.getMessage())
                    .build();
        }
    }

    public BatchCheckResultDto batchCheckResult(UUID jobId, String rawMerchantId) {
        UUID merchantId = UUID.fromString(rawMerchantId);
        BatchCheckState state = batchChecks.get(jobId);

        if (state == null || !state.merchantId().equals(merchantId)) {
            return BatchCheckResultDto.builder()
                    .checkStatus(CheckStatus.FAILED)
                    .data(List.of())
                    .build();
        }

        return BatchCheckResultDto.builder()
                .checkStatus(CheckStatus.IN_PROGRESS)
                .data(List.of())
                .build();
    }

    private String buildCsvObjectName(UUID merchantId, UUID jobId) {
        return "batch/" + merchantId + "/" + jobId + AppConstants.CSV_EXTENSION;
    }

    private record SingleCheckState(
            UUID merchantId,
            String extId,
            String docNo,
            Instant createdAt
    ) {
    }

    private record BatchCheckState(
            UUID merchantId,
            String objectName,
            Instant createdAt
    ) {
    }
}
