package com.luanleal.ledger.account;

import com.luanleal.ledger.shared.exception.InsufficientBalanceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String holder;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Account() {
    }


    public Account(String holder) {
        this.id = UUID.randomUUID();
        this.holder = holder;
        this.balance = BigDecimal.ZERO.setScale(2);
        this.createdAt = Instant.now();
    }

    public void credit(BigDecimal amount) {
        requirePositive(amount);
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        requirePositive(amount);
        hasEnoughBalance(amount);
        this.balance = this.balance.subtract(amount);
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da movimentacao deve ser positivo.");
        }
    }

    private void hasEnoughBalance(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0)
            throw new InsufficientBalanceException(this.balance, amount);
    }

    public UUID getId() {
        return id;
    }

    public String getHolder() {
        return holder;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
