package com.digital.lending.notification.event;

import com.digital.lending.events.ProfileRegisteredEvent;
import com.digital.lending.events.ProfileUpdatedEvent;
import com.digital.lending.notification.model.ProfileContactProjection;
import com.digital.lending.notification.repository.ProfileContactProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ProfileContactProjectionEventListener {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final ProfileContactProjectionRepository repository;

    @ApplicationModuleListener
    public void onProfileRegistered(ProfileRegisteredEvent event) {
        upsert(event.profileId(), event.displayName(), event.email(), event.phone(), STATUS_ACTIVE, event.occurredAt());
    }

    @ApplicationModuleListener
    public void onProfileUpdated(ProfileUpdatedEvent event) {
        upsert(event.profileId(), event.displayName(), event.email(), event.phone(), STATUS_ACTIVE, event.occurredAt());
    }

    private void upsert(String profileId, String displayName, String email, String phone, String status, Instant updatedAt) {
        if (profileId == null || profileId.isBlank()) {
            return;
        }

        ProfileContactProjection projection = repository.findById(profileId)
                .orElseGet(ProfileContactProjection::new);

        projection.setProfileId(profileId);
        projection.setDisplayName(displayName == null || displayName.isBlank() ? profileId : displayName);
        projection.setEmail(email == null ? "" : email);
        projection.setPhone(phone);
        projection.setStatus(status);
        projection.setUpdatedAt(updatedAt == null ? Instant.now() : updatedAt);

        repository.save(projection);
    }
}
