package com.luanleal.ledger.shared.exception;

import org.springframework.http.HttpStatus;

public class RequestInProgressException extends DomainException {

    public RequestInProgressException(String key) {
        super(HttpStatus.CONFLICT,
                "Ja existe uma requisicao em andamento para a chave '" + key + "'.");
    }
}
