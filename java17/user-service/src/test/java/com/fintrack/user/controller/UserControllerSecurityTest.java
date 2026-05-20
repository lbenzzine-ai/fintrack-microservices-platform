package com.fintrack.user.controller;

import com.fintrack.user.config.SecurityConfig;
import com.fintrack.user.security.JwtAuthFilter;
import com.fintrack.user.security.JwtTokenProvider;
import com.fintrack.user.service.UserService;
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

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
class UserControllerSecurityTest {

    @Autowired MockMvc mockMvc;
    @MockBean UserService userService;
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
    void shouldRejectUnauthenticatedGetUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/user-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnauthenticatedListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldAllowAuthenticatedGetUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/user-1"))
                .andExpect(status().is2xxSuccessful());
    }
}
