package com.society.app.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Per-unit outstanding amount returned inside {@link OutstandingSummaryDto}
 * (project.md § 5.9 / {@code GET /dashboard/outstanding}).
 *
 * <p>{@code outstanding = SUM(amount_due − amount_paid)} over the unit's non-VOID
 * charges (project.md § 3.10).</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutstandingRowDto {

    private Long unitId;
    private String unitNumber;
    private String unitType;
    private String ownerName;
    private BigDecimal outstanding;
}
