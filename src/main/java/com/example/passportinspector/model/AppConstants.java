package com.example.passportinspector.model;

public class AppConstants {

    private AppConstants() {}

    public static final String CSV_EXTENSION = ".csv";

    public static class PassportControllerConstants {

        private PassportControllerConstants() {}

        public static final String UUID_REGEXP =
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    }
}
