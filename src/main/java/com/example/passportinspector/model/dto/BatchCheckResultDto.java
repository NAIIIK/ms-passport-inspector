package com.example.passportinspector.model.dto;

import com.example.passportinspector.model.type.CheckStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchCheckResultDto {

    private CheckStatus checkStatus;
    private List<String> data;

}
