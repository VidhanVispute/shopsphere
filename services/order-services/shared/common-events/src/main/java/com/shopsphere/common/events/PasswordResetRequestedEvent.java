package com.shopsphere.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequestedEvent {

    private UUID userId;
    private String email;
    private String resetToken;    // raw token — Notification Service puts it in the email link
    private LocalDateTime expiresAt;
}