package com.example.passportinspector.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmevRequestDto {
    private String familyName;
    private String firstName;
    private String patronymic;
    private String series;
    private String number;
}
