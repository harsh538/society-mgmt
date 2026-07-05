package com.society.app.charge;

import com.society.app.charge.dto.ChargeConfigDto;
import com.society.app.charge.dto.UpdateChargeConfigRequest;
import com.society.app.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Society-wide {@link ChargeConfig} endpoints (project.md § 5.4). ADMIN-only.
 */
@RestController
@RequestMapping("/api/v1/charge-config")
@RequiredArgsConstructor
public class ChargeConfigController {

    private final ChargeService chargeService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ChargeConfigDto>> get() {
        return ResponseEntity.ok(ApiResponse.ok(chargeService.getConfig()));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ChargeConfigDto>> update(
            @Valid @RequestBody UpdateChargeConfigRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                chargeService.updateConfig(req), "Charge config updated"));
    }
}
