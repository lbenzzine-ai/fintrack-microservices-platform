package com.fintrack.notification.controller;

import com.fintrack.notification.config.SecurityConfig;
import com.fintrack.notification.security.JwtAuthFilter;
import com.fintrack.notification.security.JwtTokenProvider;
import com.fintrack.notification.service.NotificationService;
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

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
class NotificationControllerSecurityTest {

    @Autowired MockMvc mockMvc;
    @MockBean NotificationService notificationService;
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
    void shouldRejectUnauthenticatedGetNotification() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/notif-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnauthenticatedByAccount() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/by-account/acc-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnauthenticatedReadPreferences() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/preferences/user-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldAllowAuthenticatedGetNotification() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/notif-1"))
                .andExpect(status().is2xxSuccessful());
    }
}
