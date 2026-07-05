package com.society.app.booking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ApproveBookingRequest(
        @NotNull @DecimalMin("0") BigDecimal nominalFee
) {}
