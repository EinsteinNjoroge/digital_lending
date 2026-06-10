package com.digital.lending.profile.controller;

import com.digital.lending.profile.dto.CreateProfileRequest;
import com.digital.lending.profile.dto.ProfileDto;
import com.digital.lending.profile.dto.UpdateProfileRequest;
import com.digital.lending.profile.exception.ApiErrorResponse;
import com.digital.lending.profile.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;

import static com.digital.lending.profile.util.ProfileApiConstants.BASE_PATH;
import static com.digital.lending.profile.util.ProfileApiConstants.DESCRIPTION_PROFILE_CREATION;
import static com.digital.lending.profile.util.ProfileApiConstants.PATH_VARIABLE_ID;
import static com.digital.lending.profile.util.ProfileApiConstants.RESPONSE_200_FOUND_DESCRIPTION;
import static com.digital.lending.profile.util.ProfileApiConstants.RESPONSE_200_UPDATED_DESCRIPTION;
import static com.digital.lending.profile.util.ProfileApiConstants.RESPONSE_201_CREATED_DESCRIPTION;
import static com.digital.lending.profile.util.ProfileApiConstants.RESPONSE_204_DELETED_DESCRIPTION;
import static com.digital.lending.profile.util.ProfileApiConstants.RESPONSE_400_BAD_REQUEST_DESCRIPTION;
import static com.digital.lending.profile.util.ProfileApiConstants.RESPONSE_404_NOT_FOUND_DESCRIPTION;
import static com.digital.lending.profile.util.ProfileApiConstants.RESPONSE_409_CONFLICT_DESCRIPTION;
import static com.digital.lending.profile.util.ProfileApiConstants.RESPONSE_422_UNPROCESSABLE_DESCRIPTION;
import static com.digital.lending.profile.util.ProfileApiConstants.SUMMARY_GET_ALL_PROFILES;
import static com.digital.lending.profile.util.ProfileApiConstants.SUMMARY_GET_PROFILE_BY_ID;
import static com.digital.lending.profile.util.ProfileApiConstants.SUMMARY_PROFILE_CREATION;
import static com.digital.lending.profile.util.ProfileApiConstants.SUMMARY_SOFT_DELETE_PROFILE;
import static com.digital.lending.profile.util.ProfileApiConstants.SUMMARY_UPDATE_PROFILE_CONTACT;
import static com.digital.lending.profile.util.ProfileApiConstants.TAG_DESCRIPTION;
import static com.digital.lending.profile.util.ProfileApiConstants.TAG_NAME;

@RestController
@RequestMapping(BASE_PATH)
@Tag(name = TAG_NAME, description = TAG_DESCRIPTION)
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping
    @Operation(summary = SUMMARY_PROFILE_CREATION, description = DESCRIPTION_PROFILE_CREATION)
    @ApiResponse(responseCode = "201", description = RESPONSE_201_CREATED_DESCRIPTION, content = @Content(schema = @Schema(implementation = ProfileDto.class)))
    @ApiResponse(responseCode = "400", description = RESPONSE_400_BAD_REQUEST_DESCRIPTION, content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = RESPONSE_409_CONFLICT_DESCRIPTION, content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = RESPONSE_422_UNPROCESSABLE_DESCRIPTION, content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ProfileDto> createProfile(@Valid @RequestBody CreateProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.createProfile(request));
    }

    @GetMapping(PATH_VARIABLE_ID)
    @Operation(summary = SUMMARY_GET_PROFILE_BY_ID)
    @ApiResponse(responseCode = "200", description = RESPONSE_200_FOUND_DESCRIPTION)
    @ApiResponse(responseCode = "404", description = RESPONSE_404_NOT_FOUND_DESCRIPTION)
    public ResponseEntity<ProfileDto> getProfileById(@PathVariable String id) {
        return profileService.findProfileById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = SUMMARY_GET_ALL_PROFILES)
    public ResponseEntity<List<ProfileDto>> getAllProfiles() {
        return ResponseEntity.ok(profileService.findAllProfiles());
    }

    @PutMapping(PATH_VARIABLE_ID)
    @Operation(summary = SUMMARY_UPDATE_PROFILE_CONTACT)
    @ApiResponse(responseCode = "200", description = RESPONSE_200_UPDATED_DESCRIPTION)
    public ResponseEntity<ProfileDto> updateProfile(@PathVariable String id, @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(id, request));
    }

    @DeleteMapping(PATH_VARIABLE_ID)
    @Operation(summary = SUMMARY_SOFT_DELETE_PROFILE)
    @ApiResponse(responseCode = "204", description = RESPONSE_204_DELETED_DESCRIPTION)
    public ResponseEntity<Void> deleteProfile(@PathVariable String id) {
        profileService.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }
}
