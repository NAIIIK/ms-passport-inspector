package com.example.passportinspector.config;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;

public class SmevFeignConfig {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC_KEY = "traceId";

    @Bean
    public RequestInterceptor smevTraceIdInterceptor() {
        return requestTemplate -> {
            String traceId = MDC.get(TRACE_ID_MDC_KEY);
            if (traceId != null && !traceId.isBlank()) {
                requestTemplate.header(TRACE_ID_HEADER, traceId);
            }
        };
    }
}
