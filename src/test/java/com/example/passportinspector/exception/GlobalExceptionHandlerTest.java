package com.example.passportinspector.exception;

import com.example.passportinspector.model.dto.ErrorResponseDto;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MultipartException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private static final String TRACE_ID = "handler-test-trace-001";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldReturnBadRequestForCsvValidationException() {
        MDC.put("traceId", TRACE_ID);

        ResponseEntity<ErrorResponseDto> response =
                handler.handleDomainValidation(new CsvValidationException("Only .csv files are allowed"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ErrorResponseDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("Only .csv files are allowed");
        assertThat(body.getTraceId()).isEqualTo(TRACE_ID);
        assertThat(body.getError()).isEqualTo("Bad Request");
    }

    @Test
    void shouldReturnBadRequestForFileUploadException() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleDomainValidation(new FileUploadException("File is too large"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ErrorResponseDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("File is too large");
    }

    @Test
    void shouldReturnBadRequestWithFieldDetailsForBodyValidation() throws NoSuchMethodException {
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyTarget", String.class), 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "extId", "must not be blank"));

        ResponseEntity<ErrorResponseDto> response =
                handler.handleBodyValidation(new MethodArgumentNotValidException(parameter, bindingResult));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ErrorResponseDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("Request body validation failed");
        assertThat(body.getDetails()).containsExactly("extId: must not be blank");
    }

    @Test
    void shouldReturnBadRequestForConstraintViolation() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleParamValidation(new ConstraintViolationException(Set.of()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ErrorResponseDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("Request parameter validation failed");
    }

    @Test
    void shouldReturnBadRequestForMalformedJsonBody() {
        MockHttpInputMessage inputMessage = new MockHttpInputMessage("not json".getBytes());

        ResponseEntity<ErrorResponseDto> response =
                handler.handleMalformedRequest(new HttpMessageNotReadableException("malformed JSON", inputMessage));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ErrorResponseDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("Malformed request");
    }

    @Test
    void shouldReturnBadRequestForMultipartFailure() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleMalformedRequest(new MultipartException("multipart parse failed"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void shouldReturnForbiddenForAccessDenied() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ErrorResponseDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("Access denied");
    }

    @Test
    void shouldReturnInternalServerErrorForUnexpectedException() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleUnexpected(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        ErrorResponseDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).isEqualTo("Internal server error");
    }

    @SuppressWarnings("unused")
    private void dummyTarget(String extId) {
        // used only to obtain a MethodParameter for MethodArgumentNotValidException
    }
}