package com.example.passportinspector.worker;

import com.example.passportinspector.exception.CsvValidationException;
import com.example.passportinspector.mapper.PassportEntityMapper;
import com.example.passportinspector.model.csv.PassportCsvRow;
import com.example.passportinspector.model.type.CsvTaskStatus;
import com.example.passportinspector.repository.CsvTaskRepository;
import com.example.passportinspector.repository.PassportRepository;
import com.example.passportinspector.repository.entity.CsvTaskEntity;
import com.example.passportinspector.repository.entity.PassportEntity;
import com.example.passportinspector.service.MinioService;
import com.example.passportinspector.util.CsvRowParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvTaskProcessorTest {

    private static final UUID JOB_ID = UUID.randomUUID();
    private static final UUID MERCHANT_ID = UUID.randomUUID();

    @Mock
    private MinioService minioService;

    @Mock
    private CsvTaskRepository csvTaskRepository;

    @Mock
    private PassportRepository passportRepository;

    @Mock
    private PassportEntityMapper passportEntityMapper;

    @Mock
    private CsvRowParser csvRowParser;

    @Mock
    private JobStatusService jobStatusService;

    @InjectMocks
    private CsvTaskProcessor processor;

    @Test
    void shouldReturnFalseWhenNoTaskIsAvailable() {
        when(csvTaskRepository.findFirstForProcessing(CsvTaskStatus.NEW.name())).thenReturn(Optional.empty());

        boolean result = processor.processOneCsvTask();

        assertThat(result).isFalse();
        verify(jobStatusService, never()).markInProgress(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldParseRowsAndMarkTaskCompletedOnSuccess() {
        CsvTaskEntity task = csvTask();
        String csv = "header\nclient-001,Ivanov,Ivan,Ivanovich,0310,559835\n";
        when(csvTaskRepository.findFirstForProcessing(CsvTaskStatus.NEW.name())).thenReturn(Optional.of(task));
        when(minioService.downloadFile(task.getFileName()))
                .thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        PassportCsvRow row = new PassportCsvRow("client-001", "Ivanov", "Ivan", "Ivanovich", "0310", "559835");
        when(csvRowParser.parse(eq("client-001,Ivanov,Ivan,Ivanovich,0310,559835"), eq(2))).thenReturn(row);

        PassportEntity passport = PassportEntity.builder().jobId(JOB_ID).merchantId(MERCHANT_ID).extId("client-001").build();
        when(passportEntityMapper.fromCsvRow(JOB_ID, MERCHANT_ID, row)).thenReturn(passport);

        boolean result = processor.processOneCsvTask();

        assertThat(result).isTrue();
        assertThat(task.getStatus()).isEqualTo(CsvTaskStatus.COMPLETED);
        assertThat(task.getAttempts()).isEqualTo(1);

        verify(jobStatusService).markInProgress(JOB_ID);
        verify(jobStatusService, never()).failJob(any());

        ArgumentCaptor<List<PassportEntity>> savedCaptor = ArgumentCaptor.forClass(List.class);
        verify(passportRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).containsExactly(passport);

        verify(csvTaskRepository, times(2)).save(task);
    }

    @Test
    void shouldFailTaskWhenCsvHasNoDataRows() {
        CsvTaskEntity task = csvTask();
        String headerOnlyCsv = "header\n";
        when(csvTaskRepository.findFirstForProcessing(CsvTaskStatus.NEW.name())).thenReturn(Optional.of(task));
        when(minioService.downloadFile(task.getFileName()))
                .thenReturn(new ByteArrayInputStream(headerOnlyCsv.getBytes(StandardCharsets.UTF_8)));

        boolean result = processor.processOneCsvTask();

        assertThat(result).isTrue();
        assertThat(task.getStatus()).isEqualTo(CsvTaskStatus.FAILED);
        verify(jobStatusService).failJob(JOB_ID);
        verify(passportRepository, never()).saveAll(any());
    }

    @Test
    void shouldFailTaskWhenCsvRowIsInvalid() {
        CsvTaskEntity task = csvTask();
        String csv = "header\nmalformed-row\n";
        when(csvTaskRepository.findFirstForProcessing(CsvTaskStatus.NEW.name())).thenReturn(Optional.of(task));
        when(minioService.downloadFile(task.getFileName()))
                .thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        when(csvRowParser.parse(eq("malformed-row"), eq(2)))
                .thenThrow(new CsvValidationException("Invalid CSV row 2"));

        boolean result = processor.processOneCsvTask();

        assertThat(result).isTrue();
        assertThat(task.getStatus()).isEqualTo(CsvTaskStatus.FAILED);
        verify(jobStatusService).failJob(JOB_ID);
        verify(passportRepository, never()).saveAll(any());
    }

    private CsvTaskEntity csvTask() {
        return CsvTaskEntity.builder()
                .jobId(JOB_ID)
                .merchantId(MERCHANT_ID)
                .fileName("batch/" + MERCHANT_ID + "/" + JOB_ID + ".csv")
                .status(CsvTaskStatus.NEW)
                .attempts(0)
                .build();
    }
}