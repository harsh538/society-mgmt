package com.society.app.unit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitMemberRepository extends JpaRepository<UnitMember, Long> {

    List<UnitMember> findByUnitId(Long unitId);

    List<UnitMember> findByMemberId(Long memberId);

    Optional<UnitMember> findByUnitIdAndMemberId(Long unitId, Long memberId);

    void deleteByUnitIdAndMemberId(Long unitId, Long memberId);
}
