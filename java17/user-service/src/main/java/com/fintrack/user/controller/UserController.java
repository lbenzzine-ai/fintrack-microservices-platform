package com.fintrack.user.controller;

import com.fintrack.user.dto.UpdateUserRequest;
import com.fintrack.user.dto.UserResponse;
import com.fintrack.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users", description = "Authenticated user resource")
@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Find user by UUID")
    @GetMapping("/{uuid}")
    public UserResponse getOne(@PathVariable String uuid) {
        return userService.findByUuid(uuid);
    }

    @Operation(summary = "List users (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public Page<UserResponse> list(Pageable pageable) {
        return userService.list(pageable);
    }

    @Operation(summary = "Update mutable user fields")
    @PatchMapping("/{uuid}")
    public UserResponse update(@PathVariable String uuid, @Valid @RequestBody UpdateUserRequest req) {
        return userService.update(uuid, req);
    }

    @Operation(summary = "Delete user (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> delete(@PathVariable String uuid) {
        userService.delete(uuid);
        return ResponseEntity.noContent().build();
    }
}
