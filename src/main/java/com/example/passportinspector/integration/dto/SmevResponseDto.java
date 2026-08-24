package com.example.passportinspector.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmevResponseDto {

    private boolean isValid;
    private String status;
    private String decodeDocStatus;
    private String issuerCode;
    private String issueDate;
    private String invalidityReason;
    private String decodeInvalidityReason;
    private String invaliditySince;
}
