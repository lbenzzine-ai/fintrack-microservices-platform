package com.fintrack.user.service;

import com.fintrack.user.dto.LoginRequest;
import com.fintrack.user.dto.LoginResponse;
import com.fintrack.user.dto.RegisterUserRequest;
import com.fintrack.user.dto.UserResponse;
import com.fintrack.user.entity.Role;
import com.fintrack.user.entity.User;
import com.fintrack.user.entity.UserStatus;
import com.fintrack.user.exception.EmailAlreadyExistsException;
import com.fintrack.user.exception.InvalidCredentialsException;
import com.fintrack.user.exception.UsernameAlreadyExistsException;
import com.fintrack.user.mapper.UserMapper;
import com.fintrack.user.messaging.MessagingStrategy;
import com.fintrack.user.messaging.MessagingStrategyRegistry;
import com.fintrack.user.repository.RoleRepository;
import com.fintrack.user.repository.UserRepository;
import com.fintrack.user.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserMapper userMapper;
    @Mock JwtTokenProvider tokenProvider;
    @Mock MessagingStrategyRegistry messaging;
    @Mock MessagingStrategy strategy;

    @InjectMocks AuthService authService;

    private RegisterUserRequest registerReq;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "userRegisteredTopic", "fintrack.user.registered");
        registerReq = new RegisterUserRequest();
        registerReq.setUsername("alice");
        registerReq.setEmail("alice@example.com");
        registerReq.setPassword("Password123!");
        registerReq.setFirstName("Alice");
        registerReq.setLastName("Smith");
    }

    private User userWithStatus(UserStatus status, String passwordHash) {
        return User.builder()
                .id(1L)
                .uuid("user-uuid-1")
                .username("alice")
                .email("alice@example.com")
                .passwordHash(passwordHash)
                .firstName("Alice")
                .lastName("Smith")
                .status(status)
                .roles(Set.of(Role.builder().id(1L).name("USER").build()))
                .build();
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        void register_success_persistsUserPublishesEventReturnsResponse() {
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
            when(userRepository.existsByUsername("alice")).thenReturn(false);
            Role role = Role.builder().id(1L).name("USER").build();
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
            when(passwordEncoder.encode("Password123!")).thenReturn("HASHED");
            User saved = userWithStatus(UserStatus.ACTIVE, "HASHED");
            when(userRepository.save(any(User.class))).thenReturn(saved);
            UserResponse response = UserResponse.builder().uuid("user-uuid-1").username("alice").build();
            when(userMapper.toResponse(saved)).thenReturn(response);
            when(messaging.active()).thenReturn(strategy);

            UserResponse result = authService.register(registerReq);

            assertThat(result).isSameAs(response);
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User toSave = userCaptor.getValue();
            assertThat(toSave.getUsername()).isEqualTo("alice");
            assertThat(toSave.getEmail()).isEqualTo("alice@example.com");
            assertThat(toSave.getPasswordHash()).isEqualTo("HASHED");
            assertThat(toSave.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(toSave.getRoles()).containsExactly(role);
            verify(strategy).publish(eq("fintrack.user.registered"), eq("user-uuid-1"), any());
        }

        @Test
        void register_whenRoleMissing_createsDefaultRole() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(roleRepository.findByName("USER")).thenReturn(Optional.empty());
            Role newRole = Role.builder().id(2L).name("USER").build();
            when(roleRepository.save(any(Role.class))).thenReturn(newRole);
            when(passwordEncoder.encode(anyString())).thenReturn("HASHED");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(messaging.active()).thenReturn(strategy);
            when(userMapper.toResponse(any(User.class))).thenReturn(UserResponse.builder().build());

            authService.register(registerReq);

            verify(roleRepository).save(any(Role.class));
        }

        @Test
        void register_emailExists_throwsEmailAlreadyExists_andDoesNotSave() {
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerReq))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining("alice@example.com");

            verify(userRepository, never()).save(any());
            verifyNoInteractions(passwordEncoder, tokenProvider, messaging);
        }

        @Test
        void register_usernameExists_throwsUsernameAlreadyExists_andDoesNotSave() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername("alice")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerReq))
                    .isInstanceOf(UsernameAlreadyExistsException.class)
                    .hasMessageContaining("alice");

            verify(userRepository, never()).save(any());
            verifyNoInteractions(passwordEncoder, tokenProvider, messaging);
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        private LoginRequest loginReq(String id, String pw) {
            LoginRequest r = new LoginRequest();
            r.setUsernameOrEmail(id);
            r.setPassword(pw);
            return r;
        }

        @Test
        void login_validCredentialsByEmail_returnsTokens() {
            LoginRequest req = loginReq("alice@example.com", "Password123!");
            User user = userWithStatus(UserStatus.ACTIVE, "HASHED");
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("Password123!", "HASHED")).thenReturn(true);
            Instant exp = Instant.now().plusSeconds(60);
            when(tokenProvider.issueAccessToken(user)).thenReturn(new JwtTokenProvider.IssuedToken("access-tok", exp));
            when(tokenProvider.issueRefreshToken(user)).thenReturn(new JwtTokenProvider.IssuedToken("refresh-tok", exp.plusSeconds(3600)));
            UserResponse mapped = UserResponse.builder().uuid("user-uuid-1").username("alice").build();
            when(userMapper.toResponse(user)).thenReturn(mapped);

            LoginResponse out = authService.login(req);

            assertThat(out.getTokenType()).isEqualTo("Bearer");
            assertThat(out.getAccessToken()).isEqualTo("access-tok");
            assertThat(out.getRefreshToken()).isEqualTo("refresh-tok");
            assertThat(out.getExpiresAt()).isEqualTo(exp);
            assertThat(out.getUser()).isSameAs(mapped);
        }

        @Test
        void login_falsBackToUsernameWhenEmailMisses() {
            LoginRequest req = loginReq("alice", "Password123!");
            when(userRepository.findByEmail("alice")).thenReturn(Optional.empty());
            User user = userWithStatus(UserStatus.ACTIVE, "HASHED");
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("Password123!", "HASHED")).thenReturn(true);
            when(tokenProvider.issueAccessToken(user)).thenReturn(new JwtTokenProvider.IssuedToken("a", Instant.now()));
            when(tokenProvider.issueRefreshToken(user)).thenReturn(new JwtTokenProvider.IssuedToken("r", Instant.now()));
            when(userMapper.toResponse(user)).thenReturn(UserResponse.builder().build());

            LoginResponse out = authService.login(req);

            assertThat(out).isNotNull();
            verify(userRepository).findByUsername("alice");
        }

        @Test
        void login_userNotFound_throwsInvalidCredentials() {
            LoginRequest req = loginReq("ghost", "x");
            when(userRepository.findByEmail("ghost")).thenReturn(Optional.empty());
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(InvalidCredentialsException.class);

            verifyNoInteractions(tokenProvider, passwordEncoder);
        }

        @Test
        void login_wrongPassword_throwsInvalidCredentials() {
            LoginRequest req = loginReq("alice@example.com", "wrong");
            User user = userWithStatus(UserStatus.ACTIVE, "HASHED");
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "HASHED")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(tokenProvider, never()).issueAccessToken(any());
        }

        @ParameterizedTest
        @EnumSource(value = UserStatus.class, names = {"PENDING_VERIFICATION", "SUSPENDED", "DELETED"})
        void login_nonActiveStatus_throwsInvalidCredentials(UserStatus status) {
            LoginRequest req = loginReq("alice@example.com", "Password123!");
            User user = userWithStatus(status, "HASHED");
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(InvalidCredentialsException.class);

            verify(tokenProvider, never()).issueAccessToken(any());
        }
    }
}
