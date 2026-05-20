package com.fintrack.transaction.controller;

import com.fintrack.transaction.config.SecurityConfig;
import com.fintrack.transaction.security.JwtAuthFilter;
import com.fintrack.transaction.security.JwtTokenProvider;
import com.fintrack.transaction.service.FeeService;
import com.fintrack.transaction.service.TransactionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
class TransactionControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockBean TransactionService transactionService;
    @MockBean FeeService feeService;
    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void passThroughFilter() throws Exception {
        doAnswer(inv -> {
            ServletRequest req = inv.getArgument(0);
            ServletResponse resp = inv.getArgument(1);
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(req, resp);
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    void shouldRejectUnauthenticatedGet() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/tx-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnauthenticatedByAccount() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/by-account/acc-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnauthenticatedQuote() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/fee/quote")
                        .param("type", "DOMESTIC_TRANSFER")
                        .param("amount", "100"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldAllowAuthenticatedGet() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/tx-1"))
                .andExpect(status().is2xxSuccessful());
    }
}
