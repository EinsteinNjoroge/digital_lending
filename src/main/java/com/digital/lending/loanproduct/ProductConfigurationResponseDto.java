package com.digital.lending.loanproduct;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Schema(name = "ProductConfigurationResponse", description = "Complete definition summary output outlining structural parameters and system lifecycle metrics.")
public class ProductConfigurationResponseDto {
    @Schema(example = "f186e626-c8b0-4e0c-bc1a-592b68f275ca")
    private String id;
    @Schema(example = "1")
    private Integer version;
    @Schema(example = "LN_MZ_MZN_CASH_NANO")
    private String productCode;
    @Schema(example = "Mobile Money Quick Emergency Cash Boost")
    private String name;
    @Schema(example = "fdef_002")
    private String familyDefinitionId;
    @Schema(example = "VODA_MZ_02")
    private String partnerId;
    @Schema(example = "MZN")
    private String currency;
    @Schema(example = "true")
    private Boolean isActive;
    private Map<String, String> parameters;
    private Map<String, Object> documentMatrices;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}