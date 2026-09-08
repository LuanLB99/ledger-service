package com.luanleal.ledger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionIT extends AbstractApiIT {

    @Test
    @DisplayName("credito valido soma ao saldo e devolve o saldo resultante")
    void shouldCredit() {
        String accountId = createAccount("Holder de Teste");

        ResponseEntity<String> response =
                movement(accountId, "CREDIT", "150.00", UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(json(response).read("$.accountId", String.class)).isEqualTo(accountId);
        assertThat(json(response).read("$.type", String.class)).isEqualTo("CREDIT");
        assertThat(json(response).read("$.amount", BigDecimal.class)).isEqualByComparingTo("150.00");
        assertThat(json(response).read("$.balanceAfter", BigDecimal.class)).isEqualByComparingTo("150.00");
        assertThat(balanceOf(accountId)).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("debito dentro do saldo subtrai e devolve o saldo resultante")
    void shouldDebit() {
        String accountId = createAccount("Holder de Teste");
        movement(accountId, "CREDIT", "100.00", UUID.randomUUID().toString());

        ResponseEntity<String> response =
                movement(accountId, "DEBIT", "30.00", UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(json(response).read("$.balanceAfter", BigDecimal.class)).isEqualByComparingTo("70.00");
        assertThat(balanceOf(accountId)).isEqualByComparingTo("70.00");
    }

    @Test
    @DisplayName("requisicao sem Idempotency-Key e recusada")
    void shouldRejectMissingIdempotencyKey() {
        String accountId = createAccount("Holder de Teste");

        ResponseEntity<String> response = post("/accounts/%s/transactions".formatted(accountId), """
                {"type": "CREDIT", "amount": 10.00}""");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(balanceOf(accountId)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("tipo nulo e recusado")
    void shouldRejectNullType() {
        String accountId = createAccount("Holder de Teste");

        ResponseEntity<String> response = post("/accounts/%s/transactions".formatted(accountId), """
                {"type": null, "amount": 10.00}""", UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(balanceOf(accountId)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("tipo desconhecido e recusado antes de qualquer efeito")
    void shouldRejectUnknownType() {
        String accountId = createAccount("Holder de Teste");

        ResponseEntity<String> response =
                movement(accountId, "PIX", "10.00", UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(balanceOf(accountId)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("valor nulo e recusado")
    void shouldRejectNullAmount() {
        String accountId = createAccount("Holder de Teste");

        ResponseEntity<String> response = post("/accounts/%s/transactions".formatted(accountId), """
                {"type": "CREDIT", "amount": null}""", UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(balanceOf(accountId)).isEqualByComparingTo("0.00");
    }

    @ParameterizedTest
    @DisplayName("valor nao positivo e recusado")
    @ValueSource(strings = {"0.00", "-1.00"})
    void shouldRejectNonPositiveAmount(String amount) {
        String accountId = createAccount("Holder de Teste");

        ResponseEntity<String> response =
                movement(accountId, "CREDIT", amount, UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(balanceOf(accountId)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("valor com mais de duas casas decimais e recusado")
    void shouldRejectAmountWithMoreThanTwoDecimals() {
        String accountId = createAccount("Holder de Teste");

        ResponseEntity<String> response =
                movement(accountId, "CREDIT", "10.001", UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(balanceOf(accountId)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("movimentacao em conta inexistente devolve 404")
    void shouldRejectUnknownAccount() {
        ResponseEntity<String> response =
                movement(UUID.randomUUID().toString(), "CREDIT", "10.00", UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
