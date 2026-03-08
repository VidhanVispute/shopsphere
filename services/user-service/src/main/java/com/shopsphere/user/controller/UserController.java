package com.shopsphere.user.controller;

import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.user.dto.request.UpdateProfileRequest;
import com.shopsphere.user.dto.response.UserProfileResponse;
import com.shopsphere.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ── Self-service endpoints ──────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Email") String email) {

        // getOrCreateProfile handles the API fallback case
        UserProfileResponse profile = userService.getOrCreateProfile(userId, email);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", profile));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdateProfileRequest request) {

        UserProfileResponse profile = userService.updateMyProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", profile));
    }

    // ── Admin endpoints ─────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserProfileResponse>>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<UserProfileResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", users));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserById(
            @PathVariable UUID userId) {

        UserProfileResponse profile = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("User retrieved", profile));
    }

    @GetMapping("/by-email")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserByEmail(
            @RequestParam String email) {

        UserProfileResponse profile = userService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.success("User retrieved", profile));
    }

    @GetMapping("/admin-test")
public ResponseEntity<String> adminTest(
        @RequestHeader(value = "X-User-Role", required = false) String role,
        @RequestHeader(value = "X-User-Id", required = false) String userId) {
    return ResponseEntity.ok("role=" + role + " userId=" + userId);
}
}