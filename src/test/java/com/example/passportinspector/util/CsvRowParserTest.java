package com.example.passportinspector.util;

import com.example.passportinspector.exception.CsvValidationException;
import com.example.passportinspector.model.csv.PassportCsvRow;
import com.example.passportinspector.util.CsvRowParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvRowParserTest {

    private final CsvRowParser parser = new CsvRowParser();

    @Test
    void shouldParseValidCsvRow() {
        PassportCsvRow row = parser.parse("client-001, Иванов , Иван , Иванович ,0310,559835", 2);

        assertThat(row.extId()).isEqualTo("client-001");
        assertThat(row.personLastName()).isEqualTo("Иванов");
        assertThat(row.personFirstName()).isEqualTo("Иван");
        assertThat(row.personMiddleName()).isEqualTo("Иванович");
        assertThat(row.docSeriesNo()).isEqualTo("0310");
        assertThat(row.docNo()).isEqualTo("559835");
    }

    @Test
    void shouldRejectRowWithInvalidPassportSeries() {
        assertThatThrownBy(() -> parser.parse("client-001,Иванов,Иван,Иванович,ABCD,559835", 2))
                .isInstanceOf(CsvValidationException.class)
                .hasMessageContaining("Invalid doc_series_no");
    }
}
