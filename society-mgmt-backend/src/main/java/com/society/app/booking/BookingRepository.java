package com.society.app.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<TerraceBooking, Long> {

    @Query("""
            SELECT b FROM TerraceBooking b
            WHERE (:status IS NULL OR b.status = :status)
              AND (:from IS NULL OR b.eventDate >= :from)
              AND (:to IS NULL OR b.eventDate <= :to)
            """)
    Page<TerraceBooking> findFiltered(
            @Param("status") String status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    Page<TerraceBooking> findByBookedById(Long memberId, Pageable pageable);

    @Query("""
            SELECT b.eventDate FROM TerraceBooking b
            WHERE b.status = 'APPROVED'
              AND b.eventDate >= :startDate
              AND b.eventDate <= :endDate
            """)
    List<LocalDate> findApprovedDatesBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByEventDateAndStatus(LocalDate eventDate, String status);
}
