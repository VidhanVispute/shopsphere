package com.shopsphere.user.kafka;

import com.shopsphere.common.events.UserRegisteredEvent;
import com.shopsphere.user.entity.UserProfile;
import com.shopsphere.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventConsumer {

    private final UserProfileRepository userProfileRepository;

    @KafkaListener(topics = "user-registered", groupId = "user-service")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received user-registered event for userId: {}", event.getUserId());

        // Idempotent — if profile already exists, skip silently
        if (userProfileRepository.existsById(event.getUserId())) {
            log.info("Profile already exists for userId: {}, skipping", event.getUserId());
            return;
        }

        UserProfile profile = UserProfile.builder()
                .id(event.getUserId())
                .email(event.getEmail())
                .name(event.getName())
                .build();

        userProfileRepository.save(profile);
        log.info("Created profile for userId: {}", event.getUserId());
    }
}