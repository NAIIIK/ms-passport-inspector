package com.example.passportinspector.util;

import com.example.passportinspector.exception.CsvValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvValidationUtilTest {
    @Test
    void shouldAcceptValidCsv() {
        MockMultipartFile file = new MockMultipartFile(
                "inputFile",
                "passports.csv",
                "text/csv",
                """
                        id,person_last_name,person_first_name,person_middle_name,doc_series_no,doc_no
                        client-001,Иванов,Иван,Иванович,0310,559835
                        """.getBytes(StandardCharsets.UTF_8));
        assertDoesNotThrow(() -> CsvValidationUtil.validate(file));
    }

    @Test
    void shouldRejectWrongExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "inputFile",
                "passports.txt",
                "text/plain",
                """
                        id,person_last_name,person_first_name,person_middle_name,doc_series_no,doc_no
                        client-001,Иванов,Иван,Иванович,0310,559835
                        """.getBytes(StandardCharsets.UTF_8));
        assertThrows(CsvValidationException.class, () -> CsvValidationUtil.validate(file));
    }

    @Test
    void shouldRejectWrongHeaders() {
        MockMultipartFile file = new MockMultipartFile(
                "inputFile",
                "passports.csv",
                "text/csv",
                """ 
                                wrong_header,
                                person_last_name,
                                person_first_name client-001,Иванов,Иван
                        """.getBytes(StandardCharsets.UTF_8));
        assertThrows(CsvValidationException.class, () -> CsvValidationUtil.validate(file));
    }
}