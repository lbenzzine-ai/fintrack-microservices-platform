package com.fintrack.notification.controller;

import com.fintrack.notification.dto.NotificationResponse;
import com.fintrack.notification.dto.SendNotificationRequest;
import com.fintrack.notification.dto.UpdatePreferenceRequest;
import com.fintrack.notification.entity.NotificationPreference;
import com.fintrack.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notifications", description = "Send notifications and manage per-user preferences")
@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Send a notification directly (admin / test path)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse send(@Valid @RequestBody SendNotificationRequest req) {
        return notificationService.create(req);
    }

    @Operation(summary = "Get a notification by UUID")
    @GetMapping("/{uuid}")
    public NotificationResponse get(@PathVariable String uuid) {
        return notificationService.findByUuid(uuid);
    }

    @Operation(summary = "List notifications for an account")
    @GetMapping("/by-account/{accountUuid}")
    public Page<NotificationResponse> byAccount(@PathVariable String accountUuid, Pageable pageable) {
        return notificationService.findByAccount(accountUuid, pageable);
    }

    @Operation(summary = "Read user opt-in preferences")
    @GetMapping("/preferences/{userUuid}")
    public ResponseEntity<NotificationPreference> readPref(@PathVariable String userUuid) {
        return notificationService.findPreference(userUuid)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Upsert user opt-in preferences (idempotent)")
    @PutMapping("/preferences/{userUuid}")
    public NotificationPreference upsertPref(@PathVariable String userUuid,
                                             @Valid @RequestBody UpdatePreferenceRequest req) {
        NotificationPreference incoming = NotificationPreference.builder()
                .userUuid(userUuid)
                .emailEnabled(Boolean.TRUE.equals(req.emailEnabled()))
                .smsEnabled(Boolean.TRUE.equals(req.smsEnabled()))
                .pushEnabled(Boolean.TRUE.equals(req.pushEnabled()))
                .emailAddress(req.emailAddress())
                .phoneNumber(req.phoneNumber())
                .pushToken(req.pushToken())
                .build();
        return notificationService.upsertPreference(userUuid, incoming);
    }
}
