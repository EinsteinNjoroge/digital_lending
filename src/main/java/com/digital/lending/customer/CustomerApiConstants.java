package com.digital.lending.customer;

public final class CustomerApiConstants {
    private CustomerApiConstants() {}

    // Routing & Grouping
    public static final String GROUP_NAME = "Customer-Profile-Management-Module";
    public static final String TAG_NAME = "Customer Management";
    public static final String TAG_DESCRIPTION = "Endpoints for onboarding and managing Individual, Corporate, and Joint customer profiles.";
    public static final String BASE_PATH = "/api/v1/customers";
    public static final String PATH_VARIABLE_ID = "/{id}";

    // Operation Summaries
    public static final String SUMMARY_CUSTOMER_CREATION = "Create a new customer profile";
    public static final String DESCRIPTION_CUSTOMER_CREATION = "The schema alters requirements based on the 'customerType' parameter.";
    public static final String SUMMARY_GET_CUSTOMER_BY_ID = "Find a customer by their unique identifier";
    public static final String SUMMARY_GET_ALL_CUSTOMERS = "List all registered system profiles";
    public static final String SUMMARY_UPDATE_CUSTOMER_CONTACT = "Update customer contact attributes";
    public static final String SUMMARY_SOFT_DELETE_CUSTOMER = "Soft delete or close a customer profile record";

    // HTTP Responses
    public static final String RESPONSE_201_CREATED_DESCRIPTION = "Customer registered successfully.";
    public static final String RESPONSE_200_UPDATED_DESCRIPTION = "Profile contact parameters updated successfully.";
    public static final String RESPONSE_200_FOUND_DESCRIPTION = "Customer entity found matching the provided ID.";
    public static final String RESPONSE_204_DELETED_DESCRIPTION = "Customer profile status successfully changed to CLOSED.";
    public static final String RESPONSE_400_BAD_REQUEST_DESCRIPTION = "Invalid request payload attributes or structural validation failed.";
    public static final String RESPONSE_404_NOT_FOUND_DESCRIPTION = "No customer reference found matching the provided identifier.";
    public static final String RESPONSE_409_CONFLICT_DESCRIPTION = "An identity document or email address conflict occurred.";
    public static final String RESPONSE_422_UNPROCESSABLE_DESCRIPTION = "Business rule or compliance verification failed.";
}