package com.digital.lending.profile.util;

public final class ProfileApiConstants {

    private ProfileApiConstants() {
    }

    public static final String TAG_NAME = "Profile Management";
    public static final String TAG_DESCRIPTION = "Endpoints for onboarding and managing Individual, Corporate, and Joint profiles.";
    public static final String BASE_PATH = "/api/v1/profiles";
    public static final String PATH_VARIABLE_ID = "/{id}";

    public static final String SUMMARY_PROFILE_CREATION = "Create a new profile";
    public static final String DESCRIPTION_PROFILE_CREATION = "The schema alters requirements based on the 'profileType' parameter.";
    public static final String SUMMARY_GET_PROFILE_BY_ID = "Find a profile by its unique identifier";
    public static final String SUMMARY_GET_ALL_PROFILES = "List all registered system profiles";
    public static final String SUMMARY_UPDATE_PROFILE_CONTACT = "Update profile contact attributes";
    public static final String SUMMARY_SOFT_DELETE_PROFILE = "Soft delete or close a profile record";

    public static final String RESPONSE_201_CREATED_DESCRIPTION = "Profile registered successfully.";
    public static final String RESPONSE_200_UPDATED_DESCRIPTION = "Profile contact parameters updated successfully.";
    public static final String RESPONSE_200_FOUND_DESCRIPTION = "Profile entity found matching the provided ID.";
    public static final String RESPONSE_204_DELETED_DESCRIPTION = "Profile status successfully changed to CLOSED.";
    public static final String RESPONSE_400_BAD_REQUEST_DESCRIPTION = "Invalid request payload attributes or structural validation failed.";
    public static final String RESPONSE_404_NOT_FOUND_DESCRIPTION = "No profile reference found matching the provided identifier.";
    public static final String RESPONSE_409_CONFLICT_DESCRIPTION = "An identity document or email address conflict occurred.";
    public static final String RESPONSE_422_UNPROCESSABLE_DESCRIPTION = "Business rule or compliance verification failed.";
}
