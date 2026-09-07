package com.example.async;

import com.example.passportinspector.model.type.PassportCheckStatus;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class AwaitilityExampleTest {

    @Test
    void shouldWaitUntilBackgroundProcessingCompletes() {
        AtomicReference<PassportCheckStatus> status = new AtomicReference<>(PassportCheckStatus.IN_PROGRESS);

        CompletableFuture.runAsync(() -> {
            sleep(300);
            status.set(PassportCheckStatus.COMPLETED);
        });

        await()
                .atMost(2, SECONDS)
                .untilAsserted(() -> assertThat(status.get()).isEqualTo(PassportCheckStatus.COMPLETED));
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
