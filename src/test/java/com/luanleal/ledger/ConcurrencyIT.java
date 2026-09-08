package com.luanleal.ledger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrencyIT extends AbstractApiIT {

    private static final int CONCURRENT_REQUESTS = 20;

    @Test
    @DisplayName("debitos concorrentes na mesma conta nao causam lost update")
    void shouldSerializeConcurrentDebits() throws InterruptedException {
        String accountId = createAccount("Holder de Teste");
        movement(accountId, "CREDIT", "1000.00", UUID.randomUUID().toString());

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONCURRENT_REQUESTS);

        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    movement(accountId, "DEBIT", "50.00", UUID.randomUUID().toString());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // 1000.00 - (20 x 50.00) = 0.00
        assertThat(balanceOf(accountId)).isEqualByComparingTo("0.00");
    }
}
