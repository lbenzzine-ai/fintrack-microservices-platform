package com.fintrack.notification.strategy.channel;

public record DispatchResult(
        boolean delivered,
        String provider,
        String failureReason
) {
    public static DispatchResult ok(String provider) {
        return new DispatchResult(true, provider, null);
    }
    public static DispatchResult fail(String provider, String reason) {
        return new DispatchResult(false, provider, reason);
    }
}
