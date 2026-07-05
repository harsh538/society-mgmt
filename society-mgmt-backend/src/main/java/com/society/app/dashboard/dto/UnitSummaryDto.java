package com.society.app.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unit-count summary tiles for the admin dashboard
 * (project.md § 5.9 / {@code GET /dashboard/summary}).
 *
 * <p>All counts are over <strong>active</strong> units only — soft-deleted units
 * (is_active = false) are excluded so the dashboard reflects the live society state.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnitSummaryDto {

    private long flatCount;
    private long shopCount;
    private long terraceCount;
    private long totalUnits;
    private long ownerOccupied;
    private long tenantOccupied;
    private long vacant;
}
