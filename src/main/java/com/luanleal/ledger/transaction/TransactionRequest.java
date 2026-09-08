package com.luanleal.ledger.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransactionRequest(

        @NotNull(message = "O tipo é obrigatório: CRÉDITO ou DÉBITO")
        TransactionType type,

        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor minimo é 0.01")
        @Digits(integer = 17, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais")
        BigDecimal amount
) {
}
