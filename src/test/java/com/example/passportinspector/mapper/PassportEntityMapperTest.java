package com.example.passportinspector.mapper;

import com.example.passportinspector.model.csv.PassportCsvRow;
import com.example.passportinspector.model.dto.SingleCheckRequestDto;
import com.example.passportinspector.model.type.DocumentStatus;
import com.example.passportinspector.model.type.PassportCheckStatus;
import com.example.passportinspector.repository.entity.PassportEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PassportEntityMapperTest {

    private static final UUID JOB_ID = UUID.randomUUID();
    private static final UUID MERCHANT_ID = UUID.randomUUID();

    private final PassportEntityMapper mapper = new PassportEntityMapper();

    @Test
    void shouldMapSingleRequestAndNormalizeNames() {
        SingleCheckRequestDto request = SingleCheckRequestDto.builder()
                .extId("client-001")
                .personLastName("  ivanov ")
                .personFirstName("ivan")
                .personMiddleName("Ivanovich")
                .docSeriesNo("0310")
                .docNo("559835")
                .build();

        PassportEntity result = mapper.fromSingleRequest(JOB_ID, MERCHANT_ID, request);

        assertThat(result.getJobId()).isEqualTo(JOB_ID);
        assertThat(result.getMerchantId()).isEqualTo(MERCHANT_ID);
        assertThat(result.getExtId()).isEqualTo("client-001");
        assertThat(result.getPersonLastName()).isEqualTo("IVANOV");
        assertThat(result.getPersonFirstName()).isEqualTo("IVAN");
        assertThat(result.getPersonMiddleName()).isEqualTo("IVANOVICH");
        assertThat(result.getDocSeriesNo()).isEqualTo("0310");
        assertThat(result.getDocNo()).isEqualTo("559835");
        assertThat(result.getCheckStatus()).isEqualTo(PassportCheckStatus.UNKNOWN);
        assertThat(result.getDocumentStatus()).isEqualTo(DocumentStatus.UNKNOWN);
    }

    @Test
    void shouldMapCsvRowAndNormalizeNames() {
        PassportCsvRow row = new PassportCsvRow("client-002", " petrov", "petr ", "sergeevich", "0001", "1617897665");

        PassportEntity result = mapper.fromCsvRow(JOB_ID, MERCHANT_ID, row);

        assertThat(result.getJobId()).isEqualTo(JOB_ID);
        assertThat(result.getMerchantId()).isEqualTo(MERCHANT_ID);
        assertThat(result.getExtId()).isEqualTo("client-002");
        assertThat(result.getPersonLastName()).isEqualTo("PETROV");
        assertThat(result.getPersonFirstName()).isEqualTo("PETR");
        assertThat(result.getPersonMiddleName()).isEqualTo("SERGEEVICH");
        assertThat(result.getDocSeriesNo()).isEqualTo("0001");
        assertThat(result.getDocNo()).isEqualTo("1617897665");
        assertThat(result.getCheckStatus()).isEqualTo(PassportCheckStatus.UNKNOWN);
        assertThat(result.getDocumentStatus()).isEqualTo(DocumentStatus.UNKNOWN);
    }

    @Test
    void shouldHandleNullMiddleNameWithoutThrowing() {
        SingleCheckRequestDto request = SingleCheckRequestDto.builder()
                .extId("client-003")
                .personLastName("Sidorov")
                .personFirstName("Sidor")
                .personMiddleName(null)
                .docSeriesNo("0310")
                .docNo("559835")
                .build();

        PassportEntity result = mapper.fromSingleRequest(JOB_ID, MERCHANT_ID, request);

        assertThat(result.getPersonMiddleName()).isNull();
    }
}