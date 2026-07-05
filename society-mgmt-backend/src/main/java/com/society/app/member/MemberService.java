package com.society.app.member;

import com.society.app.member.dto.ActivateRequest;
import com.society.app.member.dto.CreateMemberRequest;
import com.society.app.member.dto.MemberDto;
import com.society.app.member.dto.UpdateMemberRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-facing CRUD for {@link Member}. Distinct from {@link AuthService}, which owns
 * login / refresh / password rotation for the currently authenticated user.
 *
 * <p>Soft delete only — {@code is_active=false}. Members are never hard-deleted because
 * they may be referenced by historical financial rows (payments, receipts, expenses).</p>
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private static final String DEFAULT_ROLE = "MEMBER";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<MemberDto> listMembers(String role, String search, Pageable pageable) {
        Page<Member> page;
        boolean hasRole = role != null && !role.isBlank();
        boolean hasSearch = search != null && !search.isBlank();

        if (hasSearch) {
            // Search by name takes precedence; role filter is applied client-side if needed.
            page = memberRepository
                    .findByFullNameContainingIgnoreCaseAndIsActiveTrue(search, pageable);
        } else if (hasRole) {
            page = memberRepository.findByRoleAndIsActiveTrue(role, pageable);
        } else {
            page = memberRepository.findByIsActiveTrue(pageable);
        }
        return page.map(MemberDto::from);
    }

    @Transactional(readOnly = true)
    public MemberDto getMember(Long id) {
        return memberRepository.findById(id)
                .map(MemberDto::from)
                .orElseThrow(() -> new EntityNotFoundException("Member not found: " + id));
    }

    @Transactional
    public MemberDto createMember(CreateMemberRequest req) {
        Member m = new Member();
        m.setFullName(req.getFullName());
        m.setPhone(req.getPhone());
        m.setEmail(req.getEmail());
        m.setRole((req.getRole() == null || req.getRole().isBlank())
                ? DEFAULT_ROLE : req.getRole());
        m.setActive(true);
        // password_hash deliberately null — admin sets it later via /activate.
        try {
            return MemberDto.from(memberRepository.save(m));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(
                    "Duplicate phone or email — member already exists", ex);
        }
    }

    @Transactional
    public MemberDto updateMember(Long id, UpdateMemberRequest req) {
        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Member not found: " + id));
        if (req.getFullName() != null) m.setFullName(req.getFullName());
        if (req.getPhone() != null) m.setPhone(req.getPhone());
        if (req.getEmail() != null) m.setEmail(req.getEmail());
        if (req.getRole() != null) m.setRole(req.getRole());
        try {
            return MemberDto.from(memberRepository.save(m));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(
                    "Duplicate phone or email — update would violate uniqueness", ex);
        }
    }

    /** Soft delete: history-preserving. */
    @Transactional
    public void deleteMember(Long id) {
        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Member not found: " + id));
        m.setActive(false);
        memberRepository.save(m);
    }

    /**
     * Set the password (initial activation) and ensure the account is active.
     * Used by admin to provision a member's first password.
     */
    @Transactional
    public MemberDto activateMember(Long id, ActivateRequest req) {
        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Member not found: " + id));
        m.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        m.setActive(true);
        return MemberDto.from(memberRepository.save(m));
    }
}
