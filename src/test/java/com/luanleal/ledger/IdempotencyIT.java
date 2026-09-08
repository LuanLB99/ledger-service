package com.luanleal.ledger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyIT extends AbstractApiIT {

    @Test
    @DisplayName("requisicao repetida com a mesma chave nao gera efeito duplicado")
    void shouldNotApplyTheSameTransactionTwice() {
        String accountId = createAccount("Holder de Teste");
        String key = UUID.randomUUID().toString();

        ResponseEntity<String> first = movement(accountId, "CREDIT", "150.00", key);
        ResponseEntity<String> second = movement(accountId, "CREDIT", "150.00", key);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(json(second).read("$.id", String.class))
                .isEqualTo(json(first).read("$.id", String.class));

        assertThat(balanceOf(accountId)).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("mesma chave com payload diferente e rejeitada")
    void shouldRejectKeyReuseWithDifferentPayload() {
        String accountId = createAccount("Holder de Teste");
        String key = UUID.randomUUID().toString();

        movement(accountId, "CREDIT", "150.00", key);
        ResponseEntity<String> conflicting = movement(accountId, "CREDIT", "999.00", key);

        assertThat(conflicting.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(balanceOf(accountId)).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("debito acima do saldo e rejeitado e nao altera a conta")
    void shouldRejectDebitAboveBalance() {
        String accountId = createAccount("Holder de Teste");
        movement(accountId, "CREDIT", "100.00", UUID.randomUUID().toString());

        ResponseEntity<String> response =
                movement(accountId, "DEBIT", "100.01", UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(balanceOf(accountId)).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("duas requisicoes simultaneas com a mesma chave creditam uma vez so")
    void shouldApplyOnlyOnceUnderConcurrentSameKey() throws Exception {
        String accountId = createAccount("Holder de Teste");
        String key = UUID.randomUUID().toString();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Callable<ResponseEntity<String>>> calls = List.of(
                () -> movement(accountId, "CREDIT", "150.00", key),
                () -> movement(accountId, "CREDIT", "150.00", key));

        List<Future<ResponseEntity<String>>> futures = executor.invokeAll(calls);
        executor.shutdown();

        List<HttpStatusCode> statuses = List.of(
                futures.get(0).get().getStatusCode(),
                futures.get(1).get().getStatusCode());

        // A perdedora da corrida pode sair por dois caminhos legitimos: 409 se
        // colidiu na reserva da chave, ou 201 devolvendo a resposta original se a
        // vencedora ja havia concluido. O que nao pode acontecer, em nenhum dos
        // dois, e o saldo somar duas vezes.
        assertThat(statuses)
                .allMatch(status -> status.equals(HttpStatus.CREATED) || status.equals(HttpStatus.CONFLICT));
        assertThat(statuses).contains(HttpStatus.CREATED);

        assertThat(balanceOf(accountId)).isEqualByComparingTo("150.00");
    }
}
