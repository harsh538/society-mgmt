package com.society.app.member;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link Member}. Phone is the primary login identifier.
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByPhone(String phone);

    Page<Member> findByIsActiveTrue(Pageable pageable);

    Page<Member> findByRoleAndIsActiveTrue(String role, Pageable pageable);

    Page<Member> findByFullNameContainingIgnoreCaseAndIsActiveTrue(
            String search, Pageable pageable);
}
