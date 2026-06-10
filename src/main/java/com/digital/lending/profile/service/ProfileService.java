package com.digital.lending.profile.service;

import com.digital.lending.profile.dto.CreateProfileRequest;
import com.digital.lending.profile.dto.ProfileDto;
import com.digital.lending.profile.dto.UpdateProfileRequest;

import java.util.List;
import java.util.Optional;

public interface ProfileService {
    ProfileDto createProfile(CreateProfileRequest request);
    Optional<ProfileDto> findProfileById(String id);
    List<ProfileDto> findAllProfiles();
    ProfileDto updateProfile(String id, UpdateProfileRequest request);
    void deleteProfile(String id);
    boolean isProfileActive(String id);
}
