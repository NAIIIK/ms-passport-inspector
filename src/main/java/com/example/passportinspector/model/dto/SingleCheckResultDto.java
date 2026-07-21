package com.example.passportinspector.model.dto;

import com.example.passportinspector.model.type.CheckStatus;
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

    private CheckStatus checkStatus;
    private String extId;
}
