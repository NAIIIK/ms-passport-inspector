package com.example.passportinspector.util;

import com.example.passportinspector.exception.CsvValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public final class CsvValidationUtil {
    private static final String CSV_EXTENSION = ".csv";

    private static final List<String> EXPECTED_HEADERS = List.of(
            "id",
            "person_last_name",
            "person_first_name",
            "person_middle_name",
            "doc_series_no",
            "doc_no"
    );

    private CsvValidationUtil() {
    }

    public static void validate(MultipartFile file) {
        validateNotEmpty(file);
        validateExtension(file.getOriginalFilename());
        validateHeaders(file);
    }

    private static void validateNotEmpty(MultipartFile file) {
        if (file == null) {
            throw new CsvValidationException("CSV file is required");
        }

        if (file.isEmpty()) {
            throw new CsvValidationException("CSV file must not be empty");
        }
    }

    private static void validateExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new CsvValidationException("CSV file name is required");
        }

        if (!fileName.toLowerCase().endsWith(CSV_EXTENSION)) {
            throw new CsvValidationException("Only .csv files are allowed");
        }
    }

    private static void validateHeaders(MultipartFile file) {
        try (var reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String headerLine = reader.readLine();

            if (headerLine == null || headerLine.isBlank()) {
                throw new CsvValidationException("CSV file must contain header line");
            }

            List<String> actualHeaders = Arrays.stream(removeBom(headerLine).split(",", -1))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .toList();

            if (!EXPECTED_HEADERS.equals(actualHeaders)) {
                throw new CsvValidationException(
                        "Invalid CSV headers. Expected: " + EXPECTED_HEADERS + ", actual: " + actualHeaders
                );
            }
        } catch (CsvValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new CsvValidationException("Failed to read CSV file");
        }
    }

    private static String removeBom(String value) {
        return value.replace("\uFEFF", "");
    }
}
