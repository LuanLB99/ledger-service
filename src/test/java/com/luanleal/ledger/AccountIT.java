package com.luanleal.ledger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountIT extends AbstractApiIT {

    @Test
    @DisplayName("conta criada nasce com saldo zero e devolve o contrato completo")
    void shouldCreateAccount() {
        ResponseEntity<String> response = post("/accounts", """
                {"holder": "Holder de Teste"}""");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(json(response).read("$.id", String.class)).isNotBlank();
        assertThat(json(response).read("$.holder", String.class)).isEqualTo("Holder de Teste");
        assertThat(json(response).read("$.balance", BigDecimal.class)).isEqualByComparingTo("0.00");
        assertThat(json(response).read("$.createdAt", String.class)).isNotBlank();
    }

    @ParameterizedTest
    @DisplayName("titular ausente ou em branco e recusado")
    @ValueSource(strings = {"null", "\"\"", "\"   \""})
    void shouldRejectMissingHolder(String holderLiteral) {
        ResponseEntity<String> response = post("/accounts", """
                {"holder": %s}""".formatted(holderLiteral));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("titular com 121 caracteres e recusado")
    void shouldRejectHolderAboveMaxLength() {
        ResponseEntity<String> response = post("/accounts", """
                {"holder": "%s"}""".formatted("a".repeat(121)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("titular com exatamente 120 caracteres e aceito")
    void shouldAcceptHolderAtMaxLength() {
        ResponseEntity<String> response = post("/accounts", """
                {"holder": "%s"}""".formatted("a".repeat(120)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("consulta devolve o contrato completo da conta")
    void shouldReturnAccount() {
        String accountId = createAccount("Holder de Teste");

        ResponseEntity<String> response = get("/accounts/" + accountId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json(response).read("$.id", String.class)).isEqualTo(accountId);
        assertThat(json(response).read("$.holder", String.class)).isEqualTo("Holder de Teste");
        assertThat(json(response).read("$.balance", BigDecimal.class)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("conta inexistente devolve 404 no formato RFC 7807")
    void shouldReturnProblemDetailForUnknownAccount() {
        ResponseEntity<String> response = get("/accounts/" + UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType())
                .isNotNull()
                .satisfies(type -> assertThat(type.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).isTrue());
        assertThat(json(response).read("$.status", Integer.class)).isEqualTo(404);
        assertThat(json(response).read("$.title", String.class)).isEqualTo("AccountNotFound");
        // O acento e proposital: prova que a resposta chega em UTF-8 ate o cliente.
        assertThat(json(response).read("$.detail", String.class)).contains("Conta não encontrada");
    }

    @Test
    @DisplayName("id malformado no path e recusado")
    void shouldRejectMalformedAccountId() {
        ResponseEntity<String> response = get("/accounts/nao-e-uuid");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
