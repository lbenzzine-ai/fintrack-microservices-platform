package com.fintrack.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FallbackResponse {
    private String service;
    private String message;
    private String hint;
    private int status;
    private Instant timestamp;
    private String correlationId;
}
