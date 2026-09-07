package com.example.controller;

import com.example.passportinspector.controller.PassportVerificationController;
import com.example.passportinspector.model.dto.BatchCheckResultDto;
import com.example.passportinspector.model.dto.CheckInitResponseDto;
import com.example.passportinspector.model.dto.SingleCheckResultDto;
import com.example.passportinspector.model.type.JobStatus;
import com.example.passportinspector.service.PassportVerificationOrchestratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PassportVerificationController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class PassportVerificationControllerTest {

    private static final String BASE_URL = "/v1/internal/validation/smev/4/clients";
    private static final String MERCHANT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String MERCHANT_ID_HEADER = "merchantId";

    private static final String EXPECTED_JOB_STATUS = "IN_PROGRESS";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PassportVerificationOrchestratorService service;

    @Test
    void singleCheckInitShouldReturnJobId() throws Exception {
        UUID jobId = UUID.fromString("c132b7d8-5d69-4697-85af-7c66b0a64e91");
        given(service.singleCheckInit(any(), eq(MERCHANT_ID)))
                .willReturn(CheckInitResponseDto.builder()
                        .jobId(jobId)
                        .checkStatus(JobStatus.IN_PROGRESS)
                        .build());

        mockMvc.perform(post(BASE_URL + "/check")
                        .header(MERCHANT_ID_HEADER, MERCHANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "extId": "client-001",
                                  "personLastName": "Иванов",
                                  "personFirstName": "Иван",
                                  "personMiddleName": "Иванович",
                                  "docSeriesNo": "0310",
                                  "docNo": "559835"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.checkStatus").value(EXPECTED_JOB_STATUS))
                .andExpect(jsonPath("$.errorCause").doesNotExist());
    }

    @Test
    void singleCheckInitShouldRejectInvalidBody() throws Exception {
        mockMvc.perform(post(BASE_URL + "/check")
                        .header(MERCHANT_ID_HEADER, MERCHANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "extId": "client-001",
                                  "personLastName": "",
                                  "personFirstName": "Иван",
                                  "personMiddleName": "Иванович",
                                  "docSeriesNo": "abc",
                                  "docNo": "559835"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void singleCheckResultShouldReturnStatus() throws Exception {
        UUID jobId = UUID.fromString("c132b7d8-5d69-4697-85af-7c66b0a64e91");
        given(service.singleCheckResult(jobId, MERCHANT_ID))
                .willReturn(SingleCheckResultDto.builder()
                        .checkStatus(JobStatus.IN_PROGRESS)
                        .build());

        mockMvc.perform(get(BASE_URL + "/check/{jobId}", jobId)
                        .header(MERCHANT_ID_HEADER, MERCHANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkStatus").value(EXPECTED_JOB_STATUS))
                .andExpect(jsonPath("$.extId").doesNotExist());
    }

    @Test
    void batchCheckInitShouldReturnJobId() throws Exception {
        UUID jobId = UUID.fromString("f5f33e7e-f611-41e7-a64c-77ed752dd384");
        given(service.batchCheckInit(any(), eq(MERCHANT_ID)))
                .willReturn(CheckInitResponseDto.builder()
                        .jobId(jobId)
                        .checkStatus(JobStatus.IN_PROGRESS)
                        .build());

        MockMultipartFile file = new MockMultipartFile(
                "inputFile",
                "passports.csv",
                "text/csv",
                """
                        id,person_last_name,person_first_name,person_middle_name,doc_series_no,doc_no
                        client-001,Иванов,Иван,Иванович,0310,559835
                        """.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart(BASE_URL + "/check/batch")
                        .file(file)
                        .header(MERCHANT_ID_HEADER, MERCHANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.checkStatus").value(EXPECTED_JOB_STATUS))
                .andExpect(jsonPath("$.errorCause").doesNotExist());
    }

    @Test
    void batchCheckResultShouldReturnStatus() throws Exception {
        UUID jobId = UUID.fromString("f5f33e7e-f611-41e7-a64c-77ed752dd384");
        given(service.batchCheckResult(jobId, MERCHANT_ID))
                .willReturn(BatchCheckResultDto.builder()
                        .checkStatus(JobStatus.IN_PROGRESS)
                        .data(List.of())
                        .build());

        mockMvc.perform(get(BASE_URL + "/check/batch/{jobId}", jobId)
                        .header(MERCHANT_ID_HEADER, MERCHANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkStatus").value(EXPECTED_JOB_STATUS))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
