package com.society.app.booking.dto;

import com.society.app.booking.TerraceBooking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record BookingDto(
        Long id,
        Long unitId,
        String unitNumber,
        Long bookedById,
        String bookedByName,
        String eventTitle,
        LocalDate eventDate,
        BigDecimal nominalFee,
        String status,
        Long approvedById,
        String approvedByName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static BookingDto from(TerraceBooking b) {
        return new BookingDto(
                b.getId(),
                b.getUnit() != null ? b.getUnit().getId() : null,
                b.getUnit() != null ? b.getUnit().getUnitNumber() : null,
                b.getBookedBy() != null ? b.getBookedBy().getId() : null,
                b.getBookedBy() != null ? b.getBookedBy().getFullName() : null,
                b.getEventTitle(),
                b.getEventDate(),
                b.getNominalFee(),
                b.getStatus(),
                b.getApprovedBy() != null ? b.getApprovedBy().getId() : null,
                b.getApprovedBy() != null ? b.getApprovedBy().getFullName() : null,
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}
