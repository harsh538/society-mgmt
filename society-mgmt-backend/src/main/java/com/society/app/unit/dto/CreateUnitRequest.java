package com.society.app.unit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUnitRequest {

    @NotBlank
    @Pattern(regexp = "FLAT|SHOP|TERRACE", message = "unitType must be FLAT, SHOP, or TERRACE")
    private String unitType;

    @NotBlank
    private String unitNumber;

    private String floor;

    private Long ownerMemberId;

    @NotBlank
    @Pattern(regexp = "OWNER|TENANT|VACANT", message = "occupancy must be OWNER, TENANT, or VACANT")
    private String occupancy;

    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal baseMaintenance;

    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal tenantSurcharge;
}
