package com.example.passportinspector.controller;

import com.example.passportinspector.model.dto.BatchCheckResultDto;
import com.example.passportinspector.model.dto.CheckInitResponseDto;
import com.example.passportinspector.model.dto.SingleCheckRequestDto;
import com.example.passportinspector.model.dto.SingleCheckResultDto;
import com.example.passportinspector.service.PassportVerificationOrchestratorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static com.example.passportinspector.model.AppConstants.PassportControllerConstants.UUID_REGEXP;

@Validated
@RestController
@RequestMapping("/v1/internal/validation/smev/4/clients")
@RequiredArgsConstructor
public class PassportVerificationController {

    private final PassportVerificationOrchestratorService service;

    /*
    ОБРАЗЕЦ ЗАПРОСА:
    POST /v1/internal/validation/smev/4/clients/check HTTP/1.1
    Host: localhost:8080
    merchantId: 550e8400-e29b-41d4-a716-446655440000
    Content-Type: application/json
    Content-Length: 166
    {
        "extId": "client-001",
        "personLastName": "Иванов",
        "personFirstName": "Иван",
        "personMiddleName": "Иванович",
        "docSeriesNo": "0310",
        "docNo": "559835"
    }
    */

    @PostMapping(value = "/check",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CheckInitResponseDto> singleCheckInit(
            @RequestHeader("merchantId")
            @Pattern(regexp = UUID_REGEXP, message = "Header merchantId must be UUID")
            String merchantId,

            @RequestBody @Valid SingleCheckRequestDto singleCheckRequestDto // new SingleCheckRequestDto()
    ) {
        System.out.println("ОДИНОЧНАЯ ПРОВЕРКА");
        return ResponseEntity.ok(service.singleCheckInit(singleCheckRequestDto, merchantId));
    }

    /*
    ОБРАЗЕЦ ЗАПРОСА ДЛЯ ПРОВЕРКИ СОСТОЯНИЯ job'ы:
    GET /v1/internal/validation/smev/4/clients/check/{jobId} HTTP/1.1
    */
    @GetMapping(value = "/check/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SingleCheckResultDto> singleCheckResult(
            @RequestHeader("merchantId")
            @Pattern(regexp = UUID_REGEXP, message = "Header merchantId must be UUID")
            String merchantId,

            @PathVariable UUID jobId) {
        return ResponseEntity.ok(service.singleCheckResult(jobId, merchantId));
    }

     /*
    ОБРАЗЕЦ ЗАПРОСА (НЕ ЗАБУДЬТЕ ПРИЛОЖИТЬ ФАЙЛ, ОН В РЕСУРСАХ В ПАКЕТЕ exampleData:
    POST /v1/internal/validation/smev/4/clients/check/batch HTTP/1.1
    Host: localhost:8080
    merchantId: 550e8400-e29b-41d4-a716-446655440000
    Content-Length: 185
    Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW

    ------WebKitFormBoundary7MA4YWxkTrZu0gW
    Content-Disposition: form-data; name="inputFile"; filename="Книга1.csv"
    Content-Type: text/csv

    (data)
    ------WebKitFormBoundary7MA4YWxkTrZu0gW--
    */

    @PostMapping(value = "/check/batch",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CheckInitResponseDto> batchCheckInit(
            @RequestHeader("merchantId")
            @Pattern(regexp = UUID_REGEXP, message = "Header merchantId must be UUID")
            String merchantId,

            @RequestParam("inputFile") MultipartFile file
    ) {
        System.out.println("ПАКЕТНАЯ ПРОВЕРКА");
        return ResponseEntity.ok(service.batchCheckInit(file, merchantId));
    }

    /*
    ОБРАЗЕЦ ЗАПРОСА ДЛЯ ПРОВЕРКИ СОСТОЯНИЯ job'ы:
    GET /v1/internal/validation/smev/4/clients/check/batch/{jobId} HTTP/1.1
    */
    @GetMapping(value = "/check/batch/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BatchCheckResultDto> batchCheckResult(
            @RequestHeader("merchantId")
            @Pattern(regexp = UUID_REGEXP, message = "Header merchantId must be UUID")
            String merchantId,

            @PathVariable
            UUID jobId) {
        return ResponseEntity.ok(service.batchCheckResult(jobId, merchantId));
    }
}
