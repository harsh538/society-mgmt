package com.society.app.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Sent as the JSON part of the multipart payload at {@code POST /payments}
 * (project.md § 5.5).
 */
@Data
@NoArgsConstructor
public class SubmitPaymentRequest {

    @NotNull
    private Long unitId;

    /** Optional — non-null when paying against a specific maintenance charge. */
    private Long chargeId;

    @NotBlank
    @jakarta.validation.constraints.Pattern(
            regexp = "MAINTENANCE|TERRACE_BOOKING|OTHER",
            message = "paymentType must be MAINTENANCE, TERRACE_BOOKING, or OTHER")
    private String paymentType;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank
    @jakarta.validation.constraints.Pattern(
            regexp = "UPI|BANK_TRANSFER|CASH",
            message = "method must be UPI, BANK_TRANSFER, or CASH")
    private String method;

    private String utrReference;

    @NotNull
    private OffsetDateTime paidAt;
}
