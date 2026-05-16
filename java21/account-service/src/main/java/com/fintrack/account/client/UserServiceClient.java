package com.fintrack.account.client;

import com.fintrack.account.dto.UserSnapshot;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Optional;

/**
 * Feign client for user-service, guarded by Resilience4j Circuit Breaker + Retry.
 * Used to enrich/verify users at the account boundary (e.g. confirming a userUuid exists
 * before creating a wallet). The CB instance {@code userServiceClient} is configured in
 * {@code account-service.yml}.
 *
 * <p>Note: discovery is via Eureka — the path-name {@code user-service} is the registered name.
 */
@FeignClient(name = "user-service", path = "/api/v1/users")
public interface UserServiceClient {

    @CircuitBreaker(name = "userServiceClient", fallbackMethod = "fallbackFindByUuid")
    @Retry(name = "userServiceClient")
    @GetMapping("/{uuid}")
    UserSnapshot findByUuid(@PathVariable("uuid") String uuid,
                            @RequestHeader("Authorization") String authorization);

    default Optional<UserSnapshot> fallbackFindByUuid(String uuid, String auth, Throwable ex) {
        // Open-circuit fallback: account-service continues without enriched user data.
        return Optional.empty();
    }
}
