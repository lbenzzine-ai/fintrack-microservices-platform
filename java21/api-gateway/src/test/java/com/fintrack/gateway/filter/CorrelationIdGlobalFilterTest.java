package com.fintrack.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdGlobalFilterTest {

    private final CorrelationIdGlobalFilter filter = new CorrelationIdGlobalFilter();

    @Test
    void generatesCorrelationId_whenHeaderMissing_andSetsOnResponse() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/anything").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicBoolean chainCalled = new AtomicBoolean();
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chainCalled).isTrue();
        String responseHeader = exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.HEADER);
        assertThat(responseHeader).isNotNull();
        // Must be a parseable UUID since none was provided
        assertThat(UUID.fromString(responseHeader)).isNotNull();
    }

    @Test
    void preservesCorrelationId_whenHeaderProvided() {
        String existing = "external-cid-abc";
        MockServerHttpRequest request = MockServerHttpRequest.get("/anything")
                .header(CorrelationIdGlobalFilter.HEADER, existing)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.HEADER))
                .isEqualTo(existing);
    }

    @Test
    void generatesNewId_whenHeaderIsBlank() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/anything")
                .header(CorrelationIdGlobalFilter.HEADER, "   ")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        String responseHeader = exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.HEADER);
        assertThat(responseHeader).isNotBlank();
        assertThat(UUID.fromString(responseHeader)).isNotNull();
    }

    @Test
    void order_isHighestPrecedence() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}
