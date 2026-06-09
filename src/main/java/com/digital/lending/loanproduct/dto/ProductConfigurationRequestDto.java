package com.digital.lending.loanproduct.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
@Schema(name = "ProductConfigurationRequest", description = "Input payload required to provision or overwrite versioned loan product structures.")
public class ProductConfigurationRequestDto {

    @NotBlank(message = "Product code is required")
    @Size(max = 32)
    @Schema(description = "Unique alphanumeric token tracking a family class of configurations", example = "LN_MZ_MZN_CASH_NANO")
    private String productCode;

    @NotBlank(message = "Product name is required")
    @Size(max = 100)
    @Schema(description = "Descriptive display name representing the system allocation profile", example = "Mobile Money Quick Emergency Cash Boost")
    private String name;

    @NotBlank(message = "Family definition ID linkage is required")
    @Schema(description = "System execution handler blueprint template identification key", example = "fdef_002")
    private String familyDefinitionId;

    @NotBlank(message = "Partner ID route identifier is required")
    @Schema(description = "Multi-tenant channel origin node path parameter tracking identity", example = "VODA_MZ_02")
    private String partnerId;

    @NotBlank(message = "ISO financial currency token is required")
    @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
    @Schema(description = "ISO-4217 country transactional tracking token code mapping boundaries", example = "MZN")
    private String currency;

    @NotNull(message = "Activation flag state is required")
    @Schema(description = "Toggles runtime routing engine processing accessibility limits visibility options", example = "true")
    private Boolean isActive;

    @Schema(description = "Dynamic parameter options mappings covering processing steps interest ratios", example = "{\"interest_rate_daily\": \"0.0015\", \"grace_period_days\": \"3\"}")
    private Map<String, String> parameters;

    @Schema(description = "Polymorphic checklist logic mappings unmarshalled into database properties models", example = "{\"UNDERWRITING_CHECKLIST\": {\"minimum_wallet_age_months\": 6, \"require_identity_verification\": true}}")
    private Map<String, Map<String, Object>> documentMatrices;
}
