package com.example.passportinspector.util;

import com.example.passportinspector.exception.CsvValidationException;
import com.example.passportinspector.model.csv.PassportCsvRow;
import org.springframework.stereotype.Component;

@Component
public class CsvRowParser {

    private static final int EXPECTED_COLUMNS_COUNT = 6;
    private static final String DOC_SERIES_REGEXP = "\\d{3,4}";
    private static final String DOC_NO_REGEXP = "\\d{6,10}";

    public PassportCsvRow parse(String line, int rowNumber) {
        String[] columns = line.split(",", -1);

        if (columns.length != EXPECTED_COLUMNS_COUNT) {
            throw new CsvValidationException(
                    "Invalid CSV row " + rowNumber
                            + ". Expected columns: " + EXPECTED_COLUMNS_COUNT
                            + ", actual: " + columns.length
            );
        }

        PassportCsvRow row = new PassportCsvRow(
                columns[0].trim(),
                columns[1].trim(),
                columns[2].trim(),
                columns[3].trim(),
                columns[4].trim(),
                columns[5].trim()
        );

        validate(row, rowNumber);

        return row;
    }

    private void validate(PassportCsvRow row, int rowNumber) {
        validateNotBlank(row.extId(), "id", rowNumber);
        validateNotBlank(row.personLastName(), "person_last_name", rowNumber);
        validateNotBlank(row.personFirstName(), "person_first_name", rowNumber);
        validateNotBlank(row.personMiddleName(), "person_middle_name", rowNumber);
        validateNotBlank(row.docSeriesNo(), "doc_series_no", rowNumber);
        validateNotBlank(row.docNo(), "doc_no", rowNumber);

        if (!row.docSeriesNo().matches(DOC_SERIES_REGEXP)) {
            throw new CsvValidationException(
                    "Invalid doc_series_no in CSV row " + rowNumber + ". Expected 3-4 digits"
            );
        }

        if (!row.docNo().matches(DOC_NO_REGEXP)) {
            throw new CsvValidationException(
                    "Invalid doc_no in CSV row " + rowNumber + ". Expected 6-10 digits"
            );
        }
    }

    private void validateNotBlank(String value, String columnName, int rowNumber) {
        if (value == null || value.isBlank()) {
            throw new CsvValidationException(
                    "Invalid CSV row " + rowNumber + ". Column " + columnName + " must not be blank"
            );
        }
    }
}