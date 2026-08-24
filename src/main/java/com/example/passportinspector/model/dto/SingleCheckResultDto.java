package com.example.passportinspector.model.dto;

import com.example.passportinspector.model.type.JobStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SingleCheckResultDto {

    private JobStatus checkStatus;
    private String extId;
}
