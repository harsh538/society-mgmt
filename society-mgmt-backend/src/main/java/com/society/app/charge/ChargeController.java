package com.society.app.charge;

import com.society.app.charge.dto.GenerateChargesRequest;
import com.society.app.charge.dto.GenerateChargesResponse;
import com.society.app.charge.dto.MaintenanceChargeDto;
import com.society.app.charge.dto.VoidChargeRequest;
import com.society.app.common.ApiResponse;
import com.society.app.member.Member;
import com.society.app.member.MemberRepository;
import com.society.app.unit.Unit;
import com.society.app.unit.UnitMemberRepository;
import com.society.app.unit.UnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Maintenance-charge endpoints (project.md § 5.4 + § 5.3 unit-scoped sub-resources).
 *
 * <p>Owns the {@code /units/{id}/charges} and {@code /units/{id}/outstanding}
 * routes — replacing the Phase 3 stubs in {@link com.society.app.unit.UnitController}.</p>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChargeController {

    private final ChargeService chargeService;
    private final UnitService unitService;
    private final UnitMemberRepository unitMemberRepository;
    private final MemberRepository memberRepository;

    // -------------------------------------------------------------------------
    // Admin: charge generation + listings
    // -------------------------------------------------------------------------

    @PostMapping("/charges/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GenerateChargesResponse>> generate(
            @Valid @RequestBody GenerateChargesRequest req) {
        GenerateChargesResponse data = chargeService.generateCharges(req);
        String message = String.format(
                "Charges generated for %d-%02d: %d created, %d skipped (already existed)",
                req.getYear(), req.getMonth(), data.getCreated(), data.getSkipped());
        return ResponseEntity.ok(ApiResponse.ok(data, message));
    }

    @GetMapping("/charges")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<MaintenanceChargeDto>>> list(
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<MaintenanceChargeDto> data =
                chargeService.listCharges(unitId, year, month, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/charges/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MaintenanceChargeDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(chargeService.getCharge(id)));
    }

    @PostMapping("/charges/{id}/void")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MaintenanceChargeDto>> voidCharge(
            @PathVariable Long id,
            @Valid @RequestBody VoidChargeRequest req) {
        MaintenanceChargeDto data = chargeService.voidCharge(id, req);
        return ResponseEntity.ok(ApiResponse.ok(data, "Charge voided"));
    }

    // -------------------------------------------------------------------------
    // Unit-scoped reads — admin OR member who owns / is linked to the unit
    // -------------------------------------------------------------------------

    @GetMapping("/units/{id}/charges")
    public ResponseEntity<ApiResponse<List<MaintenanceChargeDto>>> unitCharges(
            @PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            ensureMemberCanReadUnit(id, authentication.getName());
        }
        return ResponseEntity.ok(ApiResponse.ok(chargeService.getUnitCharges(id)));
    }

    @GetMapping("/units/{id}/outstanding")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unitOutstanding(
            @PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            ensureMemberCanReadUnit(id, authentication.getName());
        }
        return ResponseEntity.ok(ApiResponse.ok(chargeService.getUnitOutstanding(id)));
    }

    // -------------------------------------------------------------------------
    // Helpers (mirrors UnitController access pattern, project.md § 8)
    // -------------------------------------------------------------------------

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private void ensureMemberCanReadUnit(Long unitId, String phone) {
        Unit u = unitService.loadUnitOrThrow(unitId);
        Member caller = memberRepository.findByPhone(phone)
                .orElseThrow(() -> new AccessDeniedException("Unknown caller"));

        boolean isOwner = u.getOwner() != null
                && caller.getId().equals(u.getOwner().getId());
        boolean isLinked = unitMemberRepository
                .findByUnitIdAndMemberId(unitId, caller.getId())
                .isPresent();

        if (!isOwner && !isLinked) {
            throw new AccessDeniedException(
                    "Member is not authorized to view this unit");
        }
    }
}
