package com.example.passportinspector.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SingleCheckRequestDto {

    @NotBlank(message = "{singleCheck.extId.notBlank}")
    @Size(max = 64, message = "{singleCheck.extId.size}")
    @JsonProperty(value = "extId")
    private String extId; // external id

    @NotBlank(message = "{singleCheck.personLastName.notBlank}")
    @Size(max = 100, message = "{singleCheck.personLastName.size}")
    @JsonProperty(value = "personLastName")
    private String personLastName;

    @NotBlank(message = "{singleCheck.personFirstName.notBlank}")
    @Size(max = 100, message = "{singleCheck.personFirstName.size}")
    @JsonProperty(value = "personFirstName")
    private String personFirstName;

    @NotBlank(message = "{singleCheck.personMiddleName.notBlank}")
    @Size(max = 100, message = "{singleCheck.personMiddleName.size}")
    @JsonProperty(value = "personMiddleName")
    private String personMiddleName;

    @NotBlank(message = "{singleCheck.docSeriesNo.notBlank}")
    @Pattern(
            regexp = "\\d{3,4}",
            message = "{singleCheck.docSeriesNo.pattern}"
    )
    @JsonProperty(value = "docSeriesNo")
    private String docSeriesNo;

    @NotBlank(message = "{singleCheck.docNo.notBlank}")
    @Pattern(
            regexp = "\\d{6,10}",
            message = "{singleCheck.docNo.pattern}"
    )
    @JsonProperty(value = "docNo")
    private String docNo;
}
