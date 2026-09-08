package com.luanleal.ledger.shared.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class InsufficientBalanceException extends DomainException {

    public InsufficientBalanceException(BigDecimal balance, BigDecimal requested) {
        super(HttpStatus.UNPROCESSABLE_ENTITY,
                "Saldo insuficiente. Disponivel: " + balance + ", solicitado: " + requested);
    }
}
