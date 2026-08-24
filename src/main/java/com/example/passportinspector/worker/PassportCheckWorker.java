package com.example.passportinspector.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "worker.passport-check",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PassportCheckWorker {

    private final PassportVerificationProcessor passportVerificationProcessor;

    @Scheduled(fixedDelayString = "${worker.passport-check.fixed-delay-ms:3000}")
    public void processOnePassport() {
        passportVerificationProcessor.processOnePassport();
    }
}