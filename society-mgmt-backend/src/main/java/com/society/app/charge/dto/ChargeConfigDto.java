package com.society.app.charge.dto;

import com.society.app.charge.ChargeConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChargeConfigDto {

    private Long id;
    private BigDecimal defaultFlatMaintenance;
    private BigDecimal defaultShopMaintenance;
    private BigDecimal defaultTenantSurcharge;
    private int defaultDueDay;
    private String societyName;
    private String societyUpiId;
    private String societyBankDetails;

    public static ChargeConfigDto from(ChargeConfig c) {
        return new ChargeConfigDto(
                c.getId(),
                c.getDefaultFlatMaintenance(),
                c.getDefaultShopMaintenance(),
                c.getDefaultTenantSurcharge(),
                c.getDefaultDueDay(),
                c.getSocietyName(),
                c.getSocietyUpiId(),
                c.getSocietyBankDetails()
        );
    }
}
