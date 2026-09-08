package com.luanleal.ledger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;


public abstract class AbstractApiIT extends AbstractIT {

    private static final Configuration JSON = jsonConfiguration();

    @Autowired
    protected TestRestTemplate rest;

    private static Configuration jsonConfiguration() {
        ObjectMapper mapper = new ObjectMapper()
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        return Configuration.builder()
                .jsonProvider(new JacksonJsonProvider(mapper))
                .mappingProvider(new JacksonMappingProvider(mapper))
                .build();
    }

    protected DocumentContext json(ResponseEntity<String> response) {
        return JsonPath.using(JSON).parse(response.getBody());
    }

    protected ResponseEntity<String> post(String path, String body) {
        return post(path, body, null);
    }

    protected ResponseEntity<String> post(String path, String body, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) {
            headers.set("Idempotency-Key", idempotencyKey);
        }
        return asUtf8(rest.exchange(path, HttpMethod.POST,
                new HttpEntity<>(body.getBytes(StandardCharsets.UTF_8), headers), byte[].class));
    }

    protected ResponseEntity<String> get(String path) {
        return asUtf8(rest.getForEntity(path, byte[].class));
    }

    private ResponseEntity<String> asUtf8(ResponseEntity<byte[]> raw) {
        String body = raw.getBody() == null ? null : new String(raw.getBody(), StandardCharsets.UTF_8);
        return new ResponseEntity<>(body, raw.getHeaders(), raw.getStatusCode());
    }

    // ---------- atalhos de dominio ----------

    protected String createAccount(String holder) {
        ResponseEntity<String> response = post("/accounts", """
                {"holder": "%s"}""".formatted(holder));
        return json(response).read("$.id", String.class);
    }

    protected ResponseEntity<String> movement(String accountId, String type, String amount, String key) {
        return post("/accounts/%s/transactions".formatted(accountId), """
                {"type": "%s", "amount": %s}""".formatted(type, amount), key);
    }

    protected BigDecimal balanceOf(String accountId) {
        return json(get("/accounts/" + accountId)).read("$.balance", BigDecimal.class);
    }
}
