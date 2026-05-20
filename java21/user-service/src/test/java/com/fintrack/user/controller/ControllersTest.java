package com.fintrack.user.controller;

import com.fintrack.user.dto.LoginRequest;
import com.fintrack.user.dto.LoginResponse;
import com.fintrack.user.dto.RegisterUserRequest;
import com.fintrack.user.dto.UpdateUserRequest;
import com.fintrack.user.dto.UserResponse;
import com.fintrack.user.service.AuthService;
import com.fintrack.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControllersTest {

    @Mock AuthService authService;
    @Mock UserService userService;

    @InjectMocks AuthController authController;
    @InjectMocks UserController userController;

    private UserResponse user() {
        return UserResponse.builder()
                .uuid("u1").username("alice").email("alice@x").build();
    }

    @Test
    void shouldReturnRegisteredUserFromAuthController() {
        RegisterUserRequest req = new RegisterUserRequest("a", "a@x", "pass1234", "A", "B");
        UserResponse expected = user();
        when(authService.register(req)).thenReturn(expected);

        assertThat(authController.register(req)).isSameAs(expected);
    }

    @Test
    void shouldReturnLoginResponseFromAuthController() {
        LoginRequest req = new LoginRequest("alice", "pass");
        LoginResponse expected = LoginResponse.builder()
                .tokenType("Bearer").accessToken("at").refreshToken("rt")
                .expiresAt(Instant.now()).build();
        when(authService.login(req)).thenReturn(expected);

        assertThat(authController.login(req)).isSameAs(expected);
    }

    @Test
    void shouldReturnUserFromGetOne() {
        UserResponse expected = user();
        when(userService.findByUuid("u1")).thenReturn(expected);

        assertThat(userController.getOne("u1")).isSameAs(expected);
    }

    @Test
    void shouldReturnPageFromList() {
        Page<UserResponse> expected = new PageImpl<>(List.of(user()), PageRequest.of(0, 10), 1);
        when(userService.list(PageRequest.of(0, 10))).thenReturn(expected);

        assertThat(userController.list(PageRequest.of(0, 10))).isSameAs(expected);
    }

    @Test
    void shouldReturnUpdatedUserFromUpdate() {
        UpdateUserRequest req = new UpdateUserRequest("New", "Name");
        UserResponse expected = user();
        when(userService.update("u1", req)).thenReturn(expected);

        assertThat(userController.update("u1", req)).isSameAs(expected);
    }

    @Test
    void shouldCallDeleteAndReturnNoContent() {
        ResponseEntity<Void> response = userController.delete("u1");

        verify(userService).delete("u1");
        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }
}
