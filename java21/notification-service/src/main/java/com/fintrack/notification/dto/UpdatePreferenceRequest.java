package com.fintrack.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdatePreferenceRequest(
        Boolean emailEnabled,
        Boolean smsEnabled,
        Boolean pushEnabled,
        @Email @Size(max = 255) String emailAddress,
        @Size(max = 32) String phoneNumber,
        @Size(max = 255) String pushToken
) {}
