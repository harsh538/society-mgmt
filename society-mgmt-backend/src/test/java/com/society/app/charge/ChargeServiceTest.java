package com.society.app.charge;

import com.society.app.charge.dto.GenerateChargesRequest;
import com.society.app.charge.dto.GenerateChargesResponse;
import com.society.app.unit.Unit;
import com.society.app.unit.UnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for charge generation business rules (project.md § 7.1–7.3).
 */
@ExtendWith(MockitoExtension.class)
class ChargeServiceTest {

    @Mock ChargeConfigRepository chargeConfigRepository;
    @Mock MaintenanceChargeRepository chargeRepository;
    @Mock UnitRepository unitRepository;

    @InjectMocks ChargeService chargeService;

    private ChargeConfig config;

    @BeforeEach
    void setUp() {
        config = new ChargeConfig();
        config.setDefaultDueDay(10);
        config.setDefaultFlatMaintenance(BigDecimal.valueOf(1500));
        config.setDefaultShopMaintenance(BigDecimal.valueOf(2000));
        config.setDefaultTenantSurcharge(BigDecimal.valueOf(100));
        config.setSocietyName("Test Society");
        when(chargeConfigRepository.findById(1L)).thenReturn(Optional.of(config));
    }

    @Test
    void generateCharges_createsCharge_forFlatOwner() {
        Unit flat = flatUnit("A-101", "OWNER", new BigDecimal("1500"), new BigDecimal("100"));
        flat.setId(1L);
        when(unitRepository.findByUnitTypeInAndIsActiveTrue(anyList())).thenReturn(List.of(flat));
        when(chargeRepository.findByUnitIdAndPeriodYearAndPeriodMonth(1L, 2026, 6))
                .thenReturn(Optional.empty());

        ArgumentCaptor<MaintenanceCharge> saved = ArgumentCaptor.forClass(MaintenanceCharge.class);
        when(chargeRepository.save(saved.capture())).thenAnswer(inv -> inv.getArgument(0));

        GenerateChargesRequest req = new GenerateChargesRequest();
        req.setYear(2026);
        req.setMonth(6);
        GenerateChargesResponse res = chargeService.generateCharges(req);

        assertThat(res.getCreated()).isEqualTo(1);
        assertThat(res.getSkipped()).isEqualTo(0);

        MaintenanceCharge charge = saved.getValue();
        assertThat(charge.getAmountDue()).isEqualByComparingTo(new BigDecimal("1500"));
        assertThat(charge.getStatus()).isEqualTo("DUE");
    }

    @Test
    void generateCharges_appliesTenantSurcharge() {
        Unit flat = flatUnit("B-202", "TENANT", new BigDecimal("1500"), new BigDecimal("100"));
        flat.setId(2L);
        when(unitRepository.findByUnitTypeInAndIsActiveTrue(anyList())).thenReturn(List.of(flat));
        when(chargeRepository.findByUnitIdAndPeriodYearAndPeriodMonth(2L, 2026, 6))
                .thenReturn(Optional.empty());
        when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GenerateChargesRequest req = new GenerateChargesRequest();
        req.setYear(2026);
        req.setMonth(6);

        ArgumentCaptor<MaintenanceCharge> saved = ArgumentCaptor.forClass(MaintenanceCharge.class);
        when(chargeRepository.save(saved.capture())).thenAnswer(inv -> inv.getArgument(0));
        chargeService.generateCharges(req);

        assertThat(saved.getValue().getAmountDue())
                .isEqualByComparingTo(new BigDecimal("1600")); // 1500 + 100
    }

    @Test
    void generateCharges_isIdempotent_skipsExistingPeriod() {
        Unit flat = flatUnit("C-303", "OWNER", new BigDecimal("1500"), BigDecimal.ZERO);
        flat.setId(3L);
        when(unitRepository.findByUnitTypeInAndIsActiveTrue(anyList())).thenReturn(List.of(flat));
        when(chargeRepository.findByUnitIdAndPeriodYearAndPeriodMonth(3L, 2026, 6))
                .thenReturn(Optional.of(new MaintenanceCharge())); // already exists

        GenerateChargesRequest req = new GenerateChargesRequest();
        req.setYear(2026);
        req.setMonth(6);
        GenerateChargesResponse res = chargeService.generateCharges(req);

        assertThat(res.getCreated()).isEqualTo(0);
        assertThat(res.getSkipped()).isEqualTo(1);
        verify(chargeRepository, never()).save(any());
    }

    @Test
    void generateCharges_excludesTerrace() {
        // TERRACE units should never appear in findByUnitTypeInAndIsActiveTrue because
        // the service passes BILLABLE_TYPES = ["FLAT","SHOP"]. Verify the filter list.
        when(unitRepository.findByUnitTypeInAndIsActiveTrue(anyList())).thenReturn(List.of());

        GenerateChargesRequest req = new GenerateChargesRequest();
        req.setYear(2026);
        req.setMonth(1);
        GenerateChargesResponse res = chargeService.generateCharges(req);

        assertThat(res.getCreated()).isEqualTo(0);
        // Verify that the repository was asked only for FLAT+SHOP (not TERRACE).
        ArgumentCaptor<List<String>> typesCaptor = ArgumentCaptor.forClass(List.class);
        verify(unitRepository).findByUnitTypeInAndIsActiveTrue(typesCaptor.capture());
        assertThat(typesCaptor.getValue()).containsExactlyInAnyOrder("FLAT", "SHOP");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Unit flatUnit(String number, String occupancy,
                                  BigDecimal base, BigDecimal surcharge) {
        Unit u = new Unit();
        u.setUnitType("FLAT");
        u.setUnitNumber(number);
        u.setOccupancy(occupancy);
        u.setBaseMaintenance(base);
        u.setTenantSurcharge(surcharge);
        u.setActive(true);
        return u;
    }
}
