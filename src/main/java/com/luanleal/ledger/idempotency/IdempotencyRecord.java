package com.luanleal.ledger.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_key")
    private String key;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String key, String requestHash) {
        this.key = key;
        this.requestHash = requestHash;
        this.createdAt = Instant.now();
    }

    public void complete(int status, String body) {
        this.responseStatus = status;
        this.responseBody = body;
    }

    public boolean isCompleted() {
        return responseBody != null;
    }

    public boolean matches(String hash) {
        return this.requestHash.equals(hash);
    }

    public String getKey() {
        return key;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }
}
