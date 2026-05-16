package com.fintrack.notification.strategy.channel;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DispatchResult {
    private boolean delivered;
    private String provider;
    private String failureReason;
}
