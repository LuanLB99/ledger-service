package com.luanleal.ledger.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luanleal.ledger.shared.exception.IdempotencyKeyReuseException;
import com.luanleal.ledger.shared.exception.RequestInProgressException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;


@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public String hash(Object... parts) {
        try {
            StringBuilder raw = new StringBuilder();
            for (Object part : parts) {
                raw.append(objectMapper.writeValueAsString(part)).append('|');
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Falha ao calcular o hash da requisicao", e);
        }
    }


    public <T> Optional<T> findCompleted(String key, String requestHash, Class<T> type) {
        return repository.findById(key).map(record -> {
            if (!record.matches(requestHash)) {
                throw new IdempotencyKeyReuseException(key);
            }
            if (!record.isCompleted()) {
                throw new RequestInProgressException(key);
            }
            return deserialize(record.getResponseBody(), type);
        });
    }

    public void reserve(String key, String requestHash) {
        try {
            repository.saveAndFlush(new IdempotencyRecord(key, requestHash));
        } catch (DataIntegrityViolationException e) {
            throw new RequestInProgressException(key);
        }
    }

    public void complete(String key, int status, Object response) {
        repository.findById(key).ifPresent(record -> record.complete(status, serialize(response)));
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar a resposta", e);
        }
    }

    private <T> T deserialize(String body, Class<T> type) {
        try {
            return objectMapper.readValue(body, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao desserializar a resposta armazenada", e);
        }
    }
}
