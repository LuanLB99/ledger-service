package com.luanleal.ledger.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String holder,
        BigDecimal balance,
        Instant createdAt
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getHolder(),
                account.getBalance(),
                account.getCreatedAt()
        );
    }
}
