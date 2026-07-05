package com.society.app.payment;

import com.society.app.common.ApiResponse;
import com.society.app.payment.dto.PaymentDto;
import com.society.app.payment.dto.RejectPaymentRequest;
import com.society.app.payment.dto.SubmitPaymentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


/**
 * Payment lifecycle endpoints (project.md § 5.5).
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // -------------------------------------------------------------------------
    // Admin: listings + verification queue
    // -------------------------------------------------------------------------

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<PaymentDto>>> list(
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.listPayments(unitId, status, pageable)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<PaymentDto>>> pending(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getPendingQueue(pageable)));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<ApiResponse<Page<PaymentDto>>> mine(
            Authentication authentication, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.getMyPayments(authentication.getName(), pageable)));
    }

    // -------------------------------------------------------------------------
    // Member: submit
    // -------------------------------------------------------------------------

    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<ApiResponse<PaymentDto>> submit(
            @RequestPart("data") @Valid SubmitPaymentRequest data,
            @RequestPart(value = "proof", required = false) MultipartFile proof,
            Authentication authentication) {
        PaymentDto created = paymentService.submitPayment(
                authentication.getName(), data, proof);
        return ResponseEntity.ok(ApiResponse.ok(created,
                "Payment submitted for verification"));
    }

    // -------------------------------------------------------------------------
    // Detail
    // -------------------------------------------------------------------------

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentDto>> get(
            @PathVariable Long id, Authentication authentication) {
        boolean isAdmin = isAdmin(authentication);
        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.getPayment(id, authentication.getName(), isAdmin)));
    }

    // -------------------------------------------------------------------------
    // Admin: verify / reject
    // -------------------------------------------------------------------------

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentDto>> verify(
            @PathVariable Long id, Authentication authentication) {
        PaymentDto out = paymentService.verifyPayment(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(out, "Payment verified. Receipt generated."));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentDto>> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectPaymentRequest req,
            Authentication authentication) {
        PaymentDto out = paymentService.rejectPayment(id, authentication.getName(), req);
        return ResponseEntity.ok(ApiResponse.ok(out, "Payment rejected"));
    }

    // -------------------------------------------------------------------------
    // Proof file — return R2 public URL as JSON (frontend opens in new tab)
    // -------------------------------------------------------------------------

    @GetMapping("/{id}/proof")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> proof(
            @PathVariable Long id, Authentication authentication) {
        boolean isAdmin = isAdmin(authentication);
        String url = paymentService.getProof(id, authentication.getName(), isAdmin);
        return ResponseEntity.ok(ApiResponse.ok(java.util.Map.of("url", url)));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
