package com.example.passportinspector.service;

import com.example.passportinspector.model.dto.CheckInitResponseDto;
import com.example.passportinspector.model.dto.SingleCheckRequestDto;
import com.example.passportinspector.model.type.CheckStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PassportVerificationOrchestratorServiceTest {
    private static final String MERCHANT_ID = "550e8400-e29b-41d4-a716-446655440000";
    @Mock
    private MinioService minioService;
    @InjectMocks
    private PassportVerificationOrchestratorService service;

    @Test
    void singleCheckInitShouldCreateJob() {
        SingleCheckRequestDto request = SingleCheckRequestDto.builder().extId("client-001").personLastName("Иванов").personFirstName("Иван").personMiddleName("Иванович").docSeriesNo("0310").docNo("559835").build();
        CheckInitResponseDto response = service.singleCheckInit(request, MERCHANT_ID);
        assertThat(response.getJobId()).isNotNull();
        assertThat(response.getCheckStatus()).isEqualTo(CheckStatus.IN_PROGRESS);
        assertThat(response.getErrorCause()).isNull();
    }

    @Test
    void batchCheckInitShouldUploadFileToMinio() {
        MockMultipartFile file = new MockMultipartFile(
                "inputFile",
                "passports.csv",
                "text/csv",
                """
                                id,person_last_name,person_first_name,person_middle_name,doc_series_no,doc_no
                                client-001,Иванов,Иван,Иванович,0310,559835
                        """.getBytes(StandardCharsets.UTF_8));
        CheckInitResponseDto response = service.batchCheckInit(file, MERCHANT_ID);
        assertThat(response.getJobId()).isNotNull();
        assertThat(response.getCheckStatus()).isEqualTo(CheckStatus.IN_PROGRESS);
        assertThat(response.getErrorCause()).isNull();
        verify(minioService).upload(startsWith("batch/" + MERCHANT_ID + "/"), any());
    }

    @Test
    void batchCheckInitShouldReturnFailedWhenCsvIsInvalid() {
        MockMultipartFile file = new MockMultipartFile(
                "inputFile",
                "passports.csv",
                "text/csv",
                """ 
                                wrong_header,
                                doc_no client-001,
                                Иванов,
                                Иван,
                                Иванович,
                                0310,
                                559835
                        """.getBytes(StandardCharsets.UTF_8));
        CheckInitResponseDto response = service.batchCheckInit(file, MERCHANT_ID);
        assertThat(response.getJobId()).isNotNull();
        assertThat(response.getCheckStatus()).isEqualTo(CheckStatus.FAILED);
        assertThat(response.getErrorCause()).contains("Invalid CSV headers");
    }
}