package com.example.passportinspector.worker;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

class CsvParseWorkerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldRegisterWorkerWhenPropertyMissing() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(CsvParseWorker.class));
    }

    @Test
    void shouldRegisterWorkerWhenPropertyExplicitlyTrue() {
        contextRunner
                .withPropertyValues("worker.csv-parser.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(CsvParseWorker.class));
    }

    @Test
    void shouldNotRegisterWorkerWhenPropertyIsFalse() {
        contextRunner
                .withPropertyValues("worker.csv-parser.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(CsvParseWorker.class));
    }

    @Test
    void shouldDelegateToCsvTaskProcessor() {
        CsvTaskProcessor processor = Mockito.mock(CsvTaskProcessor.class);
        CsvParseWorker worker = new CsvParseWorker(processor);

        worker.processOneCsvTask();

        verify(processor).processOneCsvTask();
    }

    @Configuration
    static class TestConfig {
        @Bean
        CsvTaskProcessor csvTaskProcessor() {
            return Mockito.mock(CsvTaskProcessor.class);
        }

        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
                prefix = "worker.csv-parser",
                name = "enabled",
                havingValue = "true",
                matchIfMissing = true
        )
        CsvParseWorker csvParseWorker(CsvTaskProcessor csvTaskProcessor) {
            return new CsvParseWorker(csvTaskProcessor);
        }
    }
}