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
        RegisterUserRequest req = new RegisterUserRequest();
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("Password123!");
        req.setFirstName("Alice");
        req.setLastName("Smith");

        User user = mapper.toEntity(req);

        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getFirstName()).isEqualTo("Alice");
        assertThat(user.getLastName()).isEqualTo("Smith");
        // ignored fields stay null/default
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

        assertThat(resp.getUuid()).isEqualTo("uuid-7");
        assertThat(resp.getUsername()).isEqualTo("alice");
        assertThat(resp.getEmail()).isEqualTo("alice@example.com");
        assertThat(resp.getFirstName()).isEqualTo("Alice");
        assertThat(resp.getLastName()).isEqualTo("Smith");
        assertThat(resp.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(resp.getCreatedAt()).isEqualTo(created);
        assertThat(resp.getUpdatedAt()).isEqualTo(updated);
        assertThat(resp.getRoles()).containsExactlyInAnyOrder("USER", "ADMIN");
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
