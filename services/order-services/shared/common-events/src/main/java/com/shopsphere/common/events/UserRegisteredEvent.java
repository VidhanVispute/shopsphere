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
public class UserRegisteredEvent {

    private UUID userId;
    private String email;
    private String name;
    private String role;         // CUSTOMER or VENDOR
    private LocalDateTime registeredAt;

    // Vendor-specific — null for customers
    private String businessName;
}