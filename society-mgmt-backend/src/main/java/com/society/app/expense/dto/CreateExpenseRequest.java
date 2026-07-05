package com.society.app.expense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExpenseRequest(
        @NotBlank
        @Pattern(regexp = "GARBAGE|ELECTRICITY|LIFT|WATER|SECURITY|REPAIRS|ELECTRICAL_GOODS|OTHER",
                 message = "category must be one of GARBAGE, ELECTRICITY, LIFT, WATER, SECURITY, REPAIRS, ELECTRICAL_GOODS, OTHER")
        String category,

        @NotBlank String title,

        String vendorName,

        @NotNull @DecimalMin("0.01") BigDecimal amount,

        @NotNull LocalDate expenseDate,

        String paidFrom,

        String note
) {}
