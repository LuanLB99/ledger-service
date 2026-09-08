package com.luanleal.ledger.shared.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AccountNotFoundException extends DomainException {

    public AccountNotFoundException(UUID accountId) {
        super(HttpStatus.NOT_FOUND, "Conta não encontrada: " + accountId);
    }
}
