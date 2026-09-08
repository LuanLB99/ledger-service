package com.luanleal.ledger.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountRequest(

        @NotBlank(message = "O nome do titular é obrigatório")
        @Size(max = 120, message = "O titular deve ter no máximo 120 caracteres")
        String holder
) {
}
