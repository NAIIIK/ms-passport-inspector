package com.example.passportinspector.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.passportinspector.integration.smev.SmevCheckClient;
import com.example.passportinspector.integration.dto.SmevRequestDto;
import com.example.passportinspector.model.type.DocumentStatus;
import com.example.passportinspector.model.type.PassportCheckStatus;
import com.example.passportinspector.repository.PassportRepository;
import com.example.passportinspector.repository.entity.PassportEntity;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PassportVerificationProcessor {

    private static final String TRACE_ID = "traceId";

    private static final String SMEV_VALID_STATUS = "300";
    private static final String SMEV_INVALID_STATUS = "301";

    private final SmevCheckClient smevCheckClient;
    private final PassportRepository passportRepository;
    private final JobStatusService jobStatusService;

    @Value("${smev.client.token}")
    private String smevToken;

    @Transactional
    public boolean processOnePassport() {
        return passportRepository
                .findFirstForProcessing(PassportCheckStatus.UNKNOWN.name())
                .map(this::process)
                .orElse(false);
    }

    private boolean process(PassportEntity passport) {
        if (passport.getTraceId() != null && !passport.getTraceId().isBlank()) {
            MDC.put(TRACE_ID, passport.getTraceId());
        }

        passport.setCheckStatus(PassportCheckStatus.IN_PROGRESS);
        passport.setAttempts(passport.getAttempts() + 1);
        passport.setInProgressSince(Instant.now());
        passportRepository.save(passport);

        jobStatusService.markInProgress(passport.getJobId());

        try {
            var request = SmevRequestDto.builder()
                    .familyName(passport.getPersonLastName())
                    .firstName(passport.getPersonFirstName())
                    .patronymic(passport.getPersonMiddleName())
                    .series(passport.getDocSeriesNo())
                    .number(passport.getDocNo())
                    .build();

            var response = smevCheckClient.checkPassport(smevToken, request);

            if (SMEV_VALID_STATUS.equals(response.getStatus())) {
                passport.setDocumentStatus(DocumentStatus.VALID);
            } else if (SMEV_INVALID_STATUS.equals(response.getStatus())) {
                passport.setDocumentStatus(DocumentStatus.INVALID);
            } else {
                passport.setDocumentStatus(DocumentStatus.UNKNOWN);
            }

            passport.setCheckStatus(PassportCheckStatus.COMPLETED);
            passportRepository.save(passport);

            jobStatusService.completeIfAllPassportsProcessed(passport.getJobId());

            log.info("Passport processed. passportId={}, jobId={}, extId={}, documentStatus={}",
                    passport.getId(),
                    passport.getJobId(),
                    passport.getExtId(),
                    passport.getDocumentStatus());

            MDC.remove(TRACE_ID);
            return true;
        } catch (Exception e) {
            passport.setCheckStatus(PassportCheckStatus.FAILED);
            passportRepository.save(passport);

            jobStatusService.failJob(passport.getJobId());

            log.error("Failed to process passport. passportId={}, jobId={}, extId={}",
                    passport.getId(), passport.getJobId(), passport.getExtId(), e);

            MDC.remove(TRACE_ID);
            return true;
        }
    }
}
