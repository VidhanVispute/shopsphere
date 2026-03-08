package com.shopsphere.user.service;

import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.exception.ResourceNotFoundException;
import com.shopsphere.user.dto.request.UpdateProfileRequest;
import com.shopsphere.user.dto.response.UserProfileResponse;
import com.shopsphere.user.entity.UserProfile;
import com.shopsphere.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileResponse getMyProfile(String userId) {
        UserProfile profile = findById(UUID.fromString(userId));
        return toResponse(profile);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(String userId, UpdateProfileRequest request) {
        UserProfile profile = findById(UUID.fromString(userId));

        // Only update fields that were actually sent (non-null)
        if (request.getName()      != null) profile.setName(request.getName());
        if (request.getPhone()     != null) profile.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) profile.setAvatarUrl(request.getAvatarUrl());
        if (request.getStreet()    != null) profile.setStreet(request.getStreet());
        if (request.getCity()      != null) profile.setCity(request.getCity());
        if (request.getState()     != null) profile.setState(request.getState());
        if (request.getPincode()   != null) profile.setPincode(request.getPincode());
        if (request.getCountry()   != null) profile.setCountry(request.getCountry());

        return toResponse(userProfileRepository.save(profile));
    }

    // API fallback — creates profile if it doesn't exist yet
    // Handles race condition: registration happened but Kafka event not consumed yet
    @Transactional
    public UserProfileResponse getOrCreateProfile(String userId, String email) {
        return userProfileRepository.findById(UUID.fromString(userId))
                .map(this::toResponse)
                .orElseGet(() -> {
                    log.info("Profile not found via Kafka, creating fallback for userId: {}", userId);
                    UserProfile profile = UserProfile.builder()
                            .id(UUID.fromString(userId))
                            .email(email)
                            .build();
                    return toResponse(userProfileRepository.save(profile));
                });
    }

    // Admin methods
    public Page<UserProfileResponse> getAllUsers(Pageable pageable) {
        return userProfileRepository.findAll(pageable).map(this::toResponse);
    }

    public UserProfileResponse getUserById(UUID userId) {
        return toResponse(findById(userId));
    }

    public UserProfileResponse getUserByEmail(String email) {
        UserProfile profile = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));
        return toResponse(profile);
    }

    private UserProfile findById(UUID userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User profile not found for id: " + userId));
    }

    private UserProfileResponse toResponse(UserProfile p) {
        return UserProfileResponse.builder()
                .id(p.getId())
                .email(p.getEmail())
                .name(p.getName())
                .phone(p.getPhone())
                .avatarUrl(p.getAvatarUrl())
                .street(p.getStreet())
                .city(p.getCity())
                .state(p.getState())
                .pincode(p.getPincode())
                .country(p.getCountry())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}