package com.society.app.unit;

import com.society.app.member.Member;
import com.society.app.member.MemberRepository;
import com.society.app.unit.dto.CreateUnitRequest;
import com.society.app.unit.dto.LinkMemberRequest;
import com.society.app.unit.dto.UnitDto;
import com.society.app.unit.dto.UpdateUnitRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service layer for unit lifecycle + member linking (project.md § 5.3).
 *
 * <p>Soft delete only. Member-linking enforces the (unit_id, member_id) DB unique
 * constraint at the service layer with a friendly error before relying on the
 * database to reject it.</p>
 */
@Service
@RequiredArgsConstructor
public class UnitService {

    private final UnitRepository unitRepository;
    private final UnitMemberRepository unitMemberRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Page<UnitDto> listUnits(String type, String occupancy, Pageable pageable) {
        boolean hasType = type != null && !type.isBlank();
        boolean hasOcc = occupancy != null && !occupancy.isBlank();

        Page<Unit> page;
        if (hasType && hasOcc) {
            page = unitRepository.findByUnitTypeAndOccupancyAndIsActiveTrue(
                    type, occupancy, pageable);
        } else if (hasType) {
            page = unitRepository.findByUnitTypeAndIsActiveTrue(type, pageable);
        } else if (hasOcc) {
            page = unitRepository.findByOccupancyAndIsActiveTrue(occupancy, pageable);
        } else {
            page = unitRepository.findByIsActiveTrue(pageable);
        }
        // For list views we elide member rows to keep payload small.
        return page.map(u -> UnitDto.from(u, List.of()));
    }

    /**
     * Member-scoped list: returns units the member owns OR is linked to via
     * {@code unit_members}. Type / occupancy filters applied in-memory after
     * the join, then paginated client-side. Volume is small (a member has at
     * most a handful of units), so naive filtering is fine.
     */
    @Transactional(readOnly = true)
    public Page<UnitDto> listUnitsForMember(
            Long memberId, String type, String occupancy, Pageable pageable) {
        boolean hasType = type != null && !type.isBlank();
        boolean hasOcc = occupancy != null && !occupancy.isBlank();

        // Collect candidate unit ids: ownership + linked rows.
        List<Long> ownedIds = unitRepository.findByIsActiveTrue(
                org.springframework.data.domain.Pageable.unpaged()).stream()
                .filter(u -> u.getOwner() != null && memberId.equals(u.getOwner().getId()))
                .map(Unit::getId)
                .toList();
        List<Long> linkedIds = unitMemberRepository.findByMemberId(memberId).stream()
                .map(um -> um.getUnit().getId())
                .toList();

        java.util.Set<Long> all = new java.util.LinkedHashSet<>();
        all.addAll(ownedIds);
        all.addAll(linkedIds);

        List<Unit> units = all.stream()
                .map(unitRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(Unit::isActive)
                .filter(u -> !hasType || type.equals(u.getUnitType()))
                .filter(u -> !hasOcc || occupancy.equals(u.getOccupancy()))
                .toList();

        int from = (int) Math.min(pageable.getOffset(), units.size());
        int to = Math.min(from + pageable.getPageSize(), units.size());
        List<UnitDto> content = units.subList(from, to).stream()
                .map(u -> UnitDto.from(u, List.of()))
                .toList();
        return new org.springframework.data.domain.PageImpl<>(content, pageable, units.size());
    }

    @Transactional(readOnly = true)
    public UnitDto getUnit(Long id) {
        Unit u = unitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + id));
        if (!u.isActive()) {
            throw new EntityNotFoundException("Unit not found: " + id);
        }
        List<UnitMember> members = unitMemberRepository.findByUnitId(id);
        return UnitDto.from(u, members);
    }

    /**
     * Plain entity loader (no DTO mapping) — used by the controller for ownership
     * checks on member-scoped reads.
     */
    @Transactional(readOnly = true)
    public Unit loadUnitOrThrow(Long id) {
        Unit u = unitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + id));
        if (!u.isActive()) {
            throw new EntityNotFoundException("Unit not found: " + id);
        }
        return u;
    }

    @Transactional
    public UnitDto createUnit(CreateUnitRequest req) {
        Unit u = new Unit();
        u.setUnitType(req.getUnitType());
        u.setUnitNumber(req.getUnitNumber());
        u.setFloor(req.getFloor());
        u.setOccupancy(req.getOccupancy());
        u.setBaseMaintenance(req.getBaseMaintenance() != null
                ? req.getBaseMaintenance() : BigDecimal.ZERO);
        u.setTenantSurcharge(req.getTenantSurcharge() != null
                ? req.getTenantSurcharge() : BigDecimal.ZERO);
        u.setActive(true);

        if (req.getOwnerMemberId() != null) {
            Member owner = memberRepository.findById(req.getOwnerMemberId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Owner member not found: " + req.getOwnerMemberId()));
            u.setOwner(owner);
        }

        try {
            Unit saved = unitRepository.save(u);
            return UnitDto.from(saved, List.of());
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(
                    "Duplicate unit number — unit already exists", ex);
        }
    }

    @Transactional
    public UnitDto updateUnit(Long id, UpdateUnitRequest req) {
        Unit u = unitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + id));

        if (req.getUnitType() != null) u.setUnitType(req.getUnitType());
        if (req.getUnitNumber() != null) u.setUnitNumber(req.getUnitNumber());
        if (req.getFloor() != null) u.setFloor(req.getFloor());
        if (req.getOccupancy() != null) u.setOccupancy(req.getOccupancy());
        if (req.getBaseMaintenance() != null) u.setBaseMaintenance(req.getBaseMaintenance());
        if (req.getTenantSurcharge() != null) u.setTenantSurcharge(req.getTenantSurcharge());

        if (req.getOwnerMemberId() != null) {
            Member owner = memberRepository.findById(req.getOwnerMemberId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Owner member not found: " + req.getOwnerMemberId()));
            u.setOwner(owner);
        }

        try {
            unitRepository.save(u);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(
                    "Update violates a uniqueness constraint", ex);
        }
        List<UnitMember> members = unitMemberRepository.findByUnitId(id);
        return UnitDto.from(u, members);
    }

    @Transactional
    public void deleteUnit(Long id) {
        Unit u = unitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + id));
        u.setActive(false);
        unitRepository.save(u);
    }

    @Transactional
    public UnitDto linkMember(Long unitId, LinkMemberRequest req) {
        Unit u = unitRepository.findById(unitId)
                .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + unitId));
        Member m = memberRepository.findById(req.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Member not found: " + req.getMemberId()));

        unitMemberRepository.findByUnitIdAndMemberId(unitId, req.getMemberId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Member already linked to this unit");
                });

        UnitMember um = new UnitMember();
        um.setUnit(u);
        um.setMember(m);
        um.setRelationship(req.getRelationship());
        unitMemberRepository.save(um);

        List<UnitMember> members = unitMemberRepository.findByUnitId(unitId);
        return UnitDto.from(u, members);
    }

    @Transactional
    public void unlinkMember(Long unitId, Long memberId) {
        UnitMember um = unitMemberRepository.findByUnitIdAndMemberId(unitId, memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Member " + memberId + " is not linked to unit " + unitId));
        unitMemberRepository.delete(um);
    }
}
