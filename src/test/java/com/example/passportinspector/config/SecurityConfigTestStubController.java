package com.example.passportinspector.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityConfigTestStubController {

    @GetMapping("/actuator/health")
    public String health() {
        return "UP";
    }

    @GetMapping("/actuator/health/liveness")
    public String healthLiveness() {
        return "UP";
    }

    @GetMapping("/actuator/info")
    public String info() {
        return "{}";
    }

    @GetMapping("/actuator/prometheus")
    public String prometheus() {
        return "# metrics";
    }

    @GetMapping("/actuator/loggers")
    public String loggers() {
        return "{}";
    }

    @GetMapping("/v1/internal/validation/smev/4/clients/ping")
    public String ping() {
        return "pong";
    }
}