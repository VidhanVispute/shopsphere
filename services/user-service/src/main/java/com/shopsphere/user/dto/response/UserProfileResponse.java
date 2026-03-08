package com.shopsphere.user.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String email;
    private String name;
    private String phone;
    private String avatarUrl;
    private String street;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}