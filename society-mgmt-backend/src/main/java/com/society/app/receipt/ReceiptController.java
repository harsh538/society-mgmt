package com.society.app.receipt;

import com.society.app.common.ApiResponse;
import com.society.app.receipt.dto.ReceiptDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Receipt endpoints (project.md § 5.6).
 *
 * <p>Members see only receipts issued to themselves; admins see everything.
 * The PDF endpoint redirects to the R2 public URL — auth check happens before redirect.</p>
 */
@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReceiptDto>>> list(
            Authentication authentication, Pageable pageable) {
        boolean isAdmin = isAdmin(authentication);
        return ResponseEntity.ok(ApiResponse.ok(
                receiptService.listReceipts(authentication.getName(), isAdmin, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReceiptDto>> get(
            @PathVariable Long id, Authentication authentication) {
        boolean isAdmin = isAdmin(authentication);
        return ResponseEntity.ok(ApiResponse.ok(
                receiptService.getReceipt(id, authentication.getName(), isAdmin)));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> pdf(
            @PathVariable Long id, Authentication authentication) {
        boolean isAdmin = isAdmin(authentication);
        String url = receiptService.getReceiptPdf(id, authentication.getName(), isAdmin);
        return ResponseEntity.ok(ApiResponse.ok(java.util.Map.of("url", url)));
    }

    private static boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
