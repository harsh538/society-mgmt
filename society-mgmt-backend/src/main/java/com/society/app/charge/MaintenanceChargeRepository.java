package com.society.app.charge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceChargeRepository extends JpaRepository<MaintenanceCharge, Long> {

    Optional<MaintenanceCharge> findByUnitIdAndPeriodYearAndPeriodMonth(
            Long unitId, int periodYear, int periodMonth);

    Page<MaintenanceCharge> findByStatusNot(String status, Pageable pageable);

    Page<MaintenanceCharge> findByUnitId(Long unitId, Pageable pageable);

    Page<MaintenanceCharge> findByPeriodYearAndPeriodMonth(
            int periodYear, int periodMonth, Pageable pageable);

    Page<MaintenanceCharge> findByStatus(String status, Pageable pageable);

    Page<MaintenanceCharge> findByUnitIdAndStatus(Long unitId, String status, Pageable pageable);

    Page<MaintenanceCharge> findByPeriodYearAndPeriodMonthAndStatus(
            int periodYear, int periodMonth, String status, Pageable pageable);

    Page<MaintenanceCharge> findByUnitIdAndPeriodYearAndPeriodMonthAndStatus(
            Long unitId, int periodYear, int periodMonth, String status, Pageable pageable);

    List<MaintenanceCharge> findByUnitIdAndStatusNot(Long unitId, String status);

    List<MaintenanceCharge> findByUnitIdAndStatusNotOrderByPeriodYearDescPeriodMonthDesc(
            Long unitId, String status);

    // -------------------------------------------------------------------------
    // Dashboard aggregations (Phase 6) — project.md § 5.9 / § 3.10
    // COALESCE so an empty ledger returns 0 instead of null.
    // -------------------------------------------------------------------------

    @Query("SELECT COALESCE(SUM(c.amountDue), 0) FROM MaintenanceCharge c WHERE c.status <> 'VOID'")
    BigDecimal sumAmountDue();

    @Query("SELECT COALESCE(SUM(c.amountPaid), 0) FROM MaintenanceCharge c WHERE c.status <> 'VOID'")
    BigDecimal sumAmountPaid();
}
