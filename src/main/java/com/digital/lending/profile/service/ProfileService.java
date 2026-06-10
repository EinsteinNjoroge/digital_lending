package com.digital.lending.profile.service;

import com.digital.lending.profile.dto.CreateProfileRequestDto;
import com.digital.lending.profile.dto.ProfileDto;
import com.digital.lending.profile.dto.UpdateProfileRequestDto;

import java.util.List;
import java.util.Optional;

public interface ProfileService {
    ProfileDto createProfile(CreateProfileRequestDto request);
    Optional<ProfileDto> findProfileById(String id);
    List<ProfileDto> findAllProfiles();
    ProfileDto updateProfile(String id, UpdateProfileRequestDto request);
    void deleteProfile(String id);
    boolean isProfileActive(String id);
}
