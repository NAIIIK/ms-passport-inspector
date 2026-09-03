package com.example.passportinspector.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.passportinspector.exception.CsvValidationException;
import com.example.passportinspector.mapper.PassportEntityMapper;
import com.example.passportinspector.model.type.CsvTaskStatus;
import com.example.passportinspector.repository.CsvTaskRepository;
import com.example.passportinspector.repository.PassportRepository;
import com.example.passportinspector.repository.entity.CsvTaskEntity;
import com.example.passportinspector.repository.entity.PassportEntity;
import com.example.passportinspector.service.MinioService;
import com.example.passportinspector.util.CsvRowParser;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvTaskProcessor {

    private static final String TRACE_ID = "traceId";

    private final MinioService minioService;
    private final CsvTaskRepository csvTaskRepository;
    private final PassportRepository passportRepository;
    private final PassportEntityMapper passportEntityMapper;
    private final CsvRowParser csvRowParser;
    private final JobStatusService jobStatusService;

    @Transactional
    public boolean processOneCsvTask() {
        return csvTaskRepository.findFirstByStatusOrderByCreatedAtAsc(CsvTaskStatus.NEW)
                .map(this::process)
                .orElse(false);
    }

    private boolean process(CsvTaskEntity task) {
        if (task.getTraceId() != null && !task.getTraceId().isBlank()) {
            MDC.put(TRACE_ID, task.getTraceId());
        }

        task.setStatus(CsvTaskStatus.IN_PROGRESS);
        task.setAttempts(task.getAttempts() + 1);
        task.setInProgressSince(Instant.now());
        csvTaskRepository.save(task);

        jobStatusService.markInProgress(task.getJobId());

        try {
            List<PassportEntity> passports = readAndValidatePassports(task);

            if (passports.isEmpty()) {
                throw new CsvValidationException("CSV file does not contain data rows");
            }

            passportRepository.saveAll(passports);

            task.setStatus(CsvTaskStatus.COMPLETED);
            csvTaskRepository.save(task);

            log.info("CSV task processed. taskId={}, jobId={}, createdPassports={}",
                    task.getId(), task.getJobId(), passports.size());

            MDC.remove(TRACE_ID);
            return true;
        } catch (Exception e) {
            task.setStatus(CsvTaskStatus.FAILED);
            csvTaskRepository.save(task);
            jobStatusService.failJob(task.getJobId());

            log.error("Failed to process CSV task. taskId={}, jobId={}, reason={}",
                    task.getId(), task.getJobId(), e.getMessage(), e);

            MDC.remove(TRACE_ID);
            return true;
        }
    }

    private List<PassportEntity> readAndValidatePassports(CsvTaskEntity task) throws Exception {
        List<PassportEntity> passports = new ArrayList<>();

        try (var inputStream = minioService.downloadFile(task.getFileName());
             var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String header = reader.readLine();

            if (header == null || header.isBlank()) {
                throw new CsvValidationException("CSV file must contain header line");
            }

            String line;
            int rowNumber = 1;

            while ((line = reader.readLine()) != null) {
                rowNumber++;

                if (line.isBlank()) {
                    continue;
                }

                var csvRow = csvRowParser.parse(line, rowNumber);

                PassportEntity passport = passportEntityMapper.fromCsvRow(
                        task.getJobId(),
                        task.getMerchantId(),
                        csvRow
                );

                passport.setTraceId(task.getTraceId());
                passports.add(passport);
            }
        }

        return passports;
    }
}

