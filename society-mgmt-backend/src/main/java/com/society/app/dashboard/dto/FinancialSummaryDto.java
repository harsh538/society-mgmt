package com.society.app.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Society-wide money totals for the admin dashboard
 * (project.md § 5.9 / {@code GET /dashboard/financials}).
 *
 * <p>Computation rules (all over non-VOID charges):
 * <ul>
 *   <li>{@code totalBilled = SUM(amount_due)}</li>
 *   <li>{@code totalCollected = SUM(amount_paid)}</li>
 *   <li>{@code totalOutstanding = totalBilled − totalCollected}</li>
 *   <li>{@code totalExpenses = SUM(society_expenses.amount)} — wired in Phase 7</li>
 *   <li>{@code netPosition = totalCollected − totalExpenses}
 *       (project.md § 7.10: net position = VERIFIED income − expenses)</li>
 *   <li>{@code pendingPaymentsCount} = count of payments with status=PENDING</li>
 * </ul>
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialSummaryDto {

    private BigDecimal totalBilled;
    private BigDecimal totalCollected;
    private BigDecimal totalOutstanding;
    private BigDecimal totalExpenses;
    private BigDecimal netPosition;
    private long pendingPaymentsCount;
}
