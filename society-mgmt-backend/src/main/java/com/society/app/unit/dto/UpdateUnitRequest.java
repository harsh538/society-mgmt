package com.society.app.unit.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Partial-update payload. Only non-null fields are applied to the entity.
 * No @NotBlank/@NotNull because PATCH-style semantics — clients send only what changes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUnitRequest {

    private String unitType;
    private String unitNumber;
    private String floor;
    private Long ownerMemberId;
    private String occupancy;

    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal baseMaintenance;

    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal tenantSurcharge;
}
