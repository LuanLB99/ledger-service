package com.luanleal.ledger.shared.exception;

import org.springframework.http.HttpStatus;

public class IdempotencyKeyReuseException extends DomainException {

    public IdempotencyKeyReuseException(String key) {
        super(HttpStatus.UNPROCESSABLE_ENTITY,
                "A chave de idempotencia '" + key + "' ja foi usada com outro payload.");
    }
}
