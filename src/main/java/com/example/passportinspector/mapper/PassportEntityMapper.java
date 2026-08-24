package com.example.passportinspector.mapper;

import com.example.passportinspector.model.csv.PassportCsvRow;
import com.example.passportinspector.model.dto.SingleCheckRequestDto;
import com.example.passportinspector.model.type.DocumentStatus;
import com.example.passportinspector.model.type.PassportCheckStatus;
import com.example.passportinspector.repository.entity.PassportEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PassportEntityMapper {

    public PassportEntity fromSingleRequest(UUID jobId, UUID merchantId, SingleCheckRequestDto request) {
        return PassportEntity.builder()
                .jobId(jobId)
                .merchantId(merchantId)
                .extId(request.getExtId())
                .personLastName(normalizeName(request.getPersonLastName()))
                .personFirstName(normalizeName(request.getPersonFirstName()))
                .personMiddleName(normalizeName(request.getPersonMiddleName()))
                .docSeriesNo(request.getDocSeriesNo())
                .docNo(request.getDocNo())
                .checkStatus(PassportCheckStatus.UNKNOWN)
                .documentStatus(DocumentStatus.UNKNOWN)
                .build();
    }

    public PassportEntity fromCsvRow(UUID jobId, UUID merchantId, PassportCsvRow row) {
        return PassportEntity.builder()
                .jobId(jobId)
                .merchantId(merchantId)
                .extId(row.extId())
                .personLastName(normalizeName(row.personLastName()))
                .personFirstName(normalizeName(row.personFirstName()))
                .personMiddleName(normalizeName(row.personMiddleName()))
                .docSeriesNo(row.docSeriesNo())
                .docNo(row.docNo())
                .checkStatus(PassportCheckStatus.UNKNOWN)
                .documentStatus(DocumentStatus.UNKNOWN)
                .build();
    }

    private String normalizeName(String value) {
        if (value == null) {
            return null;
        }

        return value.trim().toUpperCase();
    }
}