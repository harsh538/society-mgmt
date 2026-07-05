package com.society.app.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RejectPaymentRequest {

    @NotBlank
    @Size(max = 500)
    private String rejectionReason;
}
