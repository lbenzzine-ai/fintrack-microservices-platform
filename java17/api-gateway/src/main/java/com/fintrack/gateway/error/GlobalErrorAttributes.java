package com.fintrack.gateway.error;

import com.fintrack.gateway.filter.CorrelationIdGlobalFilter;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.time.Instant;
import java.util.Map;

/**
 * Customizes the response shape for unhandled errors at the gateway level so that
 * clients get a consistent JSON error payload with a correlation id.
 */
@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> attrs = super.getErrorAttributes(request, options);
        attrs.put("timestamp", Instant.now().toString());
        attrs.put("correlationId", request.headers().firstHeader(CorrelationIdGlobalFilter.HEADER));
        attrs.remove("requestId");
        return attrs;
    }
}
