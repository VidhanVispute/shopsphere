package com.shopsphere.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    // ID is NOT auto-generated — it comes from Auth Service via Kafka event
    // Same UUID as the users table in auth_db
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    private String name;
    private String phone;
    private String avatarUrl;

    // Address — structured columns, not a single text blob
    private String street;
    private String city;
    private String state;
    private String pincode;
    private String country;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
}