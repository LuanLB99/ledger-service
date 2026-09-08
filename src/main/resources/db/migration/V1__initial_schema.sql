-- ============================================================================
-- V1: schema inicial do ledger
--
-- Decisoes registradas aqui:
--   * NUMERIC(19,2) para dinheiro. Nunca ponto flutuante.
--   * CHECK de saldo nao-negativo: defesa em profundidade. Mesmo que a regra
--     de negocio falhe, o banco recusa. O ultimo guardiao da invariante e o
--     schema, nao o codigo.
--   * transactions e append-only: nao ha UPDATE nem DELETE previstos.
-- ============================================================================

CREATE TABLE accounts (
    id         UUID           PRIMARY KEY,
    holder     VARCHAR(120)   NOT NULL,
    balance    NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT chk_accounts_balance_non_negative CHECK (balance >= 0)
);

CREATE TABLE transactions (
    id            UUID           PRIMARY KEY,
    account_id    UUID           NOT NULL REFERENCES accounts (id),
    type          VARCHAR(10)    NOT NULL,
    amount        NUMERIC(19, 2) NOT NULL,
    balance_after NUMERIC(19, 2) NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_transactions_type CHECK (type IN ('CREDIT', 'DEBIT'))
);

CREATE INDEX idx_transactions_account_created
    ON transactions (account_id, created_at DESC);

-- A PRIMARY KEY nesta tabela e o mecanismo de idempotencia.
-- Nao e o codigo que garante unicidade: e o banco, sob concorrencia real.
CREATE TABLE idempotency_records (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    request_hash    VARCHAR(64)  NOT NULL,
    response_status INTEGER,
    response_body   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
