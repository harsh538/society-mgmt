package com.society.app.charge.dto;

import com.society.app.charge.MaintenanceCharge;
import com.society.app.unit.Unit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceChargeDto {

    private Long id;
    private Long unitId;
    private String unitNumber;
    private String unitType;
    private int periodYear;
    private int periodMonth;
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    /** Always derived as amountDue − amountPaid (project.md § 1.2 rule 2). */
    private BigDecimal outstanding;
    private String status;
    private LocalDate dueDate;

    public static MaintenanceChargeDto from(MaintenanceCharge c) {
        Unit u = c.getUnit();
        BigDecimal due = c.getAmountDue() == null ? BigDecimal.ZERO : c.getAmountDue();
        BigDecimal paid = c.getAmountPaid() == null ? BigDecimal.ZERO : c.getAmountPaid();
        return new MaintenanceChargeDto(
                c.getId(),
                u != null ? u.getId() : null,
                u != null ? u.getUnitNumber() : null,
                u != null ? u.getUnitType() : null,
                c.getPeriodYear(),
                c.getPeriodMonth(),
                due,
                paid,
                due.subtract(paid),
                c.getStatus(),
                c.getDueDate()
        );
    }
}
