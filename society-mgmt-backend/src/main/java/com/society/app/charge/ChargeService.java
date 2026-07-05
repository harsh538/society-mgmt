package com.society.app.charge;

import com.society.app.charge.dto.ChargeConfigDto;
import com.society.app.charge.dto.GenerateChargesRequest;
import com.society.app.charge.dto.GenerateChargesResponse;
import com.society.app.charge.dto.MaintenanceChargeDto;
import com.society.app.charge.dto.UpdateChargeConfigRequest;
import com.society.app.charge.dto.VoidChargeRequest;
import com.society.app.unit.Unit;
import com.society.app.unit.UnitRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Charge configuration + monthly maintenance charge lifecycle (project.md §§ 5.4, 7.1–7.6).
 *
 * <p>Hard rules:
 * <ul>
 *   <li>All money is {@link BigDecimal} — never float / double.</li>
 *   <li>Charge generation is idempotent per (unit, year, month).</li>
 *   <li>Only active FLAT + SHOP units are billed. TERRACE excluded.</li>
 *   <li>Charges are never hard-deleted — voided via {@link #voidCharge(Long, VoidChargeRequest)}.</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ChargeService {

    private static final Long CONFIG_ID = 1L;
    private static final List<String> BILLABLE_TYPES = List.of("FLAT", "SHOP");

    private final ChargeConfigRepository chargeConfigRepository;
    private final MaintenanceChargeRepository chargeRepository;
    private final UnitRepository unitRepository;

    // -------------------------------------------------------------------------
    // Config (singleton)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ChargeConfigDto getConfig() {
        return ChargeConfigDto.from(loadConfig());
    }

    @Transactional
    public ChargeConfigDto updateConfig(UpdateChargeConfigRequest req) {
        ChargeConfig c = loadConfig();
        if (req.getDefaultFlatMaintenance() != null) c.setDefaultFlatMaintenance(req.getDefaultFlatMaintenance());
        if (req.getDefaultShopMaintenance() != null) c.setDefaultShopMaintenance(req.getDefaultShopMaintenance());
        if (req.getDefaultTenantSurcharge() != null) c.setDefaultTenantSurcharge(req.getDefaultTenantSurcharge());
        if (req.getDefaultDueDay() != null) c.setDefaultDueDay(req.getDefaultDueDay());
        if (req.getSocietyName() != null) c.setSocietyName(req.getSocietyName());
        if (req.getSocietyUpiId() != null) c.setSocietyUpiId(req.getSocietyUpiId());
        if (req.getSocietyBankDetails() != null) c.setSocietyBankDetails(req.getSocietyBankDetails());
        chargeConfigRepository.save(c);
        return ChargeConfigDto.from(c);
    }

    private ChargeConfig loadConfig() {
        return chargeConfigRepository.findById(CONFIG_ID)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Charge configuration not initialised (charge_config row id=1 missing)"));
    }

    // -------------------------------------------------------------------------
    // Charge generation
    // -------------------------------------------------------------------------

    /**
     * Idempotent monthly generation (project.md § 7.2).
     * <ol>
     *   <li>Loads all active FLAT + SHOP units (TERRACE excluded).</li>
     *   <li>Computes {@code amountDue = baseMaintenance + (occupancy='TENANT' ? tenantSurcharge : 0)}.</li>
     *   <li>Skips units that already have a charge for the period.</li>
     *   <li>Snapshots the amount onto the charge row; later config edits do not retro-edit.</li>
     * </ol>
     */
    @Transactional
    public GenerateChargesResponse generateCharges(GenerateChargesRequest req) {
        int year = req.getYear();
        int month = req.getMonth();
        ChargeConfig config = loadConfig();
        int dueDay = config.getDefaultDueDay();
        // Guard against months with fewer days than the configured due day
        // (e.g. dueDay=31, month=Feb). dueDay is constrained 1..28 in the schema,
        // so LocalDate.of(year, month, dueDay) is always valid here.
        LocalDate dueDate = LocalDate.of(year, month, dueDay);

        List<Unit> units = unitRepository.findByUnitTypeInAndIsActiveTrue(BILLABLE_TYPES);

        int created = 0;
        int skipped = 0;
        for (Unit u : units) {
            Optional<MaintenanceCharge> existing =
                    chargeRepository.findByUnitIdAndPeriodYearAndPeriodMonth(u.getId(), year, month);
            if (existing.isPresent()) {
                skipped++;
                continue;
            }

            BigDecimal base = u.getBaseMaintenance() != null ? u.getBaseMaintenance() : BigDecimal.ZERO;
            BigDecimal surcharge = "TENANT".equals(u.getOccupancy())
                    && u.getTenantSurcharge() != null
                    ? u.getTenantSurcharge()
                    : BigDecimal.ZERO;
            BigDecimal amountDue = base.add(surcharge);

            MaintenanceCharge c = new MaintenanceCharge();
            c.setUnit(u);
            c.setPeriodYear(year);
            c.setPeriodMonth(month);
            c.setAmountDue(amountDue);
            c.setAmountPaid(BigDecimal.ZERO);
            c.setStatus("DUE");
            c.setDueDate(dueDate);
            chargeRepository.save(c);
            created++;
        }

        return new GenerateChargesResponse(created, skipped, created + skipped);
    }

    // -------------------------------------------------------------------------
    // Reads
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<MaintenanceChargeDto> listCharges(
            Long unitId, Integer year, Integer month, String status, Pageable pageable) {

        boolean hasUnit = unitId != null;
        boolean hasPeriod = year != null && month != null;
        boolean hasStatus = status != null && !status.isBlank();

        Page<MaintenanceCharge> page;
        if (hasUnit && hasPeriod && hasStatus) {
            page = chargeRepository.findByUnitIdAndPeriodYearAndPeriodMonthAndStatus(
                    unitId, year, month, status, pageable);
        } else if (hasUnit && hasStatus) {
            page = chargeRepository.findByUnitIdAndStatus(unitId, status, pageable);
        } else if (hasPeriod && hasStatus) {
            page = chargeRepository.findByPeriodYearAndPeriodMonthAndStatus(year, month, status, pageable);
        } else if (hasUnit && hasPeriod) {
            // No status filter — show all (including VOID) when an explicit unit+period
            // is asked for, so admins can see history-with-voids in one place.
            page = chargeRepository.findByUnitId(unitId, pageable);
            // Filter in-memory by period.
            List<MaintenanceCharge> filtered = page.getContent().stream()
                    .filter(c -> c.getPeriodYear() == year && c.getPeriodMonth() == month)
                    .toList();
            return new org.springframework.data.domain.PageImpl<>(
                    filtered.stream().map(MaintenanceChargeDto::from).toList(),
                    pageable,
                    filtered.size()
            );
        } else if (hasUnit) {
            page = chargeRepository.findByUnitId(unitId, pageable);
        } else if (hasPeriod) {
            page = chargeRepository.findByPeriodYearAndPeriodMonth(year, month, pageable);
        } else if (hasStatus) {
            page = chargeRepository.findByStatus(status, pageable);
        } else {
            page = chargeRepository.findByStatusNot("VOID", pageable);
        }
        return page.map(MaintenanceChargeDto::from);
    }

    @Transactional(readOnly = true)
    public MaintenanceChargeDto getCharge(Long id) {
        MaintenanceCharge c = chargeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Charge not found: " + id));
        return MaintenanceChargeDto.from(c);
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    @Transactional
    public MaintenanceChargeDto voidCharge(Long id, VoidChargeRequest req) {
        MaintenanceCharge c = chargeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Charge not found: " + id));

        if ("VOID".equals(c.getStatus())) {
            throw new IllegalStateException("Charge is already voided");
        }
        if ("PAID".equals(c.getStatus())) {
            throw new IllegalStateException("Cannot void a fully paid charge");
        }

        // Reason is captured on the request; we don't persist it as a column in
        // Phase 4 (audit log lands in a later phase). Validation guarantees the
        // admin justified the action.
        c.setStatus("VOID");
        chargeRepository.save(c);
        return MaintenanceChargeDto.from(c);
    }

    // -------------------------------------------------------------------------
    // Unit-scoped reads (replaces Phase 3 stubs)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MaintenanceChargeDto> getUnitCharges(Long unitId) {
        // Order by year desc, month desc — newest period first.
        List<MaintenanceCharge> rows = chargeRepository
                .findByUnitIdAndStatusNotOrderByPeriodYearDescPeriodMonthDesc(unitId, "VOID");
        return rows.stream().map(MaintenanceChargeDto::from).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUnitOutstanding(Long unitId) {
        List<MaintenanceCharge> rows = chargeRepository.findByUnitIdAndStatusNot(unitId, "VOID");
        BigDecimal sum = BigDecimal.ZERO;
        for (MaintenanceCharge c : rows) {
            BigDecimal due = c.getAmountDue() == null ? BigDecimal.ZERO : c.getAmountDue();
            BigDecimal paid = c.getAmountPaid() == null ? BigDecimal.ZERO : c.getAmountPaid();
            sum = sum.add(due.subtract(paid));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("unitId", unitId);
        out.put("outstanding", sum.setScale(2).toPlainString());
        return out;
    }
}
