package com.society.app.charge.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Partial-update request for {@link com.society.app.charge.ChargeConfig}.
 * All fields nullable — only non-null values are applied.
 */
@Getter
@Setter
public class UpdateChargeConfigRequest {

    @DecimalMin(value = "0.00", message = "defaultFlatMaintenance must be >= 0")
    private BigDecimal defaultFlatMaintenance;

    @DecimalMin(value = "0.00", message = "defaultShopMaintenance must be >= 0")
    private BigDecimal defaultShopMaintenance;

    @DecimalMin(value = "0.00", message = "defaultTenantSurcharge must be >= 0")
    private BigDecimal defaultTenantSurcharge;

    @Min(value = 1, message = "defaultDueDay must be between 1 and 28")
    @Max(value = 28, message = "defaultDueDay must be between 1 and 28")
    private Integer defaultDueDay;

    @Size(max = 150, message = "societyName must be <= 150 chars")
    private String societyName;

    @Size(max = 100, message = "societyUpiId must be <= 100 chars")
    private String societyUpiId;

    @Size(max = 500, message = "societyBankDetails must be <= 500 chars")
    private String societyBankDetails;
}
