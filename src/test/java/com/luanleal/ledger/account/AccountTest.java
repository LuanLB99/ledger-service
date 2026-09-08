package com.luanleal.ledger.account;

import com.luanleal.ledger.shared.exception.InsufficientBalanceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DisplayName("Invariantes de saldo da conta")
class AccountTest {

    private Account accountWith(String initialBalance) {
        Account account = new Account("Holder de Teste");
        account.credit(new BigDecimal(initialBalance));
        return account;
    }

    @Test
    @DisplayName("Conta nova nasce com saldo zero e escala 2")
    void newAccountStartsAtZeroWithScaleTwo() {
        String holderName =  "Luan Leal";
        Account account = new Account(holderName);

        assertThat(account.getBalance()).isEqualByComparingTo("0.00");
        assertThat(account.getBalance().scale()).isEqualTo(2);
        assertThat(account.getHolder()).isEqualTo(holderName);
    }

    @Test
    @DisplayName("Crédito soma ao saldo")
    void creditAddsToBalance() {
        Account account = accountWith("100.00");

        account.credit(new BigDecimal("50.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Débito menor que o saldo subtrai")
    void debitBelowBalanceSubtracts() {
        Account account = accountWith("100.00");

        account.debit(new BigDecimal("30.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    @DisplayName("Débito do valor exato do saldo zera a conta, sem lancar excecao")
    void debitOfExactBalanceZeroesAccount() {
        Account account = accountWith("100.00");

        account.debit(new BigDecimal("100.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Débito acima do saldo é recusado e o saldo permanece intacto")
    void debitAboveBalanceIsRejectedAndBalanceIsUntouched() {
        Account account = accountWith("100.00");

        assertThatThrownBy(() -> account.debit(new BigDecimal("100.01")))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Crédito de valor zero é recusado")
    void creditOfZeroIsRejected() {
        Account account = accountWith("100.00");

        assertThatThrownBy(() -> account.credit(new BigDecimal("0.00")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Débito de valor negativo é recusado")
    void debitOfNegativeAmountIsRejected() {
        Account account = accountWith("100.00");

        assertThatThrownBy(() -> account.debit(new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Movimentação de valor nulo é recusada")
    void nullAmountIsRejected() {
        Account account = accountWith("100.00");

        assertThatThrownBy(() -> account.credit(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }
}
