package com.example.passportinspector.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTestStubController.class)
@Import(SecurityConfig.class)
@EnableWebSecurity
@TestPropertySource(properties = "app.security.jwt.secret=test-secret-key-for-security-config-test-only")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowHealthWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowHealthSubPathsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowInfoWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowPrometheusWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectLoggersWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/loggers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowLoggersWithValidJwt() throws Exception {
        mockMvc.perform(get("/actuator/loggers").with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectBusinessEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v1/internal/validation/smev/4/clients/ping"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowBusinessEndpointWithValidJwt() throws Exception {
        mockMvc.perform(get("/v1/internal/validation/smev/4/clients/ping").with(jwt()))
                .andExpect(status().isOk());
    }
}