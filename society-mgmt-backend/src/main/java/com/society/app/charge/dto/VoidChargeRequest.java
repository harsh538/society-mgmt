package com.society.app.charge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoidChargeRequest {

    @NotBlank(message = "reason is required when voiding a charge")
    @Size(max = 500, message = "reason must be <= 500 chars")
    private String reason;
}
