package com.society.app.charge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateChargesRequest {

    @NotNull(message = "year is required")
    @Min(value = 2000, message = "year must be >= 2000")
    @Max(value = 2100, message = "year must be <= 2100")
    private Integer year;

    @NotNull(message = "month is required")
    @Min(value = 1, message = "month must be between 1 and 12")
    @Max(value = 12, message = "month must be between 1 and 12")
    private Integer month;
}
