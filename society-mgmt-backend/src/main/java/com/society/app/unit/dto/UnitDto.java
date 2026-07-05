package com.society.app.unit.dto;

import com.society.app.member.Member;
import com.society.app.unit.Unit;
import com.society.app.unit.UnitMember;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnitDto {

    private Long id;
    private String unitType;
    private String unitNumber;
    private String floor;
    private Long ownerMemberId;
    private String ownerName;
    private String occupancy;
    private BigDecimal baseMaintenance;
    private BigDecimal tenantSurcharge;
    private boolean isActive;
    private List<UnitMemberDto> members;

    /**
     * Project a {@link Unit} and its linked {@link UnitMember} rows into the DTO.
     * Pass {@code List.of()} for members when the caller only needs a flat view.
     */
    public static UnitDto from(Unit u, List<UnitMember> members) {
        Member owner = u.getOwner();
        return new UnitDto(
                u.getId(),
                u.getUnitType(),
                u.getUnitNumber(),
                u.getFloor(),
                owner != null ? owner.getId() : null,
                owner != null ? owner.getFullName() : null,
                u.getOccupancy(),
                u.getBaseMaintenance(),
                u.getTenantSurcharge(),
                u.isActive(),
                members.stream().map(UnitMemberDto::from).toList()
        );
    }
}
