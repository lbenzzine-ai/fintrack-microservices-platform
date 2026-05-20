package com.fintrack.user.service;

import com.fintrack.user.dto.UpdateUserRequest;
import com.fintrack.user.dto.UserResponse;
import com.fintrack.user.entity.User;
import com.fintrack.user.entity.UserStatus;
import com.fintrack.user.exception.UserNotFoundException;
import com.fintrack.user.mapper.UserMapper;
import com.fintrack.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;

    @InjectMocks UserService userService;

    private User user(String uuid) {
        return User.builder()
                .id(1L)
                .uuid(uuid)
                .username("alice")
                .email("alice@example.com")
                .passwordHash("HASH")
                .firstName("Alice")
                .lastName("Smith")
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void findByUuid_existing_returnsMappedResponse() {
        User u = user("uuid-1");
        UserResponse mapped = UserResponse.builder().uuid("uuid-1").username("alice").build();
        when(userRepository.findByUuidWithRoles("uuid-1")).thenReturn(Optional.of(u));
        when(userMapper.toResponse(u)).thenReturn(mapped);

        UserResponse out = userService.findByUuid("uuid-1");

        assertThat(out).isSameAs(mapped);
    }

    @Test
    void findByUuid_missing_throwsUserNotFound() {
        when(userRepository.findByUuidWithRoles("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUuid("missing"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void list_mapsPageContent() {
        Pageable pageable = PageRequest.of(0, 10);
        User u = user("uuid-1");
        Page<User> page = new PageImpl<>(List.of(u), pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(page);
        UserResponse mapped = UserResponse.builder().uuid("uuid-1").build();
        when(userMapper.toResponse(u)).thenReturn(mapped);

        Page<UserResponse> out = userService.list(pageable);

        assertThat(out.getTotalElements()).isEqualTo(1);
        assertThat(out.getContent()).containsExactly(mapped);
    }

    @Test
    void update_updatesProvidedFields_savesAndReturnsMapped() {
        User u = user("uuid-1");
        when(userRepository.findByUuidWithRoles("uuid-1")).thenReturn(Optional.of(u));
        UpdateUserRequest req = new UpdateUserRequest();
        req.setFirstName("Alicia");
        req.setLastName("Doe");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        UserResponse mapped = UserResponse.builder().uuid("uuid-1").firstName("Alicia").lastName("Doe").build();
        when(userMapper.toResponse(any(User.class))).thenReturn(mapped);

        UserResponse out = userService.update("uuid-1", req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getFirstName()).isEqualTo("Alicia");
        assertThat(captor.getValue().getLastName()).isEqualTo("Doe");
        assertThat(out).isSameAs(mapped);
    }

    @Test
    void update_nullFields_leaveUntouched() {
        User u = user("uuid-1");
        u.setFirstName("orig-first");
        u.setLastName("orig-last");
        when(userRepository.findByUuidWithRoles("uuid-1")).thenReturn(Optional.of(u));
        UpdateUserRequest req = new UpdateUserRequest();
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(UserResponse.builder().build());

        userService.update("uuid-1", req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getFirstName()).isEqualTo("orig-first");
        assertThat(captor.getValue().getLastName()).isEqualTo("orig-last");
    }

    @Test
    void update_missing_throwsUserNotFound() {
        when(userRepository.findByUuidWithRoles("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update("nope", new UpdateUserRequest()))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void delete_existing_deletesViaRepository() {
        User u = user("uuid-1");
        when(userRepository.findByUuidWithRoles("uuid-1")).thenReturn(Optional.of(u));

        userService.delete("uuid-1");

        verify(userRepository).delete(u);
    }

    @Test
    void delete_missing_throwsUserNotFound() {
        when(userRepository.findByUuidWithRoles("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete("nope"))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).delete(any(User.class));
    }
}
