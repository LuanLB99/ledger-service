package com.luanleal.ledger.transaction;

import com.luanleal.ledger.account.Account;
import com.luanleal.ledger.account.AccountRepository;
import com.luanleal.ledger.idempotency.IdempotencyService;
import com.luanleal.ledger.shared.exception.AccountNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyService idempotencyService;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              IdempotencyService idempotencyService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public TransactionResponse execute(UUID accountId, String idempotencyKey, TransactionRequest request) {
        String requestHash = idempotencyService.hash(accountId, request);

        Optional<TransactionResponse> replay =
                idempotencyService.findCompleted(idempotencyKey, requestHash, TransactionResponse.class);
        if (replay.isPresent()) {
            return replay.get();
        }

        idempotencyService.reserve(idempotencyKey, requestHash);

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        BigDecimal amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);

        switch (request.type()) {
            case CREDIT -> account.credit(amount);
            case DEBIT-> account.debit(amount);
        }

        Transaction transaction = transactionRepository.save(
                new Transaction(accountId, request.type(), amount, account.getBalance()));

        TransactionResponse response = TransactionResponse.from(transaction);
        idempotencyService.complete(idempotencyKey, 201, response);

        return response;
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> statement(UUID accountId, Pageable pageable) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        return transactionRepository
                .findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
                .map(TransactionResponse::from);
    }
}
