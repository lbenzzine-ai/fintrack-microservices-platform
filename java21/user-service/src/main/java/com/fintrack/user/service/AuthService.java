package com.fintrack.user.service;

import com.fintrack.user.dto.LoginRequest;
import com.fintrack.user.dto.LoginResponse;
import com.fintrack.user.dto.RegisterUserRequest;
import com.fintrack.user.dto.UserResponse;
import com.fintrack.user.entity.Role;
import com.fintrack.user.entity.User;
import com.fintrack.user.entity.UserStatus;
import com.fintrack.user.event.UserRegisteredEvent;
import com.fintrack.user.exception.EmailAlreadyExistsException;
import com.fintrack.user.exception.InvalidCredentialsException;
import com.fintrack.user.exception.UsernameAlreadyExistsException;
import com.fintrack.user.mapper.UserMapper;
import com.fintrack.user.messaging.MessagingStrategyRegistry;
import com.fintrack.user.repository.RoleRepository;
import com.fintrack.user.repository.UserRepository;
import com.fintrack.user.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtTokenProvider tokenProvider;
    private final MessagingStrategyRegistry messaging;

    @Value("${fintrack.messaging.kafka.topics.user-registered:fintrack.user.registered}")
    private String userRegisteredTopic;

    @Transactional
    public UserResponse register(RegisterUserRequest req) {
        if (userRepository.existsByEmail(req.email())) throw new EmailAlreadyExistsException(req.email());
        if (userRepository.existsByUsername(req.username())) throw new UsernameAlreadyExistsException(req.username());

        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(DEFAULT_ROLE).build()));

        User user = User.builder()
                .username(req.username())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .firstName(req.firstName())
                .lastName(req.lastName())
                .status(UserStatus.ACTIVE)
                .roles(Set.of(defaultRole))
                .build();

        User saved = userRepository.save(user);
        publishUserRegistered(saved);
        log.info("Registered user uuid={} username={}", saved.getUuid(), saved.getUsername());
        return userMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.usernameOrEmail())
                .or(() -> userRepository.findByUsername(req.usernameOrEmail()))
                .orElseThrow(InvalidCredentialsException::new);

        // Java 21 — exhaustive switch over UserStatus replacing the if/else chain
        boolean allowed = switch (user.getStatus()) {
            case ACTIVE -> passwordEncoder.matches(req.password(), user.getPasswordHash());
            case PENDING_VERIFICATION, SUSPENDED, DELETED -> false;
        };
        if (!allowed) throw new InvalidCredentialsException();

        var access = tokenProvider.issueAccessToken(user);
        var refresh = tokenProvider.issueRefreshToken(user);

        return LoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(access.token())
                .refreshToken(refresh.token())
                .expiresAt(access.expiresAt())
                .user(userMapper.toResponse(user))
                .build();
    }

    private void publishUserRegistered(User user) {
        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID().toString(),
                user.getUuid(),
                user.getUsername(),
                user.getEmail(),
                Instant.now()
        );
        messaging.active().publish(userRegisteredTopic, user.getUuid(), event);
    }
}
