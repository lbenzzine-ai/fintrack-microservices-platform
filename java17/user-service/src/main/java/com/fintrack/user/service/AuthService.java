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
        if (userRepository.existsByEmail(req.getEmail())) throw new EmailAlreadyExistsException(req.getEmail());
        if (userRepository.existsByUsername(req.getUsername())) throw new UsernameAlreadyExistsException(req.getUsername());

        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(DEFAULT_ROLE).build()));

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
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
        User user = userRepository.findByEmail(req.getUsernameOrEmail())
                .or(() -> userRepository.findByUsername(req.getUsernameOrEmail()))
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

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
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userUuid(user.getUuid())
                .username(user.getUsername())
                .email(user.getEmail())
                .occurredAt(Instant.now())
                .build();
        messaging.active().publish(userRegisteredTopic, user.getUuid(), event);
    }
}
