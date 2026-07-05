package com.society.app.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One row per unit on the maintenance-status admin page
 * (project.md § 5.9 / {@code GET /dashboard/maintenance}).
 *
 * <p>Aggregates over the unit's non-VOID maintenance charges to produce
 * {@code amountDue}, {@code amountPaid}, and the derived {@code outstanding}.
 * Money is {@link BigDecimal} per the global "no float for money" rule
 * (project.md § 1.2 rule 1).</p>
 *
 * <p>{@code overallStatus} values:
 * <ul>
 *   <li>{@code NO_CHARGES} — no non-VOID charges exist for the unit yet</li>
 *   <li>{@code PAID} — outstanding ≤ 0 and at least one charge exists</li>
 *   <li>{@code PARTIAL} — some amount paid but outstanding > 0</li>
 *   <li>{@code DUE} — nothing paid and outstanding > 0</li>
 * </ul>
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRowDto {

    private Long unitId;
    private String unitNumber;
    private String unitType;
    private String occupancy;
    /** Owner full name, or {@code null} if the unit has no linked owner. */
    private String ownerName;
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private BigDecimal outstanding;
    private String overallStatus;
}
