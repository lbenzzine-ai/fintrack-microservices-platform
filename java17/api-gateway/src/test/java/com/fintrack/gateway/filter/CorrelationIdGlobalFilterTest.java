package com.fintrack.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdGlobalFilterTest {

    private final CorrelationIdGlobalFilter filter = new CorrelationIdGlobalFilter();

    @Test
    void generatesCorrelationId_whenHeaderMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/anything").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<String> seenHeader = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            seenHeader.set(ex.getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.HEADER));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        String generated = seenHeader.get();
        assertThat(generated).isNotNull();
        // Should parse as a UUID
        assertThat(UUID.fromString(generated)).isNotNull();
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.HEADER))
                .isEqualTo(generated);
    }

    @Test
    void preservesCorrelationId_whenHeaderProvided() {
        String existing = "external-cid-abc";
        MockServerHttpRequest request = MockServerHttpRequest.get("/anything")
                .header(CorrelationIdGlobalFilter.HEADER, existing)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<String> seenHeader = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            seenHeader.set(ex.getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.HEADER));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(seenHeader.get()).isEqualTo(existing);
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.HEADER))
                .isEqualTo(existing);
    }

    @Test
    void generatesNewId_whenHeaderIsBlank() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/anything")
                .header(CorrelationIdGlobalFilter.HEADER, "   ")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<String> seenHeader = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            seenHeader.set(ex.getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.HEADER));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        String generated = seenHeader.get();
        assertThat(generated).isNotBlank();
        assertThat(generated.trim()).isNotEmpty();
        assertThat(UUID.fromString(generated)).isNotNull();
    }

    @Test
    void order_isHighestPrecedence() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}
