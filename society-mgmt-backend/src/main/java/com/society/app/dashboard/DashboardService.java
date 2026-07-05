package com.society.app.dashboard;

import com.society.app.charge.MaintenanceCharge;
import com.society.app.charge.MaintenanceChargeRepository;
import com.society.app.dashboard.dto.FinancialSummaryDto;
import com.society.app.dashboard.dto.MaintenanceRowDto;
import com.society.app.dashboard.dto.OutstandingRowDto;
import com.society.app.dashboard.dto.OutstandingSummaryDto;
import com.society.app.dashboard.dto.UnitSummaryDto;
import com.society.app.expense.ExpenseRepository;
import com.society.app.payment.PaymentRepository;
import com.society.app.unit.Unit;
import com.society.app.unit.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Aggregations powering the admin dashboard (project.md § 5.9, § 3.10, § 7.6, § 7.10).
 *
 * <p>Hard rules applied here:
 * <ul>
 *   <li>Money is {@link BigDecimal} — every sum uses {@code BigDecimal.add/subtract}.</li>
 *   <li>Outstanding is always derived as {@code SUM(amount_due - amount_paid)} over
 *       non-VOID charges (project.md § 1.2 rule 2). Never stored.</li>
 *   <li>Only active units are counted (soft-deleted units excluded).</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final UnitRepository unitRepository;
    private final MaintenanceChargeRepository chargeRepository;
    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;

    // -------------------------------------------------------------------------
    // GET /dashboard/summary
    // -------------------------------------------------------------------------

    public UnitSummaryDto getSummary() {
        long flatCount = unitRepository.countByUnitTypeAndIsActiveTrue("FLAT");
        long shopCount = unitRepository.countByUnitTypeAndIsActiveTrue("SHOP");
        long terraceCount = unitRepository.countByUnitTypeAndIsActiveTrue("TERRACE");
        long totalUnits = unitRepository.countByIsActiveTrue();
        long ownerOccupied = unitRepository.countByOccupancyAndIsActiveTrue("OWNER");
        long tenantOccupied = unitRepository.countByOccupancyAndIsActiveTrue("TENANT");
        long vacant = unitRepository.countByOccupancyAndIsActiveTrue("VACANT");
        return new UnitSummaryDto(
                flatCount, shopCount, terraceCount, totalUnits,
                ownerOccupied, tenantOccupied, vacant);
    }

    // -------------------------------------------------------------------------
    // GET /dashboard/maintenance
    // -------------------------------------------------------------------------

    /**
     * Per-unit maintenance status (project.md § 5.9 + § 7.6).
     *
     * @param typeFilter   optional unit_type (FLAT/SHOP/TERRACE) — pass {@code null} for all
     * @param statusFilter optional overall status (DUE/PARTIAL/PAID/NO_CHARGES) — {@code null} for all
     */
    public List<MaintenanceRowDto> getMaintenanceStatus(String typeFilter, String statusFilter) {
        List<Unit> units = unitRepository.findByIsActiveTrue();
        List<MaintenanceRowDto> rows = new ArrayList<>(units.size());

        for (Unit u : units) {
            // All non-VOID charges for this unit (project.md § 1.2 rule 2).
            List<MaintenanceCharge> charges =
                    chargeRepository.findByUnitIdAndStatusNot(u.getId(), "VOID");

            BigDecimal totalDue = BigDecimal.ZERO;
            BigDecimal totalPaid = BigDecimal.ZERO;
            for (MaintenanceCharge c : charges) {
                BigDecimal due = c.getAmountDue() == null ? BigDecimal.ZERO : c.getAmountDue();
                BigDecimal paid = c.getAmountPaid() == null ? BigDecimal.ZERO : c.getAmountPaid();
                totalDue = totalDue.add(due);
                totalPaid = totalPaid.add(paid);
            }
            BigDecimal outstanding = totalDue.subtract(totalPaid);
            String overallStatus = computeOverallStatus(charges.isEmpty(), totalPaid, outstanding);

            String ownerName = u.getOwner() != null ? u.getOwner().getFullName() : null;

            rows.add(new MaintenanceRowDto(
                    u.getId(),
                    u.getUnitNumber(),
                    u.getUnitType(),
                    u.getOccupancy(),
                    ownerName,
                    totalDue,
                    totalPaid,
                    outstanding,
                    overallStatus
            ));
        }

        // Apply filters (case-sensitive; frontend sends canonical uppercase values).
        if (typeFilter != null && !typeFilter.isBlank()) {
            rows.removeIf(r -> !typeFilter.equals(r.getUnitType()));
        }
        if (statusFilter != null && !statusFilter.isBlank()) {
            rows.removeIf(r -> !statusFilter.equals(r.getOverallStatus()));
        }

        // Stable sort by unit number for predictable rendering.
        rows.sort(Comparator.comparing(
                MaintenanceRowDto::getUnitNumber,
                Comparator.nullsLast(String::compareToIgnoreCase)));

        return rows;
    }

    private String computeOverallStatus(boolean noCharges, BigDecimal paid, BigDecimal outstanding) {
        if (noCharges) return "NO_CHARGES";
        if (outstanding.signum() <= 0) return "PAID";
        if (paid.signum() > 0) return "PARTIAL";
        return "DUE";
    }

    // -------------------------------------------------------------------------
    // GET /dashboard/financials
    // -------------------------------------------------------------------------

    public FinancialSummaryDto getFinancials() {
        BigDecimal totalBilled = nz(chargeRepository.sumAmountDue());
        BigDecimal totalCollected = nz(chargeRepository.sumAmountPaid());
        BigDecimal totalOutstanding = totalBilled.subtract(totalCollected);

        BigDecimal totalExpenses = nz(expenseRepository.sumAllExpenses());

        // project.md § 7.10: net position = total VERIFIED income − total expenses.
        // totalCollected is already the sum of charge.amount_paid which only moves
        // inside the payment-verification transaction, so it equals verified income.
        BigDecimal netPosition = totalCollected.subtract(totalExpenses);

        long pendingPayments = paymentRepository.countByStatus("PENDING");

        return new FinancialSummaryDto(
                totalBilled,
                totalCollected,
                totalOutstanding,
                totalExpenses,
                netPosition,
                pendingPayments
        );
    }

    // -------------------------------------------------------------------------
    // GET /dashboard/outstanding
    // -------------------------------------------------------------------------

    public OutstandingSummaryDto getOutstanding() {
        // Re-use the per-unit aggregation; outstanding rows are a projection of the
        // maintenance rows. Filter to outstanding > 0 so the list stays focused on
        // units that actually owe money (project.md § 5.9).
        List<MaintenanceRowDto> rows = getMaintenanceStatus(null, null);

        BigDecimal societyTotal = BigDecimal.ZERO;
        List<OutstandingRowDto> outstandingRows = new ArrayList<>();
        for (MaintenanceRowDto r : rows) {
            BigDecimal o = r.getOutstanding() == null ? BigDecimal.ZERO : r.getOutstanding();
            societyTotal = societyTotal.add(o);
            if (o.signum() > 0) {
                outstandingRows.add(new OutstandingRowDto(
                        r.getUnitId(),
                        r.getUnitNumber(),
                        r.getUnitType(),
                        r.getOwnerName(),
                        o
                ));
            }
        }

        return new OutstandingSummaryDto(societyTotal, outstandingRows);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
