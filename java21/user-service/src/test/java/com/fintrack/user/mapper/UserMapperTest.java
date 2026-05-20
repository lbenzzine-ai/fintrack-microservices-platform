package com.fintrack.user.mapper;

import com.fintrack.user.dto.RegisterUserRequest;
import com.fintrack.user.dto.UserResponse;
import com.fintrack.user.entity.Role;
import com.fintrack.user.entity.User;
import com.fintrack.user.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void toEntity_copiesUsernameEmailNamesAndIgnoresIdentityFields() {
        RegisterUserRequest req = new RegisterUserRequest(
                "alice", "alice@example.com", "Password123!", "Alice", "Smith");

        User user = mapper.toEntity(req);

        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getFirstName()).isEqualTo("Alice");
        assertThat(user.getLastName()).isEqualTo("Smith");
        assertThat(user.getId()).isNull();
        assertThat(user.getUuid()).isNull();
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getStatus()).isNull();
    }

    @Test
    void toResponse_mapsAllFieldsAndCollapsesRolesToNames() {
        Instant created = Instant.parse("2024-01-01T00:00:00Z");
        Instant updated = Instant.parse("2024-02-01T00:00:00Z");
        Set<Role> roles = new HashSet<>();
        roles.add(Role.builder().id(1L).name("USER").build());
        roles.add(Role.builder().id(2L).name("ADMIN").build());
        User user = User.builder()
                .id(7L)
                .uuid("uuid-7")
                .username("alice")
                .email("alice@example.com")
                .passwordHash("HASH")
                .firstName("Alice")
                .lastName("Smith")
                .status(UserStatus.ACTIVE)
                .createdAt(created)
                .updatedAt(updated)
                .roles(roles)
                .build();

        UserResponse resp = mapper.toResponse(user);

        assertThat(resp.uuid()).isEqualTo("uuid-7");
        assertThat(resp.username()).isEqualTo("alice");
        assertThat(resp.email()).isEqualTo("alice@example.com");
        assertThat(resp.firstName()).isEqualTo("Alice");
        assertThat(resp.lastName()).isEqualTo("Smith");
        assertThat(resp.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(resp.createdAt()).isEqualTo(created);
        assertThat(resp.updatedAt()).isEqualTo(updated);
        assertThat(resp.roles()).containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    void roleNames_nullRoles_returnsEmptySet() {
        assertThat(mapper.roleNames(null)).isEmpty();
    }

    @Test
    void roleNames_emptyRoles_returnsEmptySet() {
        assertThat(mapper.roleNames(Set.of())).isEmpty();
    }
}
