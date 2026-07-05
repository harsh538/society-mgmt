package com.society.app.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Society-wide outstanding response
 * (project.md § 5.9 / {@code GET /dashboard/outstanding}).
 *
 * <p>{@code societyTotal} is the headline total shown in a large stat card;
 * {@code units} is the per-unit drill-down (only units with outstanding &gt; 0).</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutstandingSummaryDto {

    private BigDecimal societyTotal;
    private List<OutstandingRowDto> units;
}
