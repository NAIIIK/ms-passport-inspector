package com.example.passportinspector.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import com.example.passportinspector.model.dto.BatchCheckResultDto;
import com.example.passportinspector.model.dto.CheckInitResponseDto;
import com.example.passportinspector.model.dto.SingleCheckRequestDto;
import com.example.passportinspector.model.dto.SingleCheckResultDto;
import com.example.passportinspector.service.PassportVerificationOrchestratorService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static com.example.passportinspector.model.AppConstants.PassportControllerConstants.UUID_REGEXP;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/internal/validation/smev/4/clients")
public class PassportVerificationController {

    private final PassportVerificationOrchestratorService service;

    @PreAuthorize("hasRole('CLIENT') and @merchantAccessGuard.hasMerchantId(authentication, #merchantId)")
    @PostMapping(
            value = "/check",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CheckInitResponseDto> singleCheckInit(
            @RequestHeader("merchantId")
            @Pattern(regexp = UUID_REGEXP, message = "Header merchantId must be UUID")
            String merchantId,

            @RequestBody @Valid SingleCheckRequestDto singleCheckRequestDto
    ) {
        return ResponseEntity.ok(service.singleCheckInit(singleCheckRequestDto, merchantId));
    }

    @PreAuthorize("hasRole('CLIENT') and @merchantAccessGuard.hasMerchantId(authentication, #merchantId)")
    @GetMapping(
            value = "/check/{jobId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<SingleCheckResultDto> singleCheckResult(
            @RequestHeader("merchantId")
            @Pattern(regexp = UUID_REGEXP, message = "Header merchantId must be UUID")
            String merchantId,

            @PathVariable UUID jobId
    ) {
        return ResponseEntity.ok(service.singleCheckResult(jobId, merchantId));
    }

    @PreAuthorize("hasRole('CLIENT') and @merchantAccessGuard.hasMerchantId(authentication, #merchantId)")
    @PostMapping(
            value = "/check/batch",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CheckInitResponseDto> batchCheckInit(
            @RequestHeader("merchantId")
            @Pattern(regexp = UUID_REGEXP, message = "Header merchantId must be UUID")
            String merchantId,

            @RequestParam("inputFile") MultipartFile file
    ) {
        return ResponseEntity.ok(service.batchCheckInit(file, merchantId));
    }

    @PreAuthorize("hasRole('CLIENT') and @merchantAccessGuard.hasMerchantId(authentication, #merchantId)")
    @GetMapping(
            value = "/check/batch/{jobId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<BatchCheckResultDto> batchCheckResult(
            @RequestHeader("merchantId")
            @Pattern(regexp = UUID_REGEXP, message = "Header merchantId must be UUID")
            String merchantId,

            @PathVariable UUID jobId
    ) {
        return ResponseEntity.ok(service.batchCheckResult(jobId, merchantId));
    }
}