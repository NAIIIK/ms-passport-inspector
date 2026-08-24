package com.example.passportinspector.model.dto;

import com.example.passportinspector.model.type.JobStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckInitResponseDto {

    private UUID jobId;
    private JobStatus checkStatus;
    private String errorCause;
}


