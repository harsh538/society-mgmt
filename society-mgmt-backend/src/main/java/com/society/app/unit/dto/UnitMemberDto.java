package com.society.app.unit.dto;

import com.society.app.member.Member;
import com.society.app.unit.UnitMember;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnitMemberDto {

    private Long memberId;
    private String memberName;
    private String memberPhone;
    private String relationship;

    public static UnitMemberDto from(UnitMember um) {
        Member m = um.getMember();
        return new UnitMemberDto(
                m.getId(),
                m.getFullName(),
                m.getPhone(),
                um.getRelationship()
        );
    }
}
