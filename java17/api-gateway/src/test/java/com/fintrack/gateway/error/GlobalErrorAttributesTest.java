package com.fintrack.gateway.error;

import com.fintrack.gateway.filter.CorrelationIdGlobalFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalErrorAttributesTest {

    private final GlobalErrorAttributes attrs = new GlobalErrorAttributes();

    private ServerRequest serverRequest(MockServerWebExchange exchange) {
        List<HttpMessageReader<?>> readers = HandlerStrategies.withDefaults().messageReaders();
        return ServerRequest.create(exchange, readers);
    }

    @Test
    void addsTimestampAndCorrelationId() {
        MockServerHttpRequest req = MockServerHttpRequest.get("/oops")
                .header(CorrelationIdGlobalFilter.HEADER, "cid-xyz")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        // Seed a throwable so DefaultErrorAttributes.getError() finds something.
        attrs.storeErrorInformation(new RuntimeException("boom"), exchange);
        ServerRequest sr = serverRequest(exchange);

        Map<String, Object> result = attrs.getErrorAttributes(sr, ErrorAttributeOptions.defaults());

        assertThat(result.get("timestamp")).isNotNull();
        assertThat(result.get("correlationId")).isEqualTo("cid-xyz");
        assertThat(result).doesNotContainKey("requestId");
    }

    @Test
    void nullCorrelationIdWhenHeaderMissing() {
        MockServerHttpRequest req = MockServerHttpRequest.get("/oops").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(req);
        attrs.storeErrorInformation(new RuntimeException("boom"), exchange);
        ServerRequest sr = serverRequest(exchange);

        Map<String, Object> result = attrs.getErrorAttributes(sr, ErrorAttributeOptions.defaults());

        assertThat(result.get("timestamp")).isNotNull();
        assertThat(result.get("correlationId")).isNull();
    }
}
