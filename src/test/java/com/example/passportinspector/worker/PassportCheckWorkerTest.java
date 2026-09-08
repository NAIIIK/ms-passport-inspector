package com.example.passportinspector.worker;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

class PassportCheckWorkerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldRegisterWorkerWhenPropertyMissing() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(PassportCheckWorker.class));
    }

    @Test
    void shouldRegisterWorkerWhenPropertyExplicitlyTrue() {
        contextRunner
                .withPropertyValues("worker.passport-check.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(PassportCheckWorker.class));
    }

    @Test
    void shouldNotRegisterWorkerWhenPropertyIsFalse() {
        contextRunner
                .withPropertyValues("worker.passport-check.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(PassportCheckWorker.class));
    }

    @Test
    void shouldDelegateToPassportVerificationProcessor() {
        PassportVerificationProcessor processor = Mockito.mock(PassportVerificationProcessor.class);
        PassportCheckWorker worker = new PassportCheckWorker(processor);

        worker.processOnePassport();

        verify(processor).processOnePassport();
    }

    @Configuration
    static class TestConfig {
        @Bean
        PassportVerificationProcessor passportVerificationProcessor() {
            return Mockito.mock(PassportVerificationProcessor.class);
        }

        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
                prefix = "worker.passport-check",
                name = "enabled",
                havingValue = "true",
                matchIfMissing = true
        )
        PassportCheckWorker passportCheckWorker(PassportVerificationProcessor passportVerificationProcessor) {
            return new PassportCheckWorker(passportVerificationProcessor);
        }
    }
}