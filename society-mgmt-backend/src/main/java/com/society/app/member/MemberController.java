package com.society.app.member;

import com.society.app.common.ApiResponse;
import com.society.app.member.dto.ActivateRequest;
import com.society.app.member.dto.CreateMemberRequest;
import com.society.app.member.dto.MemberDto;
import com.society.app.member.dto.UpdateMemberRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Admin-only member CRUD endpoints (project.md § 5.2).
 * All operations require {@code ROLE_ADMIN}.
 */
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<MemberDto>>> list(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<MemberDto> data = memberService.listMembers(role, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MemberDto>> create(
            @Valid @RequestBody CreateMemberRequest req) {
        MemberDto data = memberService.createMember(req);
        return ResponseEntity.ok(ApiResponse.ok(data, "Member created"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(memberService.getMember(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemberRequest req) {
        MemberDto data = memberService.updateMember(id, req);
        return ResponseEntity.ok(ApiResponse.ok(data, "Member updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Member deleted"));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<MemberDto>> activate(
            @PathVariable Long id,
            @Valid @RequestBody ActivateRequest req) {
        MemberDto data = memberService.activateMember(id, req);
        return ResponseEntity.ok(ApiResponse.ok(data, "Member activated"));
    }
}
