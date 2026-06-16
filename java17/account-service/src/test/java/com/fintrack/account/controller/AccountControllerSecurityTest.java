package com.fintrack.account.controller;

import com.fintrack.account.config.SecurityConfig;
import com.fintrack.account.dto.AccountResponse;
import com.fintrack.account.security.JwtAuthFilter;
import com.fintrack.account.security.JwtTokenProvider;
import com.fintrack.account.service.AccountService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
class AccountControllerSecurityTest {

    @Autowired MockMvc mockMvc;
    @MockBean AccountService accountService;
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
    void shouldRejectUnauthenticatedGetAccount() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/acc-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnauthenticatedGetBalance() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/acc-1/balance"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnauthenticatedMine() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldAllowAuthenticatedGetAccount() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/acc-1"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void shouldOpenAccountUsingJwtPrincipalNotHeader() throws Exception {
        when(accountService.createForUser(eq("user-uuid-from-jwt"), any()))
                .thenReturn(mock(AccountResponse.class));

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/accounts")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user("user-uuid-from-jwt"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currencyCode\":\"USD\"}")
                        .header("X-User-Id", "attacker-uuid"))
                .andExpect(status().isCreated());

        verify(accountService).createForUser(eq("user-uuid-from-jwt"), eq("USD"));
    }

    @Test
    void shouldReturnMineUsingJwtPrincipalNotHeader() throws Exception {
        when(accountService.findByUserUuid(eq("user-uuid-from-jwt")))
                .thenReturn(mock(AccountResponse.class));

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/accounts/me")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user("user-uuid-from-jwt"))
                        .header("X-User-Id", "attacker-uuid"))
                .andExpect(status().isOk());

        verify(accountService).findByUserUuid(eq("user-uuid-from-jwt"));
    }
}
