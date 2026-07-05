package com.society.app.unit;

import com.society.app.common.ApiResponse;
import com.society.app.member.Member;
import com.society.app.member.MemberRepository;
import com.society.app.unit.dto.CreateUnitRequest;
import com.society.app.unit.dto.LinkMemberRequest;
import com.society.app.unit.dto.UnitDto;
import com.society.app.unit.dto.UpdateUnitRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unit endpoints (project.md § 5.3). All write operations are ADMIN-only.
 * Read of a single unit is allowed to a MEMBER iff they own it or are linked.
 *
 * <p>{@code /units/{id}/charges} and {@code /units/{id}/outstanding} live in
 * {@link com.society.app.charge.ChargeController} as of Phase 4.</p>
 */
@RestController
@RequestMapping("/api/v1/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;
    private final UnitMemberRepository unitMemberRepository;
    private final MemberRepository memberRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UnitDto>>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String occupancy,
            Pageable pageable,
            Authentication authentication) {
        Page<UnitDto> data;
        if (isAdmin(authentication)) {
            data = unitService.listUnits(type, occupancy, pageable);
        } else {
            // Member-scoped: return only units the caller owns or is linked to.
            Member caller = memberRepository.findByPhone(authentication.getName())
                    .orElseThrow(() -> new AccessDeniedException("Unknown caller"));
            data = unitService.listUnitsForMember(caller.getId(), type, occupancy, pageable);
        }
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UnitDto>> create(@Valid @RequestBody CreateUnitRequest req) {
        UnitDto data = unitService.createUnit(req);
        return ResponseEntity.ok(ApiResponse.ok(data, "Unit created"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UnitDto>> get(@PathVariable Long id,
                                                    Authentication authentication) {
        // Admin reads everything; members read only their own units.
        if (!isAdmin(authentication)) {
            ensureMemberCanReadUnit(id, authentication.getName());
        }
        return ResponseEntity.ok(ApiResponse.ok(unitService.getUnit(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UnitDto>> update(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateUnitRequest req) {
        UnitDto data = unitService.updateUnit(id, req);
        return ResponseEntity.ok(ApiResponse.ok(data, "Unit updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        unitService.deleteUnit(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Unit deleted"));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UnitDto>> linkMember(
            @PathVariable Long id,
            @Valid @RequestBody LinkMemberRequest req) {
        UnitDto data = unitService.linkMember(id, req);
        return ResponseEntity.ok(ApiResponse.ok(data, "Member linked to unit"));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unlinkMember(
            @PathVariable Long id,
            @PathVariable Long memberId) {
        unitService.unlinkMember(id, memberId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Member unlinked from unit"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    /**
     * Member-scoped access check: the caller (identified by phone) must either own
     * the unit or appear in {@code unit_members}. Throws AccessDeniedException
     * otherwise so {@link com.society.app.common.GlobalExceptionHandler} maps to 403.
     */
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
