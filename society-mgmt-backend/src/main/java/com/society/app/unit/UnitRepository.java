package com.society.app.unit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {

    Page<Unit> findByIsActiveTrue(Pageable pageable);

    Page<Unit> findByUnitTypeAndIsActiveTrue(String unitType, Pageable pageable);

    Page<Unit> findByOccupancyAndIsActiveTrue(String occupancy, Pageable pageable);

    Page<Unit> findByUnitTypeAndOccupancyAndIsActiveTrue(
            String unitType, String occupancy, Pageable pageable);

    boolean existsByUnitNumberAndIsActiveTrue(String unitNumber);

    /**
     * Used by charge generation to fetch all active FLAT + SHOP units
     * (TERRACE is excluded from monthly maintenance per project.md § 7.2).
     */
    List<Unit> findByUnitTypeInAndIsActiveTrue(List<String> unitTypes);

    // -------------------------------------------------------------------------
    // Dashboard aggregations (Phase 6) — project.md § 5.9
    // -------------------------------------------------------------------------

    /** All active units (no pagination) — drives the dashboard maintenance-status table. */
    List<Unit> findByIsActiveTrue();

    /** Active-unit count grouped by unit_type (FLAT / SHOP / TERRACE). */
    long countByUnitTypeAndIsActiveTrue(String unitType);

    /** Active-unit count grouped by occupancy (OWNER / TENANT / VACANT). */
    long countByOccupancyAndIsActiveTrue(String occupancy);

    /** Total active-unit count (denominator on the summary tile). */
    long countByIsActiveTrue();
}
