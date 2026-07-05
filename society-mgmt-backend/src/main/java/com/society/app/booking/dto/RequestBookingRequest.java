package com.society.app.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RequestBookingRequest(
        @NotBlank String eventTitle,
        @NotNull LocalDate eventDate,
        Long unitId
) {}
