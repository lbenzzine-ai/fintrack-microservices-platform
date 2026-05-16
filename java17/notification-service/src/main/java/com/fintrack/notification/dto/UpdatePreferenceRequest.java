package com.fintrack.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePreferenceRequest {
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private Boolean pushEnabled;

    @Email @Size(max = 255)
    private String emailAddress;

    @Size(max = 32)
    private String phoneNumber;

    @Size(max = 255)
    private String pushToken;
}
