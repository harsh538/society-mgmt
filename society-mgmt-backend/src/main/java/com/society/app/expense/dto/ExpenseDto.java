package com.society.app.expense.dto;

import com.society.app.expense.SocietyExpense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ExpenseDto(
        Long id,
        String category,
        String title,
        String vendorName,
        BigDecimal amount,
        LocalDate expenseDate,
        String paidFrom,
        boolean hasBill,
        String note,
        Long recordedById,
        String recordedByName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ExpenseDto from(SocietyExpense e) {
        return new ExpenseDto(
                e.getId(),
                e.getCategory(),
                e.getTitle(),
                e.getVendorName(),
                e.getAmount(),
                e.getExpenseDate(),
                e.getPaidFrom(),
                e.getBillFilePath() != null && !e.getBillFilePath().isBlank(),
                e.getNote(),
                e.getRecordedBy() != null ? e.getRecordedBy().getId() : null,
                e.getRecordedBy() != null ? e.getRecordedBy().getFullName() : null,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
