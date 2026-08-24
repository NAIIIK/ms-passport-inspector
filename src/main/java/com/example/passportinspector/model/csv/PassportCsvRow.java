package com.example.passportinspector.model.csv;

public record PassportCsvRow(
        String extId,
        String personLastName,
        String personFirstName,
        String personMiddleName,
        String docSeriesNo,
        String docNo
) {}
