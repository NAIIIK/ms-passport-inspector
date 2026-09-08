package com.example.passportinspector.worker;

import com.example.passportinspector.integration.dto.SmevRequestDto;
import com.example.passportinspector.integration.dto.SmevResponseDto;
import com.example.passportinspector.integration.smev.SmevCheckClient;
import com.example.passportinspector.model.type.DocumentStatus;
import com.example.passportinspector.model.type.PassportCheckStatus;
import com.example.passportinspector.repository.PassportRepository;
import com.example.passportinspector.repository.entity.PassportEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassportVerificationProcessorTest {

    private static final UUID JOB_ID = UUID.randomUUID();

    @Mock
    private SmevCheckClient smevCheckClient;

    @Mock
    private PassportRepository passportRepository;

    @Mock
    private JobStatusService jobStatusService;

    @InjectMocks
    private PassportVerificationProcessor processor;

    @BeforeEach
    void setSmevToken() {
        ReflectionTestUtils.setField(processor, "smevToken", "test-token");
    }

    @Test
    void shouldReturnFalseWhenNoPassportIsAvailable() {
        when(passportRepository.findFirstForProcessing(PassportCheckStatus.UNKNOWN.name()))
                .thenReturn(Optional.empty());

        boolean result = processor.processOnePassport();

        assertThat(result).isFalse();
        verify(jobStatusService, never()).markInProgress(any());
    }

    @Test
    void shouldMarkDocumentValidWhenSmevReturnsValidStatus() {
        PassportEntity passport = passport();
        when(passportRepository.findFirstForProcessing(PassportCheckStatus.UNKNOWN.name()))
                .thenReturn(Optional.of(passport));
        when(smevCheckClient.checkPassport(eq("test-token"), any(SmevRequestDto.class)))
                .thenReturn(smevResponse("300"));

        boolean result = processor.processOnePassport();

        assertThat(result).isTrue();
        assertThat(passport.getDocumentStatus()).isEqualTo(DocumentStatus.VALID);
        assertThat(passport.getCheckStatus()).isEqualTo(PassportCheckStatus.COMPLETED);
        assertThat(passport.getAttempts()).isEqualTo(1);

        verify(jobStatusService).markInProgress(JOB_ID);
        verify(jobStatusService).completeIfAllPassportsProcessed(JOB_ID);
        verify(jobStatusService, never()).failJob(any());
    }

    @Test
    void shouldMarkDocumentInvalidWhenSmevReturnsInvalidStatus() {
        PassportEntity passport = passport();
        when(passportRepository.findFirstForProcessing(PassportCheckStatus.UNKNOWN.name()))
                .thenReturn(Optional.of(passport));
        when(smevCheckClient.checkPassport(eq("test-token"), any(SmevRequestDto.class)))
                .thenReturn(smevResponse("301"));

        processor.processOnePassport();

        assertThat(passport.getDocumentStatus()).isEqualTo(DocumentStatus.INVALID);
        assertThat(passport.getCheckStatus()).isEqualTo(PassportCheckStatus.COMPLETED);
    }

    @Test
    void shouldMarkDocumentUnknownWhenSmevReturnsUnrecognizedStatus() {
        PassportEntity passport = passport();
        when(passportRepository.findFirstForProcessing(PassportCheckStatus.UNKNOWN.name()))
                .thenReturn(Optional.of(passport));
        when(smevCheckClient.checkPassport(eq("test-token"), any(SmevRequestDto.class)))
                .thenReturn(smevResponse("999"));

        processor.processOnePassport();

        assertThat(passport.getDocumentStatus()).isEqualTo(DocumentStatus.UNKNOWN);
        assertThat(passport.getCheckStatus()).isEqualTo(PassportCheckStatus.COMPLETED);
    }

    @Test
    void shouldFailPassportWhenSmevClientThrows() {
        PassportEntity passport = passport();
        when(passportRepository.findFirstForProcessing(PassportCheckStatus.UNKNOWN.name()))
                .thenReturn(Optional.of(passport));
        when(smevCheckClient.checkPassport(eq("test-token"), any(SmevRequestDto.class)))
                .thenThrow(new RuntimeException("SMEV unavailable"));

        boolean result = processor.processOnePassport();

        assertThat(result).isTrue();
        assertThat(passport.getCheckStatus()).isEqualTo(PassportCheckStatus.FAILED);

        verify(jobStatusService).failJob(JOB_ID);
        verify(jobStatusService, never()).completeIfAllPassportsProcessed(any());
    }

    @Test
    void shouldMapPassportFieldsIntoSmevRequest() {
        PassportEntity passport = passport();
        when(passportRepository.findFirstForProcessing(PassportCheckStatus.UNKNOWN.name()))
                .thenReturn(Optional.of(passport));
        when(smevCheckClient.checkPassport(eq("test-token"), any(SmevRequestDto.class)))
                .thenReturn(smevResponse("300"));

        processor.processOnePassport();

        ArgumentCaptor<SmevRequestDto> requestCaptor = ArgumentCaptor.forClass(SmevRequestDto.class);
        verify(smevCheckClient).checkPassport(eq("test-token"), requestCaptor.capture());

        SmevRequestDto request = requestCaptor.getValue();
        assertThat(request.getFamilyName()).isEqualTo("Ivanov");
        assertThat(request.getFirstName()).isEqualTo("Ivan");
        assertThat(request.getPatronymic()).isEqualTo("Ivanovich");
        assertThat(request.getSeries()).isEqualTo("0310");
        assertThat(request.getNumber()).isEqualTo("559835");
    }

    private PassportEntity passport() {
        return PassportEntity.builder()
                .jobId(JOB_ID)
                .extId("client-001")
                .personLastName("Ivanov")
                .personFirstName("Ivan")
                .personMiddleName("Ivanovich")
                .docSeriesNo("0310")
                .docNo("559835")
                .checkStatus(PassportCheckStatus.UNKNOWN)
                .documentStatus(DocumentStatus.UNKNOWN)
                .attempts(0)
                .build();
    }

    private SmevResponseDto smevResponse(String status) {
        SmevResponseDto response = new SmevResponseDto();
        response.setStatus(status);
        return response;
    }
}