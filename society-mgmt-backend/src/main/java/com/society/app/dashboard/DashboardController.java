package com.society.app.dashboard;

import com.society.app.common.ApiResponse;
import com.society.app.dashboard.dto.FinancialSummaryDto;
import com.society.app.dashboard.dto.MaintenanceRowDto;
import com.society.app.dashboard.dto.OutstandingSummaryDto;
import com.society.app.dashboard.dto.UnitSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin dashboard endpoints (project.md § 5.9). All routes are ADMIN-only.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    /** {@code GET /dashboard/summary} — unit counts. */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<UnitSummaryDto>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getSummary()));
    }

    /**
     * {@code GET /dashboard/maintenance} — per-unit maintenance status.
     *
     * @param type   optional FLAT / SHOP / TERRACE
     * @param status optional DUE / PARTIAL / PAID / NO_CHARGES
     */
    @GetMapping("/maintenance")
    public ResponseEntity<ApiResponse<List<MaintenanceRowDto>>> maintenance(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(
                ApiResponse.ok(dashboardService.getMaintenanceStatus(type, status)));
    }

    /** {@code GET /dashboard/financials} — money totals + pending payments badge. */
    @GetMapping("/financials")
    public ResponseEntity<ApiResponse<FinancialSummaryDto>> financials() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getFinancials()));
    }

    /** {@code GET /dashboard/outstanding} — society-wide + per-unit outstanding. */
    @GetMapping("/outstanding")
    public ResponseEntity<ApiResponse<OutstandingSummaryDto>> outstanding() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getOutstanding()));
    }
}
